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
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.stargate.VortexClientHandshakePacket;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VortexChildNode extends VortexNode implements ServerSideNode {

    private String primaryMasterNode;
    private final Map<String, VortexMasterNode> masterNodes = new ConcurrentHashMap<>();

    public VortexChildNode(StarGateSession session, VortexNodeParent vortexParent) {
        super(session, vortexParent);
    }

    @Override
    protected void deinitialize0() {
        for (VortexMasterNode masterNode : this.masterNodes.values()) {
            this.unregisterFromMasterNode(masterNode);
        }
    }

    @Override
    protected boolean handleInternal(VortexPacket packet) {
        if (packet instanceof VortexMessagePacket && this.onMessagePacket((VortexMessagePacket) packet)) {
            return true;
        }
        return super.handleInternal(packet);
    }

    private boolean onMessagePacket(VortexMessagePacket packet) {
        if (packet.getTargetNode().isEmpty()) {
            for (VortexMasterNode masterNode : this.masterNodes.values()) {
                masterNode.sendPacket(packet);
            }
            return true;
        }

        VortexMasterNode masterNode = this.getMasterNode(packet.getTargetNode());
        if (masterNode == null || masterNode.isClosed()) {
            return false;
        }

        masterNode.sendPacket(packet);
        return true;
    }

    public boolean registerToMasterNode(VortexMasterNode masterNode) {
        if (this.masterNodes.containsKey(masterNode.getName())) {
            return false;
        }
        masterNode.registerChildNode(this);

        this.masterNodes.put(masterNode.getName(), masterNode);
        return true;
    }

    public boolean unregisterFromMasterNode(VortexMasterNode masterNode) {
        if (this.masterNodes.remove(masterNode.getName()) == null) {
            return false;
        }
        masterNode.unregisterChildNode(this);
        return true;
    }

    public VortexMasterNode getMasterNode(String nodeName) {
        return this.masterNodes.get(nodeName);
    }

    public Collection<VortexMasterNode> getMasterNodes() {
        return Collections.unmodifiableCollection(this.masterNodes.values());
    }

    public VortexMasterNode getPrimaryMasterNode() {
        if (this.primaryMasterNode == null) {
            return null;
        }
        return this.getMasterNode(this.primaryMasterNode);
    }

    public void setPrimaryMasterNode(String primaryMasterNode) {
        this.primaryMasterNode = primaryMasterNode;
    }
}
