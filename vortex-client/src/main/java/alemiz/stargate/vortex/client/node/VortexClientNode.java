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

package alemiz.stargate.vortex.client.node;

import alemiz.stargate.StarGateSession;
import alemiz.stargate.client.ClientSession;
import alemiz.stargate.vortex.client.VortexClient;
import alemiz.stargate.vortex.client.data.VortexClientSettings;
import alemiz.stargate.vortex.common.node.ClientSideNode;
import alemiz.stargate.vortex.common.node.VortexNode;
import alemiz.stargate.vortex.common.node.VortexNodeParent;
import alemiz.stargate.vortex.common.node.VortexNodeType;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

public class VortexClientNode extends VortexNode implements ClientSideNode {

    private final List<String> masterNodes = Collections.synchronizedList(new ObjectArrayList<>());

    public VortexClientNode(VortexNodeType vortexType, StarGateSession session, VortexNodeParent vortexParent) {
        super(vortexType, session, vortexParent);
    }

    @Override
    protected void initialize0(Channel channel) {
        VortexClientSettings settings = (VortexClientSettings) this.vortexParent.getSettings();
        if (settings.getMasterNodes() != null) {
            this.masterNodes.addAll(settings.getMasterNodes());
        }
    }

    @Override
    public VortexClient getVortexParent() {
        return (VortexClient) super.getVortexParent();
    }

    @Override
    public String getName() {
        return ((ClientSession) this.session).getClient().getClientName();
    }
}
