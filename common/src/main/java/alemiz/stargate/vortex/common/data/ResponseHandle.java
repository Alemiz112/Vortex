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

package alemiz.stargate.vortex.common.data;

import alemiz.stargate.vortex.common.protocol.packet.VortexResponse;
import io.netty.util.concurrent.Promise;

public class ResponseHandle {

    private final long sendTime;
    private final Promise<VortexResponse> promise;

    public ResponseHandle(long sendTime, Promise<VortexResponse> promise) {
        this.sendTime = sendTime;
        this.promise = promise;
    }

    public long getSendTime() {
        return this.sendTime;
    }

    public Promise<VortexResponse> getPromise() {
        return this.promise;
    }
}
