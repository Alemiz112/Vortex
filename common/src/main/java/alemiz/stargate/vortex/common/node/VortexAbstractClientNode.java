/*
 * Copyright 2023 Alemiz
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

package alemiz.stargate.vortex.common.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.vortex.common.protocol.packet.VortexTopicSubscribePacket;

public abstract class VortexAbstractClientNode extends VortexNode implements ClientSideNode {

    public VortexAbstractClientNode(StarGateSession session, VortexNodeOwner vortexParent) {
        super(session, vortexParent);
    }

    @Override
    protected void subscribe0(String topic) {
        VortexTopicSubscribePacket packet = new VortexTopicSubscribePacket();
        packet.setTopic(topic.trim());
        this.sendPacket(packet);
    }

    @Override
    protected void unsubscribe0(String topic) {
        VortexTopicSubscribePacket packet = new VortexTopicSubscribePacket();
        packet.setTopic(topic.trim());
        packet.setUnsubscribe(true);
        this.sendPacket(packet);
    }
}
