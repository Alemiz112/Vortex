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

package alemiz.stargate.vortex.minecraft.protocol.stargate;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.handler.SessionHandler;
import alemiz.stargate.protocol.ServerTransferPacket;
import alemiz.stargate.vortex.common.node.VortexMasterNode;
import alemiz.stargate.vortex.minecraft.node.MinecraftServerChildNode;
import alemiz.stargate.vortex.minecraft.protocol.packet.PlayerTransferPacket;

public class StarGateChildHandler extends SessionHandler<StarGateSession> {

    private final MinecraftServerChildNode node;

    public StarGateChildHandler(MinecraftServerChildNode node, StarGateSession session) {
        super(session);
        this.node = node;
    }

    @Override
    public boolean handleServerTransfer(ServerTransferPacket packet) {
        VortexMasterNode masterNode = node.getPrimaryMasterNode();
        if (masterNode == null) {
            return false;
        }

        PlayerTransferPacket transferPacket = new PlayerTransferPacket();
        transferPacket.setPlayerIdentifier(packet.getPlayerName());
        transferPacket.setTargetServer(packet.getTargetServer());
        transferPacket.setSenderNode(this.node.getNodeName());
        masterNode.sendPacket(transferPacket);
        return true;
    }
}
