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

package alemiz.stargate.vortex.client;

import alemiz.stargate.client.ClientSession;
import alemiz.stargate.client.StarGateClient;
import alemiz.stargate.codec.ProtocolCodec;
import alemiz.stargate.protocol.types.HandshakeData;
import alemiz.stargate.utils.ServerLoader;
import alemiz.stargate.utils.StarGateLogger;
import alemiz.stargate.vortex.client.data.VortexClientSettings;
import alemiz.stargate.vortex.client.node.VortexClientMasterNode;
import alemiz.stargate.vortex.client.node.VortexClientNode;
import alemiz.stargate.vortex.client.stargate.StarGateListener;
import alemiz.stargate.vortex.common.VortexLogger;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeOwner;
import alemiz.stargate.vortex.common.node.VortexNodeType;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;
import alemiz.stargate.vortex.common.protocol.stargate.VortexGatePacket;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VortexClient implements ServerLoader, VortexNodeOwner {

    // Force init default node types
    public static VortexNodeType DEFAULT_MASTER_NODE = VortexNodeType.from("vortex-master", VortexClientMasterNode::new);
    public static VortexNodeType DEFAULT_CHILD_NODE = VortexNodeType.from("vortex-node", VortexClientNode::new);

    private final StarGateLogger logger = new VortexLogger();
    private final VortexClientSettings settings;
    private final StarGateClient client;

    private VortexNode vortexNode;
    private VortexPacketPool packetPool = new VortexPacketPool();
    private VortexListener listener;

    public VortexClient(VortexClientSettings settings) {
        this.settings = settings;

        HandshakeData handshakeData = new HandshakeData(settings.getClientName(), settings.getPassword(),
                HandshakeData.SOFTWARE.CUSTOM, settings.getProtocolversion());
        this.client = new StarGateClient(settings.getRemoteAddress(), handshakeData, this);
        this.client.setClientListener(new StarGateListener(this));
        this.registerStarGateCodec(this.client.getProtocolCodec());
    }

    public void start() {
        this.client.start();
    }

    public void shutdown() {
        if (!this.client.isConnected()) {
            throw new IllegalStateException("Client is not connected");
        }
        this.client.shutdown();
    }

    private void registerStarGateCodec(ProtocolCodec codec) {
        codec.registerPacket(VortexPacketPool.VORTEX_GATE_PACKET, VortexGatePacket.class);
        codec.registerPacket(VortexPacketPool.VORTEX_CLIENT_HANDSHAKE_PACKET, VortexClientHandshakePacket.class);
    }

    public void onNodeCreated(VortexNode vortexNode, ClientSession session) {
        if (this.vortexNode != null) {
            throw new IllegalStateException("Vortex node was already initialized!");
        }
        log.info("Vortex node was successfully created");

        vortexNode.initialize(session.getChannel());
        this.vortexNode = vortexNode;

        if (this.listener != null) {
            this.listener.onNodeCreated(session.getAddress(), vortexNode);
        }
    }

    public void onNodeDisconnected(VortexNode vortexNode) {
        if (this.vortexNode != vortexNode) {
            throw new IllegalStateException("Incorrect Vortex node passed!");
        }
        this.vortexNode = null;

        if (this.listener != null) {
            this.listener.onNodeDisconnected(vortexNode);
        }
    }

    @Override
    public VortexClientSettings getSettings() {
        return this.settings;
    }

    public VortexNode getVortexNode() {
        return this.vortexNode;
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

    public boolean isClosed() {
        return !this.client.isConnected();
    }

    public boolean isConnected() {
        return this.vortexNode != null && !this.vortexNode.isClosed();
    }

    @Override
    public StarGateLogger getStarGateLogger() {
        return this.logger;
    }
}
