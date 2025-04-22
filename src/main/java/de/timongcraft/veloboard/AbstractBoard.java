package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractBoard {

    private static final String BOARD_IDENTIFIER = "veloboard";

    protected final ConnectedPlayer player;
    protected final String id;
    protected final Lock linesLock = new ReentrantLock();
    private boolean deleted = false;

    public AbstractBoard(Player player) {
        this.player = (ConnectedPlayer) player;
        this.id = BOARD_IDENTIFIER + ":" + player.getUniqueId() + ":" + System.currentTimeMillis();
    }

    protected void sendPacket(MinecraftPacket packet) {
        if (deleted) {
            throw new IllegalStateException("This " + getClass().getSimpleName() + "  is deleted");
        } else {
            if (player.isActive()) {
                player.getConnection().write(packet);
            }
        }
    }

    public ConnectedPlayer getPlayer() {
        return player;
    }

    /**
     * Might no longer be accessible in the future
     */
    @ApiStatus.Internal
    public String getId() {
        return id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public abstract void clear();

    public void delete() {
        deleted = true;
        clear();
    }

}