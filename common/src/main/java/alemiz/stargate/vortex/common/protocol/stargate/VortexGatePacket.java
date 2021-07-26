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
import alemiz.stargate.vortex.common.data.CompressionEnum;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
public class VortexGatePacket extends StarGatePacket implements ReferenceCounted {

    private short vortexPacketId;
    private ByteBuf payload;

    private CompressionEnum compression;
    private long uncompressedSize;

    @Override
    public void encodePayload(ByteBuf buffer) {
        buffer.writeShort(this.vortexPacketId);
        buffer.writeByte(this.compression.ordinal());
        if (this.compression != CompressionEnum.NO_COMPRESS) {
            PacketHelper.writeLong(buffer, this.uncompressedSize);
        }

        PacketHelper.writeInt(buffer, this.payload.readableBytes());
        buffer.writeBytes(this.payload);
    }

    @Override
    public void decodePayload(ByteBuf buffer) {
        this.vortexPacketId = buffer.readShort();
        this.compression = CompressionEnum.values()[buffer.readByte()];
        if (this.compression != CompressionEnum.NO_COMPRESS) {
            this.uncompressedSize = PacketHelper.readLong(buffer);
        }

        int size = PacketHelper.readInt(buffer);
        this.payload = buffer.readRetainedSlice(size);
    }

    @Override
    public boolean handle(StarGatePacketHandler handler) {
        // We should handle this packet from pipeline
        return false;
    }

    @Override
    public byte getPacketId() {
        return VortexPacketPool.VORTEX_GATE_PACKET;
    }

    @Override
    public int refCnt() {
        if (this.payload == null) {
            return 0;
        }
        return this.payload.refCnt();
    }

    @Override
    public VortexGatePacket retain() {
        if (this.payload != null) {
            this.payload.retain();
        }
        return this;
    }

    @Override
    public VortexGatePacket retain(int increment) {
        if (this.payload != null) {
            this.payload.retain(increment);
        }
        return this;
    }

    @Override
    public VortexGatePacket touch() {
        if (this.payload != null) {
            this.payload.touch();
        }
        return this;
    }

    @Override
    public VortexGatePacket touch(Object hint) {
        if (this.payload != null) {
            this.payload.touch(hint);
        }
        return this;
    }

    @Override
    public boolean release() {
        if (this.payload != null) {
            return this.payload.release();
        }
        return false;
    }

    @Override
    public boolean release(int decrement) {
        if (this.payload != null) {
            this.payload.release(decrement);
        }
        return false;
    }
}
