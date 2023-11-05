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
import alemiz.stargate.vortex.common.data.ChildNodeData;
import alemiz.stargate.vortex.common.node.*;
import alemiz.stargate.vortex.common.protocol.packet.VortexChildInfoPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static  alemiz.stargate.vortex.minecraft.Minecraft.MINECRAFT_CLIENT_MASTER;

/**
 * During this Minecraft implementation we will asume that Master nodes will refer to transfer proxies and
 * that Child nodes will be the actual downstream servers known to the proxy.
 */
public class MinecraftClientMasterNode extends VortexAbstractClientNode {

    private final Map<String, ChildNodeData> childDataMap = new ConcurrentHashMap<>();

    public MinecraftClientMasterNode(StarGateSession session, VortexNodeOwner vortexParent) {
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
        if (packet.getAction() == VortexChildInfoPacket.Action.REMOVE) {
            this.childDataMap.remove(nodeName);
            return true;
        }

        if (!this.childDataMap.containsKey(nodeName)) {
            ChildNodeData data = new ChildNodeData(nodeName);
            this.childDataMap.put(nodeName, data);
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
    public VortexNodeType getVortexType() {
        return MINECRAFT_CLIENT_MASTER;
    }
}
