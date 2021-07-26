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

import alemiz.stargate.protocol.DisconnectPacket;
import alemiz.stargate.server.ServerSession;
import alemiz.stargate.server.StarGateServer;
import alemiz.stargate.server.StarGateServerListener;
import alemiz.stargate.vortex.VortexServer;
import alemiz.stargate.vortex.common.node.VortexNode;

import java.net.InetSocketAddress;
import java.util.Collection;

public class StarGateListener extends StarGateServerListener {

    private final VortexServer loader;
    private final StarGateServer server;

    public StarGateListener(VortexServer loader, StarGateServer server) {
        this.loader = loader;
        this.server = server;
    }

    @Override
    public boolean onSessionCreated(InetSocketAddress address, ServerSession session) {
        return true;
    }

    @Override
    public void onSessionAuthenticated(ServerSession session) {
        Collection<ServerSession> sessions = this.server.getSessions().values();
        for (ServerSession oldSession : sessions) {
            if (session.getAddress() != oldSession.getAddress() && oldSession.getHandshakeData() != null &&
                    oldSession.getClientName().equals(session.getClientName())) {
                oldSession.disconnect(DisconnectPacket.REASON.ANOTHER_LOCATION_LOGIN);
            }
        }

        session.setPacketHandler(new StarGatePacketHandler(session, this.loader));
    }

    @Override
    public void onSessionDisconnected(ServerSession session) {
        this.loader.onNodeClosed(session);
    }
}
