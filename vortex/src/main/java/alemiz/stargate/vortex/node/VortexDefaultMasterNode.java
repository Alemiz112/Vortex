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
import alemiz.stargate.vortex.common.node.VortexMasterNode;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeParent;
import alemiz.stargate.vortex.common.node.VortexNodeType;

import java.util.Map;

import static alemiz.stargate.vortex.VortexServer.DEFAULT_MASTER_NODE;

public class VortexDefaultMasterNode extends VortexMasterNode {

    public VortexDefaultMasterNode(StarGateSession session, VortexNodeParent vortexParent) {
        super(session, vortexParent);
    }

    @Override
    public VortexServer getVortexParent() {
        return (VortexServer) super.getVortexParent();
    }

    @Override
    public VortexNodeType getVortexType() {
        return DEFAULT_MASTER_NODE;
    }

    @Override
    public String getName() {
        return ((ServerSession) this.session).getClientName();
    }
}
