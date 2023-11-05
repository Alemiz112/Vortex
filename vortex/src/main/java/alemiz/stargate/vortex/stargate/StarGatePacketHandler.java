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

package alemiz.stargate.vortex.stargate;

import alemiz.stargate.server.ServerSession;
import alemiz.stargate.server.handler.ConnectedHandler;
import alemiz.stargate.vortex.VortexServer;
import alemiz.stargate.vortex.common.protocol.stargate.PacketHandler;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.ServerSideNode;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StarGatePacketHandler extends ConnectedHandler implements PacketHandler {

    private final VortexServer loader;
    private VortexNode node;

    public StarGatePacketHandler(ServerSession session, VortexServer loader) {
        super(session);
        this.loader = loader;
    }

    @Override
    public boolean handleVortexClientHandshake(VortexClientHandshakePacket packet) {
        try {
            this.node = this.loader.createNewNode(packet.getVortexType(), packet.getTopics(), this.session);
        } catch (Throwable t) {
            log.error("Unable to create new Vortex node!", t);
            this.session.disconnect("Vortex Internal Error");
        }

        if (this.node instanceof ServerSideNode) {
            ((ServerSideNode) this.node).handleClientHandshake(packet);
        }
        return true;
    }
}
