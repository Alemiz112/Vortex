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

package alemiz.stargate.vortex.minecraft.protocol.packet;

import alemiz.stargate.protocol.types.PacketHelper;
import alemiz.stargate.vortex.common.protocol.VortexPacketListener;
import alemiz.stargate.vortex.common.protocol.packet.VortexMessagePacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.minecraft.Minecraft;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
public class PlayerTransferPacket extends VortexMessagePacket {

    /**
     * Usually name of the player or identifier proxy is aware of
     */
    private String playerIdentifier;

    /**
     * Name of the server player will be transferred to
     */
    private String targetServer;

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeString(buffer, this.playerIdentifier);
        PacketHelper.writeString(buffer, this.targetServer);
    }

    @Override
    public void decode(ByteBuf buffer) {
        this.playerIdentifier = PacketHelper.readString(buffer);
        this.targetServer = PacketHelper.readString(buffer);
    }

    @Override
    public short getPacketId() {
        return Minecraft.PLAYER_TRANSFER_PACKET;
    }
}
