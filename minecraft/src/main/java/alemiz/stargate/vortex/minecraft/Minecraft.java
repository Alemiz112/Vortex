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

package alemiz.stargate.vortex.minecraft;

import alemiz.stargate.vortex.common.node.VortexNodeType;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import alemiz.stargate.vortex.minecraft.node.MinecraftClientChildNode;
import alemiz.stargate.vortex.minecraft.node.MinecraftClientMasterNode;
import alemiz.stargate.vortex.minecraft.node.MinecraftServerChildNode;
import alemiz.stargate.vortex.minecraft.node.MinecraftServerMasterNode;
import alemiz.stargate.vortex.minecraft.protocol.packet.PlayerTransferPacket;

public class Minecraft {

    public static VortexNodeType MINECRAFT_SERVER_CHILD;
    public static VortexNodeType MINECRAFT_SERVER_MASTER;

    public static VortexNodeType MINECRAFT_CLIENT_CHILD;
    public static VortexNodeType MINECRAFT_CLIENT_MASTER;

    public static void initServerTypes() {
        MINECRAFT_SERVER_CHILD = VortexNodeType.from("minecraft", MinecraftServerChildNode::new);
        MINECRAFT_SERVER_MASTER = VortexNodeType.from("minecraft-master", MinecraftServerMasterNode::new);
    }

    public static void initClientTypes() {
        MINECRAFT_CLIENT_CHILD = VortexNodeType.from("minecraft", MinecraftClientChildNode::new);
        MINECRAFT_CLIENT_MASTER = VortexNodeType.from("minecraft-master", MinecraftClientMasterNode::new);
    }

    public static final short PLAYER_TRANSFER_PACKET = 256;

    public static void registerPackets(VortexPacketPool packetPool) {
        packetPool.registerPacket(PlayerTransferPacket.class, PLAYER_TRANSFER_PACKET, PlayerTransferPacket::new);
    }
}
