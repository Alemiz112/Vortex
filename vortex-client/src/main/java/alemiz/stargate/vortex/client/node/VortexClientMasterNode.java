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

package alemiz.stargate.vortex.client.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.client.ClientSession;
import alemiz.stargate.vortex.client.VortexClient;
import alemiz.stargate.vortex.client.data.ChildNodeData;
import alemiz.stargate.vortex.common.node.ClientSideNode;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeParent;
import alemiz.stargate.vortex.common.node.VortexNodeType;
import alemiz.stargate.vortex.common.protocol.packet.VortexChildInfoPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static alemiz.stargate.vortex.client.VortexClient.DEFAULT_MASTER_NODE;

public class VortexClientMasterNode extends VortexNode implements ClientSideNode {
    private final Map<String, ChildNodeData> childDataMap = new ConcurrentHashMap<>();

    public VortexClientMasterNode(StarGateSession session, VortexNodeParent vortexParent) {
        super(session, vortexParent);
    }

    @Override
    protected boolean handleInternal(VortexPacket packet) {
        if (packet instanceof VortexChildInfoPacket) {
            return this.handleChildInfo((VortexChildInfoPacket) packet);
        }
        return super.handleInternal(packet);
    }

    private boolean handleChildInfo(VortexChildInfoPacket packet) {
        String nodeName = packet.getNodeName();
        switch (packet.getAction()) {
            case ADD:
                this.childDataMap.putIfAbsent(nodeName, new ChildNodeData(nodeName));
                break;
            case REMOVE:
                this.childDataMap.remove(nodeName);
                break;
        }
        return true;
    }

    public ChildNodeData getChildNodeData(String name) {
        return this.childDataMap.get(name);
    }

    public Collection<ChildNodeData> getChildNodes() {
        return Collections.unmodifiableCollection(this.childDataMap.values());
    }

    @Override
    public VortexClient getVortexParent() {
        return (VortexClient) super.getVortexParent();
    }

    @Override
    public VortexNodeType getVortexType() {
        return DEFAULT_MASTER_NODE;
    }

    @Override
    public String getName() {
        return ((ClientSession) this.session).getClient().getClientName();
    }
}
