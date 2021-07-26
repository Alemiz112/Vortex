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
public abstract class VortexMessagePacket extends VortexPacket {

    /**
     * Name of the master/child node which should rceive the message.
     * Leave empty for all nodes assigned nodes
     */
    private String targetNode = "";

    /**
     * Name of the node which sends the message.
     */
    private String senderNode;


    @Override
    public final void encodePayload(ByteBuf buffer) {
        PacketHelper.writeString(buffer, this.targetNode);
        PacketHelper.writeString(buffer, this.senderNode);
        this.encode(buffer);
    }

    public abstract void encode(ByteBuf buffer);

    @Override
    public final void decodePayload(ByteBuf buffer) {
        this.targetNode = PacketHelper.readString(buffer);
        this.senderNode = PacketHelper.readString(buffer);
        this.decode(buffer);
    }

    public abstract void decode(ByteBuf buffer);

    @Override
    public boolean handle(VortexPacketListener listener) {
        return false;
    }
}
