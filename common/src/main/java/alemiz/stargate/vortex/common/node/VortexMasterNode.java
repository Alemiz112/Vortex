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
import alemiz.stargate.vortex.common.protocol.packet.VortexChildInfoPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VortexMasterNode extends VortexNode implements ServerSideNode {

    protected final Map<String, VortexNode> childNodes = new ConcurrentHashMap<>();

    public VortexMasterNode(StarGateSession session, VortexNodeOwner vortexParent) {
        super(session, vortexParent);
    }

    @Override
    protected void deinitialize0() {
        for (VortexNode node : this.childNodes.values()) {
            if (node instanceof VortexChildNode) {
                ((VortexChildNode) node).unregisterFromMasterNode(this);
            }
        }
    }

    @Override
    protected boolean onMessagePacket(VortexMessagePacket packet) {
        // Broadcast any message recived from master to all child nodes
        for (VortexNode vortexNode : this.childNodes.values()) {
            vortexNode.sendPacket(packet);
        }
        return true;
    }

    public void registerChildNode(VortexNode node) {
        this.childNodes.put(node.getNodeName(), node);

        VortexNodeListener listener = this.getVortexParent().getNodeListener();
        if (listener != null) {
            listener.onChildNodeRegsiter(node, this);
        }

        VortexChildInfoPacket packet = new VortexChildInfoPacket();
        packet.setNodeName(node.getNodeName());
        packet.setAction(VortexChildInfoPacket.Action.ADD);
        this.sendPacket(packet);
    }

    public void unregisterChildNode(VortexNode node) {
        this.childNodes.remove(node.getNodeName());

        VortexNodeListener listener = this.getVortexParent().getNodeListener();
        if (listener != null) {
            listener.onChildNodeUnregister(node, this);
        }

        // Do not nottify client side when channel is already closed
        if (!this.isClosed()) {
            VortexChildInfoPacket packet = new VortexChildInfoPacket();
            packet.setNodeName(node.getNodeName());
            packet.setAction(VortexChildInfoPacket.Action.REMOVE);
            this.sendPacket(packet);
        }
    }

    public VortexNode getChildNode(String name) {
        return this.childNodes.get(name);
    }

    @Override
    public VortexServerNodeOwner getVortexParent() {
        return (VortexServerNodeOwner) super.getVortexParent();
    }
}
