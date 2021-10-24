/*
 * Copyright 2021 Alemiz
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package alemiz.stargate.vortex.common.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.pipeline.UnhandledPacketConsumer;
import alemiz.stargate.utils.StarGateLogger;
import alemiz.stargate.vortex.common.data.ResponseHandle;
import alemiz.stargate.vortex.common.data.VortexSettings;
import alemiz.stargate.vortex.common.pipeline.VortexPacketDecoder;
import alemiz.stargate.vortex.common.pipeline.VortexPacketEncoder;
import alemiz.stargate.vortex.common.pipeline.VortexPipelineTail;
import alemiz.stargate.vortex.common.protocol.packet.VortexLatencyPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.VortexPacketListener;
import alemiz.stargate.vortex.common.protocol.packet.VortexResponse;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public abstract class VortexNode extends SimpleChannelInboundHandler<VortexPacket> {
    public static final String NAME = "vortex-node";

    public static final int PING_INTERVAL_MILLIS = 50;
    public static final int RESPONSE_TIMEOUT_INTERVAL_SECONDS = 30;

    protected final VortexNodeOwner vortexParent;
    protected final StarGateSession session;

    protected VortexPacketListener vortexPacketListener;

    private ScheduledFuture<?> pingFuture;
    private ScheduledFuture<?> responsesFuture;

    private long landPingTime;
    private long latency;

    private final AtomicInteger responseIdAllocator = new AtomicInteger(0);
    private final Long2ObjectMap<ResponseHandle> pendingResponses = new Long2ObjectOpenHashMap<>();

    private volatile boolean closed = false;

    public VortexNode(StarGateSession session, VortexNodeOwner vortexParent) {
        this.session = session;
        this.vortexParent = vortexParent;
    }

    public final void initialize(Channel channel) {
        if (log.isTraceEnabled()) {
            this.session.setLogInputLevel(StarGateLogger.LEVEL_FILTERED);
            this.session.setLogOutputLevel(StarGateLogger.LEVEL_FILTERED);
        }

        VortexSettings settings = this.vortexParent.getSettings();
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addBefore(UnhandledPacketConsumer.NAME, VortexPacketDecoder.NAME, new VortexPacketDecoder(this.vortexParent.getPacketPool()));
        pipeline.addBefore(UnhandledPacketConsumer.NAME, VortexPacketEncoder.NAME,
                new VortexPacketEncoder(this.session, settings.getCompression(), settings.getCompressionLevel()));
        pipeline.addAfter(VortexPacketDecoder.NAME, VortexNode.NAME, this);
        pipeline.addLast(VortexPipelineTail.NAME, new VortexPipelineTail(this));

        this.pingFuture = channel.eventLoop().scheduleAtFixedRate(this::sendPing, 200, PING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        this.responsesFuture = channel.eventLoop().scheduleAtFixedRate(this::collectResponses, RESPONSE_TIMEOUT_INTERVAL_SECONDS, RESPONSE_TIMEOUT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        this.initialize0(channel);
    }

    protected void initialize0(Channel channel) {
        // Allow custom initialize implementations
    }

    private void deinitialize() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.pingFuture.cancel(false);
        this.responsesFuture.cancel(false);
        this.deinitialize0();
    }

    protected void deinitialize0() {
        // Allow custom deinitialize implementations
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VortexPacket packet) throws Exception {
        if (this.closed) {
            return;
        }

        // Ensure we dont slowly cause memory leak
        ResponseHandle responseHandle = null;
        if (packet instanceof VortexResponse) {
            responseHandle = this.pendingResponses.remove(((VortexResponse) packet).getResponseId());
        }

        if (this.vortexPacketListener != null && packet.handle(this.vortexPacketListener)) {
            return;
        }

        if (this.handleInternal(packet)) {
            return;
        }

        if (responseHandle == null) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(packet));
        } else {
            responseHandle.getPromise().setSuccess((VortexResponse) packet);
        }
    }

    protected boolean handleInternal(VortexPacket packet) {
        if (packet instanceof VortexMessagePacket && this.onMessagePacket((VortexMessagePacket) packet)) {
            return true;
        }

        if (packet instanceof VortexLatencyPacket) {
            this.onPing((VortexLatencyPacket) packet);
            return true;
        }
        return false;
    }

    protected boolean onMessagePacket(VortexMessagePacket packet) {
        // Implement if required
        return false;
    }

    public void onDisconnected() {
        if (this.isClosed()) {
            return;
        }
        log.info("Vortex node " + this.getNodeName() + " has disconnected!");
        this.deinitialize();
    }

    private void onPing(VortexLatencyPacket packet) {
        if (packet.isPong()) {
            this.latency = System.currentTimeMillis() - this.landPingTime;
        } else {
            packet.setPong(true);
            this.sendPacket(packet);
        }
    }

    private void sendPing() {
        long currTime = System.currentTimeMillis();
        if ((this.landPingTime + PING_INTERVAL_MILLIS) >= currTime) {
            return;
        }
        this.landPingTime = currTime;

        VortexLatencyPacket packet = new VortexLatencyPacket();
        packet.setSendTime(currTime);
        packet.setPong(false);
        this.sendPacket(packet);
    }

    private void collectResponses() {
        long currTime = System.currentTimeMillis();
        Iterator<ResponseHandle> iterator = this.pendingResponses.values().iterator();
        while (iterator.hasNext()) {
            ResponseHandle handle = iterator.next();
            if (currTime > (handle.getSendTime() + RESPONSE_TIMEOUT_INTERVAL_SECONDS)) {
                handle.getPromise().setFailure(new TimeoutException("No response received"));
                iterator.remove();
            }
        }
    }

    public Promise<VortexResponse> sendResponsePacket(VortexResponse packet) {
        if (this.closed || !this.session.getChannel().isActive()) {
            return null;
        }

        int id = this.responseIdAllocator.getAndIncrement();
        packet.setResponseId(id);

        Promise<VortexResponse> promise = new DefaultPromise<>(this.session.getChannel().eventLoop());
        long currTime = System.currentTimeMillis();
        this.pendingResponses.put(id, new ResponseHandle(currTime, promise));

        this.sendPacket(packet);
        return promise;
    }

    public void sendPacket(VortexPacket packet) {
        if (!this.closed && this.session.getChannel().isActive()) {
            this.session.getChannel().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    public void disconnect(String reason) {
        if (!this.isClosed()) {
            log.info("Disconnecting Vortex node " + this.getNodeName() + " due to: " + reason);
        }
        this.session.disconnect(reason);
        this.deinitialize();
    }

    public void close() {
        if (!this.session.isClosed()) {
            this.session.close();
        }
        this.deinitialize();
    }

    public Promise<VortexResponse> getResponsePromise(int responseId) {
        ResponseHandle responseHandle = this.pendingResponses.get(responseId);
        if (responseHandle != null) {
            return responseHandle.getPromise();
        }
        return null;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public String getNodeName() {
        return this.session.getSessionName();
    }

    public long getLatency() {
        return this.latency;
    }

    public VortexPacketListener getVortexPacketListener() {
        return this.vortexPacketListener;
    }

    public void setVortexPacketListener(VortexPacketListener vortexPacketListener) {
        this.vortexPacketListener = vortexPacketListener;
    }

    public InetSocketAddress getAddress() {
        return this.session.getAddress();
    }

    public abstract VortexNodeType getVortexType();

    public VortexNodeOwner getVortexParent() {
        return this.vortexParent;
    }
}
