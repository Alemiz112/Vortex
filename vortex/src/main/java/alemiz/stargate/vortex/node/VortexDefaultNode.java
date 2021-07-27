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

package alemiz.stargate.vortex.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.server.ServerSession;
import alemiz.stargate.vortex.VortexServer;
import alemiz.stargate.vortex.common.node.*;
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static alemiz.stargate.vortex.VortexServer.DEFAULT_CHILD_NODE;

@Log4j2
public class VortexDefaultNode extends VortexChildNode {

    public VortexDefaultNode(StarGateSession session, VortexNodeParent vortexParent) {
        super(session, vortexParent);
    }

    @Override
    public void handleClientHandshake(VortexClientHandshakePacket handshake) {
        for (String nodeName : handshake.getMasterNodes()) {
            VortexNode masterNode = this.getVortexParent().getVortexNode(nodeName);
            if (!(masterNode instanceof VortexMasterNode)) {
                log.warn(nodeName + " is not master node!");
            } else {
                this.registerToMasterNode((VortexMasterNode) masterNode);
            }
        }

        String masterNodeName = handshake.getPrimaryMasterNode();
        if (masterNodeName != null && !masterNodeName.isEmpty()) {
            this.setPrimaryMasterNode(masterNodeName);
        }
    }

    @Override
    public VortexServer getVortexParent() {
        return (VortexServer) super.getVortexParent();
    }

    @Override
    public VortexNodeType getVortexType() {
        return DEFAULT_CHILD_NODE;
    }

    @Override
    public String getName() {
        return ((ServerSession) this.session).getClientName();
    }
}
