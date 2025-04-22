package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import de.timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.ResetScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateTeamsPacket;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import de.timongcraft.velopacketimpl.utils.annotations.Since;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;

@SuppressWarnings("unused")
public class VeloBoard extends AbstractBoard {

    /**
     * DEPRECATION NOTICE: Now unused, and new variable will no longer be publicly accessible, as it should have no use.
     * <b>Will be removed in the minor release!
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public static final String VELOBOARD_ID = "veloboard";
    /**
     * DEPRECATION NOTICE: Will no longer be publicly accessible, as it should have no use.
     * <b>Will be removed in the minor release!
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public static final String[] COLOR_CODES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r"};

    private Component title;
    @Since(MINECRAFT_1_20_3)
    private @Nullable ComponentUtils.NumberFormat defaultNumberFormat;
    private final List<Component> lines = new ArrayList<>();

    public VeloBoard(Player player) {
        this(player, Component.empty());
    }

    public VeloBoard(Player player, Component title) {
        this(player, title, null);
    }

    @Since(MINECRAFT_1_20_3)
    public VeloBoard(Player player, Component title, @Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        super(player);
        this.title = title;
        this.defaultNumberFormat = defaultNumberFormat;
    }

    public void initialize() {
        sendObjectivePacket(UpdateObjectivesPacket.Mode.CREATE_SCOREBOARD);
        sendPacket(new DisplayObjectivePacket(1, id));
    }

    public void resend() {
        clear();

        initialize();
        linesLock.lock();
        try {
            for (int i = 0; i < linesSize(); ++i) {
                sendScorePacket(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                sendTeamPacket(i, UpdateTeamsPacket.Mode.CREATE_TEAM, getLineByScore(lines, i));
            }
        } finally {
            linesLock.unlock();
        }
    }

    /**
     * @see #updateLines(Component...)
     * @see #updateLines(Collection)
     */
    public void updateLine(int lineIndex, Component lineText) {
        checkLineIndex(lineIndex, false, true);

        List<Component> newLines;
        linesLock.lock();
        try {
            if (lineIndex < linesSize()) {
                lines.set(lineIndex, lineText);
                sendLineChange(getScoreByLine(lineIndex));
                return;
            }

            newLines = new ArrayList<>(lines);
            if (lineIndex > linesSize()) {
                for (int i = linesSize(); i < lineIndex; ++i) {
                    newLines.add(Component.empty());
                }
            }
        } finally {
            linesLock.unlock();
        }

        newLines.add(lineText);
        updateLines(newLines);
    }

    public void removeLine(int lineIndex) {
        checkLineIndex(lineIndex, false, false);
        if (lineIndex >= linesSize()) return;
        List<Component> newLines = new ArrayList<>(lines);

        newLines.remove(lineIndex);
        updateLines(newLines);
    }

    public void updateLines(Component... lines) {
        updateLines(Arrays.asList(lines));
    }

    public void updateLines(Collection<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        checkLineIndex(linesSize(), false, true);
        List<Component> oldLines = new ArrayList<>(this.lines);
        linesLock.lock();
        try {
            this.lines.clear();
            this.lines.addAll(lines);
        } finally {
            linesLock.unlock();
        }
        int linesSize = this.linesSize();

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
            if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(this.lines, i))) {
                sendLineChange(i);
            }
        }
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void updateLinesSilent(Collection<Component> lines) {
        linesLock.lock();
        try {
            this.lines.clear();
            this.lines.addAll(lines);
        } finally {
            linesLock.unlock();
        }
    }

    private void sendLineChange(int score) {
        sendTeamPacket(score, UpdateTeamsPacket.Mode.UPDATE_TEAM_INFO, getLineByScore(lines, score));
    }

    private void checkLineIndex(int lineIndex, boolean checkInRange, boolean checkMax) {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("Line index must be non-negative");
        }

        if (checkInRange && lineIndex >= linesSize()) {
            throw new IllegalArgumentException("Line index must be within the valid range (index >= 0 && index < " + linesSize() + ")");
        }

        if (checkMax && lineIndex >= COLOR_CODES.length - 1) {
            throw new IllegalArgumentException("Line index must be less than " + COLOR_CODES.length + ". For 'unlimited' lines, use SimpleBoard instead");
        }
    }

    @Override
    public void clear() {
        linesLock.lock();
        try {
            for (int i = 0; i < linesSize(); ++i) {
                sendTeamPacket(i, UpdateTeamsPacket.Mode.REMOVE_TEAM);
            }
        } finally {
            linesLock.unlock();
        }

        sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
    }

    @Override
    public void delete() {
        super.delete();
        title = null;
        linesLock.lock();
        try {
            lines.clear();
        } finally {
            linesLock.unlock();
        }
        defaultNumberFormat = null;
    }

    public Component getTitle() {
        return title;
    }

    public void updateTitle(Component title) {
        this.title = title;

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
    }

    @Since(MINECRAFT_1_20_3)
    public @Nullable ComponentUtils.NumberFormat getNumberFormat() {
        return defaultNumberFormat;
    }

    @Since(MINECRAFT_1_20_3)
    public void setNumberFormat(@Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        this.defaultNumberFormat = defaultNumberFormat;

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
    }

    /**
     * DEPRECATION NOTICE: This method is not thread-safe and may lead to concurrency issues.
     * It will be phased out in a future version of VeloBoard.
     *
     * <p>Replacement methods:
     * <br>{@link #getLinesCopy()}
     * <br>{@link #updateLinesSilent(Collection)}
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public List<Component> getLines() {
        return lines;
    }

    /**
     * Returns an immutable view of the lines.
     *
     * <p>Note: To perform mutable operations on the lines, use {@link #updateLine(int, Component)},
     * {@link #updateLines(Component...)}, {@link #updateLines(Collection)}, or {@link #updateLinesSilent(Collection)}
     *
     * @return an unmodifiable list of the current lines
     */
    public List<Component> getLinesCopy() {
        return Collections.unmodifiableList(lines);
    }

    public Component getLine(int lineIndex) {
        checkLineIndex(lineIndex, true, false);
        return lines.get(lineIndex);
    }

    private int getScoreByLine(int line) {
        return linesSize() - line - 1;
    }

    private Component getLineByScore(List<Component> lines, int score) {
        return score < linesSize() ? lines.get(linesSize() - score - 1) : null;
    }

    private void sendObjectivePacket(UpdateObjectivesPacket.Mode mode) {
        sendPacket(
                new UpdateObjectivesPacket(
                        id,
                        mode,
                        player.translateMessage(title),
                        UpdateObjectivesPacket.Type.INTEGER,
                        defaultNumberFormat
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
        sendTeamPacket(score, mode, Component.empty());
    }

    private void sendTeamPacket(int score, UpdateTeamsPacket.Mode mode, Component teamPrefix) {
        sendPacket(
                new UpdateTeamsPacket(
                        id + ':' + score,
                        mode,
                        Component.empty(),
                        EnumSet.noneOf(UpdateTeamsPacket.FriendlyFlag.class),
                        UpdateTeamsPacket.NameTagVisibility.ALWAYS,
                        UpdateTeamsPacket.CollisionRule.ALWAYS,
                        NamedTextColor.BLACK,
                        player.translateMessage(teamPrefix),
                        Component.empty(),
                        Collections.singletonList(COLOR_CODES[score])
                )
        );
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

    public int linesSize() {
        return lines.size();
    }

}