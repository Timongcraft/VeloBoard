package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public abstract class AbstractBoard {

    private static final String BOARD_IDENTIFIER = "veloboard";

    protected final ConnectedPlayer player;
    protected final String id;
    private final Lock lock = new ReentrantLock();
    private volatile boolean deleted = false;

    public AbstractBoard(Player player) {
        Objects.requireNonNull(player, "player");
        this.player = (ConnectedPlayer) player;
        this.id = BOARD_IDENTIFIER + ":" + player.getUniqueId();
    }

    protected void sendPacket(MinecraftPacket packet) {
        if (player.isActive()) {
            player.getConnection().write(packet);
        }
    }

    public Player getPlayer() {
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

    protected void delete() {
        clear();
        deleted = true;
    }

    protected void withLock(Runnable action) {
        lock.lock();
        try {
            if (deleted) {
                throw new IllegalStateException("This " + getClass().getSimpleName() + " is deleted");
            }

            action.run();
        } finally {
            lock.unlock();
        }
    }

    protected <T> T withLock(Supplier<T> action) {
        lock.lock();
        try {
            if (deleted) {
                throw new IllegalStateException("This " + getClass().getSimpleName() + " is deleted");
            }

            return action.get();
        } finally {
            lock.unlock();
        }
    }

}