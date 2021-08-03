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
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public abstract class VortexChildNode extends VortexNode implements ServerSideNode {

    private String primaryMasterNode;
    protected final Map<String, VortexMasterNode> masterNodes = new ConcurrentHashMap<>();

    public VortexChildNode(StarGateSession session, VortexNodeOwner vortexParent) {
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
    protected void deinitialize0() {
        for (VortexMasterNode masterNode : this.masterNodes.values()) {
            this.unregisterFromMasterNode(masterNode);
        }
    }

    @Override
    protected boolean onMessagePacket(VortexMessagePacket packet) {
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
        if (this.masterNodes.containsKey(masterNode.getNodeName()) || masterNode.isClosed()) {
            return false;
        }
        this.masterNodes.put(masterNode.getNodeName(), masterNode);
        masterNode.registerChildNode(this);
        return true;
    }

    public boolean unregisterFromMasterNode(VortexMasterNode masterNode) {
        if (this.masterNodes.remove(masterNode.getNodeName()) == null) {
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

    @Override
    public VortexServerNodeOwner getVortexParent() {
        return (VortexServerNodeOwner) super.getVortexParent();
    }
}
