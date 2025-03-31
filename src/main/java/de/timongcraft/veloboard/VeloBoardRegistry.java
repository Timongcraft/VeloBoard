package de.timongcraft.veloboard;

import de.timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.ResetScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateTeamsPacket;

/**
 * Handles the registration of required components.
 *
 * <p>This class ensures that all necessary setups are performed, but users can
 * implement their own registration logic if preferred.
 */
@SuppressWarnings("unused")
public class VeloBoardRegistry {

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