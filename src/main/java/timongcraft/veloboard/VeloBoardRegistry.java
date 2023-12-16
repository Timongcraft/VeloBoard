package timongcraft.veloboard;

import timongcraft.veloboard.network.protocol.packets.*;

@SuppressWarnings("unused")
public class VeloBoardRegistry {

    //TODO: Add implementation for styled number format
    //TODO: Add java docs
    //Decide on using ComponentHolder directly

    public static void register() {
        registerPackets();
    }

    private static void registerPackets() {
        DisplayObjectivePacket.register();
        UpdateObjectivesPacket.register();
        UpdateScorePacket.register();
        UpdateTeamsPacket.register();
    }

}