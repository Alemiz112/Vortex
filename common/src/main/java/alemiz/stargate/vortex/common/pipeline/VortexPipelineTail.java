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

import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VortexPipelineTail extends ChannelInboundHandlerAdapter {
    public static final String NAME = "vortex-pipeline-tail";

    private final VortexNode vortexNode;

    public VortexPipelineTail(VortexNode vortexNode) {
        this.vortexNode = vortexNode;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof VortexPacket) {
            log.debug("VortexPacket reached the tail of the VortexPipeline: " + msg.toString());
            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable error) throws Exception {
        log.error("Exception was caught in Vortex connection", error);
        this.vortexNode.disconnect("Vortex internal error");
    }
}
