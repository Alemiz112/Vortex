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

package alemiz.stargate.vortex.minecraft.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.vortex.common.node.*;
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;

import static  alemiz.stargate.vortex.minecraft.Minecraft.MINECRAFT_SERVER_MASTER;

/**
 * During this Minecraft implementation we will asume that Master nodes will refer to transfer proxies and
 * that Child nodes will be the actual downstream servers known to the proxy.
 */
public class MinecraftServerMasterNode extends VortexMasterNode {

    public MinecraftServerMasterNode(StarGateSession session, VortexNodeOwner vortexParent) {
        super(session, vortexParent);
    }

    @Override
    protected boolean onMessagePacket(VortexMessagePacket packet) {
        if (packet.getTargetNode().isEmpty()) {
            for (VortexNode node : this.childNodes.values()) {
                node.sendPacket(packet);
            }
            return true;
        }

        VortexNode node = this.getChildNode(packet.getTargetNode());
        if (node == null || node.isClosed()) {
            return false;
        }
        node.sendPacket(packet);
        return true;
    }

    @Override
    public void registerChildNode(VortexNode node) {
        if (!(node instanceof MinecraftServerChildNode)) {
            throw new IllegalArgumentException("Minecraft master node can accept only Minecraft child node!");
        }
        super.registerChildNode(node);
    }

    @Override
    public MinecraftServerChildNode getChildNode(String name) {
        return (MinecraftServerChildNode) super.getChildNode(name);
    }

    @Override
    public VortexNodeType getVortexType() {
        return MINECRAFT_SERVER_MASTER;
    }
}
