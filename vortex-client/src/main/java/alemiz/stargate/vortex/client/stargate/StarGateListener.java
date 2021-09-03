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

package alemiz.stargate.vortex.client.stargate;

import alemiz.stargate.client.ClientSession;
import alemiz.stargate.client.StarGateClientListener;
import alemiz.stargate.vortex.client.VortexClient;
import alemiz.stargate.vortex.client.data.VortexClientSettings;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeType;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;

import java.net.InetSocketAddress;

public class StarGateListener extends StarGateClientListener {

    private final VortexClient loader;

    public StarGateListener(VortexClient loader) {
        this.loader = loader;
    }

    @Override
    public void onSessionCreated(InetSocketAddress address, ClientSession session) {
        // unused
    }

    @Override
    public void onSessionAuthenticated(ClientSession session) {
        VortexClientSettings settings = this.loader.getSettings();
        VortexNodeType vortexType = VortexNodeType.fromString(settings.getVortexType());

        VortexClientHandshakePacket packet = new VortexClientHandshakePacket();
        packet.setVortexType(settings.getVortexType());
        packet.setPrimaryMasterNode(settings.getPrimaryMasterNode());
        if (settings.getMasterNodes() != null) {
            packet.getMasterNodes().addAll(settings.getMasterNodes());
        }
        session.sendPacket(packet);

        VortexNode vortexNode = vortexType.getFactory().newInstance(session, this.loader);
        this.loader.onNodeCreated(vortexNode, session);
    }

    @Override
    public void onSessionDisconnected(ClientSession session) {
        VortexNode vortexNode = this.loader.getVortexNode();
        if (vortexNode == null) {
            return;
        }

        vortexNode.onDisconnected();
        this.loader.onNodeDisconnected(vortexNode);
    }
}
