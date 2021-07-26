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

package alemiz.stargate.vortex.common.pipeline;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.vortex.common.VortexProtocolException;
import alemiz.stargate.vortex.common.data.CompressionEnum;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.packet.VortexResponse;
import alemiz.stargate.vortex.common.protocol.stargate.VortexGatePacket;
import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.ByteBuffer;
import java.util.List;

public class VortexPacketEncoder extends MessageToMessageEncoder<VortexPacket> {
    public static final String NAME = "vortex-encoder";

    private final StarGateSession session;
    private final CompressionEnum compression;
    private final int compressionLevel;

    public VortexPacketEncoder(StarGateSession session, CompressionEnum compression, int compressionLevel) {
        this.session = session;
        this.compression = compression;
        this.compressionLevel = compressionLevel;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, VortexPacket vortexPacket, List<Object> out) throws Exception {
        ByteBuf buffer = ctx.channel().alloc().ioBuffer();
        try {
            if (vortexPacket instanceof VortexResponse) {
                buffer.writeLong(((VortexResponse) vortexPacket).getResponseId());
            }
            vortexPacket.encodePayload(buffer);

            int decompressedSize = buffer.readableBytes();
            ByteBuf payload = this.compress(buffer);

            VortexGatePacket packet = new VortexGatePacket();
            packet.setVortexPacketId(vortexPacket.getPacketId());
            packet.setCompression(this.compression);
            packet.setUncompressedSize(decompressedSize);
            packet.setPayload(payload);
            out.add(packet);
        } catch (Throwable t) {
            throw new VortexProtocolException("Unable to encode packet " + vortexPacket.getClass().getSimpleName(), t);
        } finally {
            buffer.release();
        }
    }

    private ByteBuf compress(ByteBuf buffer) {
        if (compression == CompressionEnum.NO_COMPRESS) {
            return buffer.retain();
        }

        int decompressedSize =  buffer.readableBytes();
        int compressedSize = (int) Zstd.compressBound(decompressedSize);

        ByteBuf compressed = buffer.alloc().directBuffer(compressedSize);

        if (buffer.hasMemoryAddress()) {
            compressedSize = (int) Zstd.compressUnsafe(compressed.memoryAddress(), compressedSize, buffer.memoryAddress() + buffer.readerIndex(),
                    decompressedSize, this.compressionLevel);
        } else {
            ByteBuffer compressedNio = compressed.nioBuffer(0, compressedSize);
            ByteBuffer decompressedNio = buffer.nioBuffer(buffer.readerIndex(), buffer.readableBytes());
            compressedSize = Zstd.compress(compressedNio, decompressedNio, this.compressionLevel);
        }

        compressed.writerIndex(compressedSize);
        return compressed;
    }
}
