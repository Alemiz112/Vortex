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

package alemiz.stargate.vortex.common.node;

import lombok.Data;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@ToString(exclude = {"factory"})
public class VortexNodeType implements Comparable<VortexNodeType> {

    private static final Map<String, VortexNodeType> vortexTypes = Collections.synchronizedMap(new HashMap());

    private final String name;
    private final VortexNodeFactory factory;

    private VortexNodeType(String name, VortexNodeFactory factory) {
        this.name = name;
        this.factory = factory;
    }

    public static VortexNodeType fromString(String vortexName) {
        assert vortexName != null;

        VortexNodeType vortexType = vortexTypes.get(vortexName);
        if (vortexType == null) {
            throw new NullPointerException("Vortex " + vortexName + " was not found!");
        }
        return vortexType;
    }

    public static VortexNodeType from(String vortexName, VortexNodeFactory factory) {
        assert vortexName != null;
        assert factory != null;

        VortexNodeType vortexType = vortexTypes.get(vortexName);
        if (vortexType == null) {
            vortexTypes.put(vortexName, vortexType = new VortexNodeType(vortexName, factory));
        }
        return vortexType;
    }


    public static Collection<VortexNodeType> values() {
        return Collections.unmodifiableCollection(vortexTypes.values());
    }

    @Override
    public int compareTo(VortexNodeType vortexType) {
        return this.name.compareTo(vortexType.getName());
    }

}
