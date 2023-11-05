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

package alemiz.stargate.vortex.common.protocol.stargate;

import alemiz.stargate.handler.StarGatePacketHandler;
import alemiz.stargate.protocol.StarGatePacket;
import alemiz.stargate.protocol.types.PacketHelper;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
public class VortexClientHandshakePacket extends StarGatePacket {

    private String vortexType;
    private String primaryMasterNode = "";
    private List<String> masterNodes = new ObjectArrayList<>();
    private List<String> topics = new ObjectArrayList<>();

    @Override
    public void encodePayload(ByteBuf buffer) {
        PacketHelper.writeString(buffer, this.vortexType);
        PacketHelper.writeString(buffer, this.primaryMasterNode);
        PacketHelper.writeArray(buffer, this.masterNodes, PacketHelper::writeString);
        PacketHelper.writeArray(buffer, this.topics, PacketHelper::writeString);
    }

    @Override
    public void decodePayload(ByteBuf buffer) {
        this.vortexType = PacketHelper.readString(buffer);
        this.primaryMasterNode = PacketHelper.readString(buffer);
        PacketHelper.readArray(buffer, this.masterNodes, PacketHelper::readString);
        PacketHelper.readArray(buffer, this.topics, PacketHelper::readString);
    }

    @Override
    public boolean handle(StarGatePacketHandler handler) {
        if (handler instanceof PacketHandler) {
            return ((PacketHandler) handler).handleVortexClientHandshake(this);
        }
        return super.handle(handler);
    }

    @Override
    public byte getPacketId() {
        return VortexPacketPool.VORTEX_CLIENT_HANDSHAKE_PACKET;
    }
}
