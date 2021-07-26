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

package alemiz.stargate.vortex.common.protocol;

import alemiz.stargate.vortex.common.protocol.packet.*;
import alemiz.stargate.vortex.common.protocol.packet.VortexPacket;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VortexPacketPool {

    public static final byte VORTEX_GATE_PACKET = (byte) 0xdc;
    public static final byte VORTEX_CLIENT_HANDSHAKE_PACKET = (byte) 0xdd;

    public static final short VORTEX_LATENCY_PACKET = 0;
    public static final short VORTEX_CHILD_INFO_PACKET = 1;

    private final Short2ObjectMap<PacketFactory> packetFactoryMap = new Short2ObjectOpenHashMap<>();
    private final Object2ShortMap<Class<? extends VortexPacket>> packetIdMap = new Object2ShortOpenHashMap<>();

    public VortexPacketPool() {
        // Register default packets
        this.registerPacket(VortexLatencyPacket.class, VORTEX_LATENCY_PACKET, VortexLatencyPacket::new);
        this.registerPacket(VortexChildInfoPacket.class, VORTEX_CHILD_INFO_PACKET, VortexChildInfoPacket::new);
    }

    public <T extends VortexPacket> VortexPacket constructPacket(Class<T> packetClass) {
        if (!this.packetFactoryMap.containsKey(packetClass)) {
            throw new IllegalStateException("Packet " + packetClass.getSimpleName() + " is not registered!");
        }

        short packetId = this.packetIdMap.getShort(packetClass);
        return this.constructPacket(packetId);
    }

    public VortexPacket constructPacket(short packetId) {
        PacketFactory factory = this.packetFactoryMap.get(packetId);
        if (factory == null) {
            return null;
        }
        return factory.newInstance();
    }

    public <T extends VortexPacket> void registerPacket(Class<T> packetClass, short packetId, PacketFactory packetFactory) {
        if (packetClass == null || packetFactory == null) {
            throw new NullPointerException("Packet class and factory can not be null!");
        }

        if (packetId < 0) {
            throw new IllegalArgumentException("Packet ID can not be negative!");
        }

        if (this.packetFactoryMap.containsKey(packetId) || this.packetIdMap.containsKey(packetId)) {
            throw new IllegalStateException("Packet " + packetClass.getSimpleName() + " is already registered!");
        }

        this.packetFactoryMap.put(packetId, packetFactory);
        this.packetIdMap.put(packetClass, packetId);
    }

    public <T extends VortexPacket> void deregisterPacket(Class<T> packetClass) {
        if (packetClass == null) {
            throw new NullPointerException("Packet class can not be null!");
        }

        if (!this.packetIdMap.containsKey(packetClass)) {
            return;
        }

        short packetId = this.packetIdMap.removeShort(packetClass);
        this.packetFactoryMap.remove(packetId);
    }

}
