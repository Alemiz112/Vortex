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
import alemiz.stargate.vortex.common.data.VortexSettings;
import alemiz.stargate.vortex.common.pipeline.VortexPacketDecoder;
import alemiz.stargate.vortex.common.pipeline.VortexPacketEncoder;
import alemiz.stargate.vortex.common.protocol.packet.VortexLatencyPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.VortexPacketListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public abstract class VortexNode extends SimpleChannelInboundHandler<VortexPacket> {
    public static final String NAME = "vortex-node";

    public static final int PING_INTERVAL = 50;

    protected final VortexNodeParent vortexParent;
    protected final StarGateSession session;

    protected VortexPacketListener vortexPacketListener;

    private ScheduledFuture<?> pingFuture;
    private long landPingTime;
    private long latency;

    private volatile boolean closed = false;

    public VortexNode(StarGateSession session, VortexNodeParent vortexParent) {
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

        this.pingFuture = channel.eventLoop().scheduleAtFixedRate(this::sendPing, 200, PING_INTERVAL, TimeUnit.MILLISECONDS);
        this.initialize0(channel);
    }

    protected void initialize0(Channel channel) {
        // Allow custom initialize implementations
    }

    private void deinitialize() {
        if (this.closed) {
            return;
        }
        this.closed = closed;
        this.pingFuture.cancel(false);
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

        if (this.vortexPacketListener != null && packet.handle(this.vortexPacketListener)) {
            return;
        }

        if (!this.handleInternal(packet)) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(packet));
        }
    }

    protected boolean handleInternal(VortexPacket packet) {
        if (packet instanceof VortexLatencyPacket) {
            this.onPing((VortexLatencyPacket) packet);
            return true;
        }

        return false;
    }

    public void onDisconnected() {
        if (this.isClosed()) {
            return;
        }
        log.info("Vortex node " + this.getName() + " has disconnected!");
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
        if ((this.landPingTime + PING_INTERVAL) >= currTime) {
            return;
        }
        this.landPingTime = currTime;

        VortexLatencyPacket packet = new VortexLatencyPacket();
        packet.setSendTime(currTime);
        packet.setPong(false);
        this.sendPacket(packet);
    }

    public void sendPacket(VortexPacket packet) {
        if (!this.closed && this.session.getChannel().isActive()) {
            this.session.getChannel().writeAndFlush(packet);
        }
    }

    public void disconnect(String reason) {
        if (!this.isClosed()) {
            log.info("Disconnecting Vortex node " + this.getName() + " due to: " + reason);
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

    public boolean isClosed() {
        return this.closed;
    }

    public abstract String getName();

    public long getLatency() {
        return this.latency;
    }

    public InetSocketAddress getAddress() {
        return this.session.getAddress();
    }

    public abstract VortexNodeType getVortexType();

    public VortexNodeParent getVortexParent() {
        return this.vortexParent;
    }
}
