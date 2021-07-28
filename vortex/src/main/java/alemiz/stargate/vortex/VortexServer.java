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

package alemiz.stargate.vortex;

import alemiz.stargate.codec.ProtocolCodec;
import alemiz.stargate.server.ServerSession;
import alemiz.stargate.server.StarGateServer;
import alemiz.stargate.utils.ServerLoader;
import alemiz.stargate.utils.StarGateLogger;
import alemiz.stargate.vortex.common.data.VortexSettings;
import alemiz.stargate.vortex.common.node.VortexServerNodeOwner;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;
import alemiz.stargate.vortex.common.protocol.stargate.VortexGatePacket;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeType;
import alemiz.stargate.vortex.common.VortexLogger;
import alemiz.stargate.vortex.node.VortexDefaultMasterNode;
import alemiz.stargate.vortex.node.VortexDefaultNode;
import alemiz.stargate.vortex.stargate.StarGateListener;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class VortexServer implements ServerLoader, VortexServerNodeOwner {

    // Force init default node types
    public static final VortexNodeType DEFAULT_MASTER_NODE = VortexNodeType.from("vortex-master", VortexDefaultMasterNode::new);
    public static final VortexNodeType DEFAULT_CHILD_NODE = VortexNodeType.from("vortex-node", VortexDefaultNode::new);

    private final StarGateLogger logger = new VortexLogger();
    private final VortexSettings settings;
    private final StarGateServer server;

    private final Map<InetSocketAddress, VortexNode> vortexNodes = new ConcurrentHashMap<>();
    private VortexPacketPool packetPool = new VortexPacketPool();
    private VortexListener listener;

    public VortexServer(VortexSettings settings) {
        this.settings = settings;

        InetSocketAddress address = new InetSocketAddress("0.0.0.0", settings.getPort());
        this.server = new StarGateServer(address, settings.getPassword(), this);
        this.server.setServerListener(new StarGateListener(this, this.server));
        this.registerStarGateCodec(this.server.getProtocolCodec());
    }

    public void start() {
        this.server.start();
    }

    public void shutdown() {
        if (this.server.isShutdown()) {
            throw new IllegalStateException("Server is already shutdown");
        }
        this.server.shutdown();
    }

    private void registerStarGateCodec(ProtocolCodec codec) {
        codec.registerPacket(VortexPacketPool.VORTEX_GATE_PACKET, VortexGatePacket.class);
        codec.registerPacket(VortexPacketPool.VORTEX_CLIENT_HANDSHAKE_PACKET, VortexClientHandshakePacket.class);
    }

    public VortexNode createNewNode(String typeName, ServerSession session) {
        VortexNodeType vortexType = VortexNodeType.fromString(typeName);
        VortexNode node = vortexType.getFactory().newInstance(session, this);
        node.initialize(session.getChannel());

        VortexNode oldNode = this.vortexNodes.remove(session.getAddress());
        if (oldNode != null) {
            oldNode.disconnect("Connected from another location!");
        }

        log.info("New Vortex node successfully created " + session.getAddress());
        this.vortexNodes.put(session.getAddress(), node);
        if (this.listener != null) {
            this.listener.onNodeCreated(session.getAddress(), node);
        }
        return node;
    }

    public void onNodeClosed(ServerSession session) {
        VortexNode vortexNode = this.vortexNodes.remove(session.getAddress());
        if (vortexNode == null) {
            return;
        }

        vortexNode.onDisconnected();
        if (this.listener != null) {
            this.listener.onNodeDisconnected(vortexNode);
        }
    }

    @Override
    public VortexNode getVortexNode(InetSocketAddress address) {
        return this.vortexNodes.get(address);
    }

    @Override
    public VortexNode getVortexNode(String name) {
        for (VortexNode vortexNode : this.vortexNodes.values()) {
            if (vortexNode.getNodeName().equals(name)) {
                return vortexNode;
            }
        }
        return null;
    }

    public VortexListener getListener() {
        return listener;
    }

    public void setListener(VortexListener listener) {
        this.listener = listener;
    }

    @Override
    public VortexPacketPool getPacketPool() {
        return this.packetPool;
    }

    public void setPacketPool(VortexPacketPool packetPool) {
        this.packetPool = packetPool;
    }

    @Override
    public VortexSettings getSettings() {
        return this.settings;
    }

    @Override
    public StarGateLogger getStarGateLogger() {
        return this.logger;
    }

    public boolean isClosed() {
        return this.server.isShutdown();
    }
}
