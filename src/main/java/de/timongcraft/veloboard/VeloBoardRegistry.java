package de.timongcraft.veloboard;

import timongcraft.velopacketimpl.network.protocol.packets.*;

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
        ResetScorePacket.register(true);
        UpdateTeamsPacket.register(true);
    }

}