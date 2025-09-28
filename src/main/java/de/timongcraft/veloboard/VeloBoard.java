package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import de.timongcraft.veloboard.utils.ListUtils;
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
import org.jetbrains.annotations.Unmodifiable;

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

    private static final String[] COLOR_CODES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r"};
    public static final int MAX_LINES_SIZE = COLOR_CODES.length;

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
        this.title = Objects.requireNonNull(title, "title");
        this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;
    }

    public void initialize() {
        withLock(() -> {
            sendObjectivePacket(UpdateObjectivesPacket.Mode.CREATE_SCOREBOARD);
            sendPacket(new DisplayObjectivePacket(1, id));
        });
    }

    public void resend() {
        withLock(() -> {
            clear();
            initialize();

            for (int i = 0; i < lines.size(); ++i) {
                sendScorePacketUnchecked(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                sendTeamPacketUnchecked(i, UpdateTeamsPacket.Mode.CREATE_TEAM, getLineByScoreUnchecked(lines, i));
            }
        });
    }

    @Override
    public void clear() {
        withLock(() -> {
            for (int i = 0; i < this.lines.size(); ++i) {
                sendTeamPacketUnchecked(i, UpdateTeamsPacket.Mode.REMOVE_TEAM);
            }

            sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
        });
    }

    @Override
    public void delete() {
        withLock(() -> {
            super.delete();
            title = null;
            lines.clear();
            defaultNumberFormat = null;
        });
    }

    public Component getLine(int lineIndex) {
        return withLock(() -> {
            checkLineIndexUnsafe(lineIndex, true, false);
            return lines.get(lineIndex);
        });
    }

    /**
     * @see #updateLines(Component...)
     * @see #updateLines(Collection)
     */
    public void updateLine(int lineIndex, Component lineText) {
        Objects.requireNonNull(lineText, "lineText");
        withLock(() -> {
            checkLineIndexUnsafe(lineIndex, false, true);

            if (lineIndex < lines.size()) {
                lines.set(lineIndex, lineText);
                sendLineChangeUnsafe(getScoreByLineUnsafe(lineIndex));
                return;
            }

            List<Component> newLines = new ArrayList<>(lines);
            ListUtils.setOrPad(newLines, lineIndex, lineText, Component::empty);
            updateLines(newLines);
        });
    }

    public void removeLine(int lineIndex) {
        withLock(() -> {
            checkLineIndexUnsafe(lineIndex, true, true);
            List<Component> newLines = new ArrayList<>(lines);

            newLines.remove(lineIndex);
            updateLines(newLines);
        });
    }

    /**
     * Returns an immutable view of the lines.
     *
     * <p>Note: To perform mutable operations on the lines, use {@link #updateLine(int, Component)},
     * {@link #updateLines(Component...)}, {@link #updateLines(Collection)}, or {@link #updateLinesSilent(Collection)}
     *
     * @return an unmodifiable list of the current lines
     */
    @Unmodifiable
    public List<Component> getLinesCopy() {
        return withLock(() -> List.copyOf(lines));
    }

    public int linesSize() {
        return withLock(lines::size);
    }

    public void updateLines(Component... lines) {
        updateLines(Arrays.asList(lines));
    }

    public void updateLines(Collection<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        for (Component component : lines) {
            Objects.requireNonNull(component, "lines contain null element");
        }
        withLock(() -> {
            checkLineIndexUnsafe(lines.size(), false, true);

            List<Component> oldLines = new ArrayList<>(this.lines);

            this.lines.clear();
            this.lines.addAll(lines);

            if (oldLines.size() != this.lines.size()) {
                if (oldLines.size() > this.lines.size()) {
                    for (int i = oldLines.size(); i > this.lines.size(); i--) {
                        sendTeamPacketUnchecked(i - 1, UpdateTeamsPacket.Mode.REMOVE_TEAM);
                        sendScorePacketUnchecked(i - 1, UpdateScorePacket.Action.REMOVE_SCORE);

                        oldLines.remove(0);
                    }
                } else {
                    for (int i = oldLines.size(); i < this.lines.size(); i++) {
                        sendScorePacketUnchecked(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                        sendTeamPacketUnchecked(i, UpdateTeamsPacket.Mode.CREATE_TEAM);
                    }
                }
            }

            for (int i = 0; i < this.lines.size(); ++i) {
                if (!Objects.equals(getLineByScoreUnchecked(oldLines, i), getLineByScoreUnchecked(this.lines, i))) {
                    sendLineChangeUnsafe(i);
                }
            }
        });
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void updateLinesSilent(Collection<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        for (Component component : lines) {
            Objects.requireNonNull(component, "lines contain null element");
        }
        withLock(() -> {
            this.lines.clear();
            this.lines.addAll(lines);
        });
    }

    public Component getTitle() {
        return withLock(() -> title);
    }

    public void updateTitle(Component title) {
        Objects.requireNonNull(title, "title");
        withLock(() -> {
            this.title = title;
            sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
        });
    }

    @Since(MINECRAFT_1_20_3)
    public @Nullable ComponentUtils.NumberFormat getNumberFormat() {
        return withLock(() -> defaultNumberFormat);
    }

    @Since(MINECRAFT_1_20_3)
    public void setNumberFormat(@Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        withLock(() -> {
            this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;

            sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
        });
    }

    /**
     * Translates the component in the locale of the {@link VeloBoard}'s player.
     *
     * <p>DEPRECATION NOTICE: This method is deprecated because it simply calls {@code connectedPlayer.translateMessage(component)}.
     * Instead, use {@code ((ConnectedPlayer) player).translateMessage(component)} or replacement code with the GlobalTranslator directly.
     */
    @Deprecated(since = "1.4.0", forRemoval = true)
    public Component translateComponent(Component component) {
        return player.translateMessage(component);
    }

    private void checkLineIndexUnsafe(int lineIndex, boolean checkInRange, boolean checkMax) {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("Line index must be non-negative");
        }

        if (checkInRange && lineIndex >= lines.size()) {
            throw new IllegalArgumentException("Line index must be within the valid range (index >= 0 && index < " + lines.size() + ")");
        }

        if (checkMax && lineIndex >= MAX_LINES_SIZE) {
            throw new IllegalArgumentException("Line index " + lineIndex + " must be less than " + MAX_LINES_SIZE + "." +
                    "For unlimited* lines, use SimpleBoard instead.");
        }
    }

    private int getScoreByLineUnsafe(int lineIndex) {
        return lines.size() - lineIndex - 1;
    }

    private static Component getLineByScoreUnchecked(List<Component> lines, int score) {
        return lines.get(lines.size() - score - 1);
    }

    private void sendLineChangeUnsafe(int score) {
        sendTeamPacketUnchecked(score, UpdateTeamsPacket.Mode.UPDATE_TEAM_INFO, getLineByScoreUnchecked(lines, score));
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

    private void sendScorePacketUnchecked(int score, UpdateScorePacket.Action action) {
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

    private void sendTeamPacketUnchecked(int score, UpdateTeamsPacket.Mode mode) {
        sendTeamPacketUnchecked(score, mode, Component.empty());
    }

    private void sendTeamPacketUnchecked(int score, UpdateTeamsPacket.Mode mode, Component teamPrefix) {
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

}