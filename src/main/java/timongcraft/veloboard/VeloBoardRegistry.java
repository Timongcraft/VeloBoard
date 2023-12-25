package timongcraft.veloboard;

import timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import timongcraft.velopacketimpl.network.protocol.packets.UpdateTeamsPacket;

@SuppressWarnings("unused")
public class VeloBoardRegistry {

    //TODO: Add java docs

    public static void register() {
        registerPackets();
    }

    private static void registerPackets() {
        DisplayObjectivePacket.register(true);
        UpdateObjectivesPacket.register(true);
        UpdateScorePacket.register(true);
        UpdateTeamsPacket.register(true);
    }

}