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

package alemiz.stargate.vortex.common.protocol.packet;

import alemiz.stargate.protocol.types.PacketHelper;
import alemiz.stargate.vortex.common.protocol.VortexPacketListener;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
public class VortexTopicSubscribePacket implements VortexPacket {

    private String topic;
    private boolean unsubscribe;

    @Override
    public void encodePayload(ByteBuf buffer) {
        PacketHelper.writeString(buffer, this.topic);
        buffer.writeBoolean(this.unsubscribe);
    }

    @Override
    public void decodePayload(ByteBuf buffer) {
        this.topic = PacketHelper.readString(buffer);
        this.unsubscribe = buffer.readBoolean();
    }

    @Override
    public boolean handle(VortexPacketListener listener) {
        return false;
    }

    @Override
    public short getPacketId() {
        return VortexPacketPool.VORTEX_TOPIC_SUBSCRIBE_PACKET;
    }
}
