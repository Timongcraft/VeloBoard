package de.timongcraft.veloboard;

import com.google.common.annotations.Beta;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.ResetScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateTeamsPacket;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import de.timongcraft.velopacketimpl.utils.annotations.Since;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;

@SuppressWarnings("unused")
public class VeloBoard {

    public static final String VELOBOARD_ID = "veloboard";
    public static final String[] COLOR_CODES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r"};

    private final ConnectedPlayer player;
    private final String id;
    private Component title;
    @Since(MINECRAFT_1_20_3)
    private ComponentUtils.NumberFormat numberFormat;
    private final List<Component> lines = new ArrayList<>();
    private final Lock linesLock = new ReentrantLock();
    private boolean deleted = false;

    public VeloBoard(Player player) {
        this(player, Component.empty());
    }

    public VeloBoard(Player player, Component title) {
        this.player = (ConnectedPlayer) player;
        this.title = title;
        this.id = VELOBOARD_ID;
    }

    @Since(MINECRAFT_1_20_3)
    public VeloBoard(Player player, Component title, ComponentUtils.NumberFormat numberFormat) {
        this.player = (ConnectedPlayer) player;
        this.title = title;
        this.numberFormat = numberFormat;
        this.id = VELOBOARD_ID;
    }

    public void initialize() {
        sendObjectivePacket(UpdateObjectivesPacket.Mode.CREATE_SCOREBOARD);
        sendPacket(new DisplayObjectivePacket(1, id));
    }

    public void resend() {
        clear();

        initialize();
        updateTitle(title);
        linesLock.lock();
        try {
            for (int i = 0; i < lines.size(); ++i) {
                sendScorePacket(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                sendTeamPacket(i, UpdateTeamsPacket.Mode.CREATE_TEAM);
                sendLineChange(i);
            }
        } finally {
            linesLock.unlock();
        }
    }

    public Component getTitle() {
        return title;
    }

    public void updateTitle(Component title) {
        this.title = title;

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
    }

    @Since(MINECRAFT_1_20_3)
    public ComponentUtils.NumberFormat getNumberFormat() {
        return numberFormat;
    }

    @Since(MINECRAFT_1_20_3)
    public void setNumberFormat(ComponentUtils.NumberFormat numberFormat) {
        this.numberFormat = numberFormat;

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
    }

    /**
     * DEPRECATION NOTICE: This method is not thread-safe and may lead to concurrency issues.
     * It will be phased out in a future version of VeloBoard.
     *
     * <p>Replacement methods:
     * <br>{@link #getLinesCopy()}
     * <br>{@link #updateLinesWithoutUpdate(Collection)}
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public List<Component> getLines() {
        return lines;
    }

    /**
     * Returns an immutable view of the lines.
     *
     * <p>Note: To perform mutable operations on the lines, use {@link #updateLine(int, Component)},
     * {@link #updateLines(Component...)}, {@link #updateLines(Collection)}, or {@link #updateLinesWithoutUpdate(Collection)}
     *
     * @return an unmodifiable list of the current lines
     */
    @Beta
    public List<Component> getLinesCopy() {
        return Collections.unmodifiableList(lines);
    }

    public Component getLine(int lineNumber) {
        checkLineNumber(lineNumber, true, false);
        return lines.get(lineNumber);
    }

    /**
     * @see #updateLines(Component...)
     * @see #updateLines(Collection)
     */
    public void updateLine(int lineNumber, Component lineText) {
        checkLineNumber(lineNumber, false, true);

        List<Component> newLines;
        linesLock.lock();
        try {
            if (lineNumber < lines.size()) {
                lines.set(lineNumber, lineText);
                sendLineChange(getScoreByLine(lineNumber));
                return;
            }

            newLines = new ArrayList<>(lines);
            if (lineNumber > lines.size()) {
                for (int i = lines.size(); i < lineNumber; ++i) {
                    newLines.add(Component.empty());
                }
            }
        } finally {
            linesLock.unlock();
        }

        newLines.add(lineText);
        updateLines(newLines);
    }

    public void removeLine(int lineNumber) {
        checkLineNumber(lineNumber, false, false);
        if (lineNumber >= lines.size()) return;
        List<Component> newLines = new ArrayList<>(lines);

        newLines.remove(lineNumber);
        updateLines(newLines);
    }

    public void updateLines(Component... lines) {
        updateLines(Arrays.asList(lines));
    }

    public void updateLines(Collection<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        checkLineNumber(lines.size(), false, true);
        List<Component> oldLines = new ArrayList<>(this.lines);
        linesLock.lock();
        try {
            this.lines.clear();
            this.lines.addAll(lines);
        } finally {
            linesLock.unlock();
        }
        int linesSize = this.lines.size();

        if (oldLines.size() != linesSize) {
            if (oldLines.size() > linesSize) {
                for (int i = oldLines.size(); i > linesSize; i--) {
                    sendTeamPacket(i - 1, UpdateTeamsPacket.Mode.REMOVE_TEAM);
                    sendScorePacket(i - 1, UpdateScorePacket.Action.REMOVE_SCORE);

                    oldLines.remove(0);
                }
            } else {
                for (int i = oldLines.size(); i < linesSize; i++) {
                    sendScorePacket(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                    sendTeamPacket(i, UpdateTeamsPacket.Mode.CREATE_TEAM);
                }
            }
        }

        for (int i = 0; i < linesSize; ++i) {
            if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(i)))
                sendLineChange(i);
        }
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    @Beta
    public void updateLinesWithoutUpdate(Collection<Component> lines) {
        linesLock.lock();
        try {
            this.lines.clear();
            this.lines.addAll(lines);
        } finally {
            linesLock.unlock();
        }
    }

    private void sendLineChange(int score) {
        sendTeamPacket(score, UpdateTeamsPacket.Mode.UPDATE_TEAM_INFO, getLineByScore(score), Component.empty());
    }

    private void checkLineNumber(int lineNumber, boolean checkInRange, boolean checkMax) {
        if (lineNumber < 0)
            throw new IllegalArgumentException("Line number must be positive");

        if (checkInRange && lineNumber >= lines.size())
            throw new IllegalArgumentException("Line number must be under " + lines.size());

        if (checkMax && lineNumber >= COLOR_CODES.length - 1)
            throw new IllegalArgumentException("Line number is too high: " + lineNumber);
    }

    private int getScoreByLine(int line) {
        return lines.size() - line - 1;
    }

    private Component getLineByScore(int score) {
        return getLineByScore(lines, score);
    }

    private Component getLineByScore(List<Component> lines, int score) {
        return score < lines.size() ? lines.get(lines.size() - score - 1) : null;
    }

    private void sendObjectivePacket(UpdateObjectivesPacket.Mode mode) {
        sendPacket(
                new UpdateObjectivesPacket(
                        id,
                        mode,
                        player.translateMessage(title),
                        UpdateObjectivesPacket.Type.INTEGER,
                        numberFormat
                )
        );
    }

    private void sendScorePacket(int score, UpdateScorePacket.Action action) {
        sendPacket(
                action != UpdateScorePacket.Action.REMOVE_SCORE || player.getProtocolVersion().getProtocol() < MINECRAFT_1_20_3.getProtocol() ?
                        new UpdateScorePacket(
                                COLOR_CODES[score],
                                action,
                                id,
                                score
                        )
                        :
                        new ResetScorePacket(
                                COLOR_CODES[score],
                                id
                        )
        );
    }

    private void sendTeamPacket(int score, UpdateTeamsPacket.Mode mode) {
        sendTeamPacket(score, mode, Component.empty(), Component.empty());
    }

    private void sendTeamPacket(int score, UpdateTeamsPacket.Mode mode, Component prefix, Component suffix) {
        sendPacket(
                new UpdateTeamsPacket(
                        id + ':' + score,
                        mode,
                        Component.empty(),
                        EnumSet.noneOf(UpdateTeamsPacket.FriendlyFlag.class),
                        UpdateTeamsPacket.NameTagVisibility.ALWAYS,
                        UpdateTeamsPacket.CollisionRule.ALWAYS,
                        NamedTextColor.WHITE,
                        player.translateMessage(prefix),
                        player.translateMessage(suffix),
                        Collections.singletonList(COLOR_CODES[score])
                )
        );
    }

    private void sendPacket(MinecraftPacket packet) {
        if (deleted) {
            throw new IllegalStateException("This VeloBoard is deleted");
        } else {
            if (player.isActive())
                player.getConnection().write(packet);
        }
    }

    /**
     * Translates the component in the locale of the {@link VeloBoard}'s player.
     *
     * <p>DEPRECATION NOTICE: This method is deprecated because it simply calls {@code connectedPlayer.translateMessage(component)}.
     * Instead, use {@code ((ConnectedPlayer) player).translateMessage(component)} directly.
     *
     * <p>Note: This method will remain available for the foreseeable future, as its implementation relies on
     * Velocity's internal module, which may (and should) not be utilized by all projects.
     */
    @Deprecated(since = "1.4.0")
    public Component translateComponent(Component component) {
        return player.translateMessage(component);
    }

    public Player getPlayer() {
        return player;
    }

    public String getId() {
        return id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * DEPRECATION NOTICE: This method is deprecated because it simply calls {@code lines.size()}.
     * Instead, use {@code lines.size()} directly.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public int linesSize() {
        return lines.size();
    }

    public void clear() {
        linesLock.lock();
        try {
            for (int i = 0; i < this.lines.size(); ++i) {
                this.sendTeamPacket(i, UpdateTeamsPacket.Mode.REMOVE_TEAM);
            }
        } finally {
            linesLock.unlock();
        }

        this.sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
    }

    public void delete() {
        clear();
        title = null;
        linesLock.lock();
        try {
            lines.clear();
        } finally {
            linesLock.unlock();
        }
        numberFormat = null;

        this.deleted = true;
    }

}