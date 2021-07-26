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

import alemiz.stargate.vortex.common.VortexProtocolException;
import alemiz.stargate.vortex.common.data.CompressionEnum;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import alemiz.stargate.vortex.common.protocol.VortexPacketPool;
import alemiz.stargate.vortex.common.protocol.packet.VortexResponse;
import alemiz.stargate.vortex.common.protocol.stargate.VortexGatePacket;
import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.log4j.Log4j2;

import java.nio.ByteBuffer;
import java.util.List;

@Log4j2
public class VortexPacketDecoder extends MessageToMessageDecoder<VortexGatePacket> {
    public static final String NAME = "vortex-decoder";

    private final VortexPacketPool packetPool;

    public VortexPacketDecoder(VortexPacketPool packetPool) {
        this.packetPool = packetPool;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, VortexGatePacket packet, List<Object> out) throws Exception {
        VortexPacket vortexPacket = this.packetPool.constructPacket(packet.getVortexPacketId());
        if (vortexPacket == null) {
            log.debug("Received unknown Vortex packet with ID " + packet.getVortexPacketId());
            return;
        }

        ByteBuf decompressed = this.decompress(packet.getPayload(), packet.getCompression(), packet.getUncompressedSize());
        try {
            if (vortexPacket instanceof VortexResponse) {
                ((VortexResponse) vortexPacket).setResponseId(decompressed.readLong());
            }
            vortexPacket.decodePayload(decompressed);
            out.add(vortexPacket);
        } catch (Throwable t) {
            throw new VortexProtocolException("Unable to decode packet " + packet.getClass().getSimpleName(), t);
        } finally {
            if (decompressed != null) {
                decompressed.release();
            }
        }
    }

    private ByteBuf decompress(ByteBuf buffer, CompressionEnum compression, long decompressedSize) {
        if (compression == CompressionEnum.NO_COMPRESS) {
            return buffer.retain();
        }

        ByteBuf decompressed = buffer.alloc().directBuffer((int) decompressedSize);
        if (buffer.hasMemoryAddress()) {
            Zstd.decompressUnsafe(decompressed.memoryAddress(), decompressedSize, buffer.memoryAddress() + buffer.readerIndex(), buffer.readableBytes());
        } else {
            ByteBuffer compressedNio = buffer.nioBuffer(buffer.readerIndex(), buffer.readableBytes());
            ByteBuffer decompressedNio = decompressed.nioBuffer(0, (int) decompressedSize);
            Zstd.decompress(decompressedNio, compressedNio);
        }

        decompressed.writerIndex((int) decompressedSize);
        return decompressed;
    }
}
