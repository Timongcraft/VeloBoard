package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import de.timongcraft.veloboard.utils.ListUtils;
import de.timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.ResetScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import de.timongcraft.velopacketimpl.utils.annotations.Since;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;

/**
 * <p><b>Note</b>: With this board, you are no longer limited in line size* and avoid the overhead of teams,
 * as the normal {@link de.timongcraft.veloboard.VeloBoard} requires a team to be created for each line, which is no longer necessary due to new display-name introduced in 1.20.3.
 */
@SuppressWarnings("unused")
@Since(MINECRAFT_1_20_3)
public class SimpleBoard extends AbstractBoard {

    private ComponentHolder title;
    private @Nullable ComponentUtils.NumberFormat defaultNumberFormat;
    private final List<LinesEntry> lines = new ArrayList<>();
    private final LinesEntry EMPTY_ENTRY;

    public SimpleBoard(Player player) {
        this(player, Component.empty());
    }

    public SimpleBoard(Player player, Component title) {
        this(player, title, null);
    }

    public SimpleBoard(Player player, Component title, @Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        super(player);
        setTitleSilent(Objects.requireNonNull(title, "title"));
        this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;
        EMPTY_ENTRY = new LinesEntry(new ComponentHolder(player.getProtocolVersion(), Component.empty()), null);
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
                sendLineChangeUnsafe(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
            }
        });
    }

    @Override
    public void clear() {
        withLock(() -> sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD));
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

    public @Nullable Component getLineComponent(int lineIndex) {
        return withLock(() -> {
            checkLineIndexUnsafe(lineIndex, true);
            return lines.get(lineIndex) != null ? lines.get(lineIndex).getComponent() : null;
        });
    }

    public @Nullable LinesEntry getLine(int lineIndex) {
        return withLock(() -> {
            checkLineIndexUnsafe(lineIndex, true);
            return lines.get(lineIndex) != null ? lines.get(lineIndex) : null;
        });
    }

    /**
     * @see #setLineComponents(Component...)
     * @see #setLines(Collection)
     */
    public void setLineComponent(int lineIndex, Component lineComponent) {
        Objects.requireNonNull(lineComponent, "lineComponent");
        setLine(lineIndex, new LinesEntry(lineComponent, null, player));
    }

    public void setLine(int lineIndex, LinesEntry line) {
        Objects.requireNonNull(line, "line");
        withLock(() -> {
            checkLineIndexUnsafe(lineIndex, false);
            LinesEntry linesEntry = new LinesEntry(new ComponentHolder(player.getProtocolVersion(), player.translateMessage(line.getComponent())), line.formatCompiled(player.getProtocolVersion()));

            if (lineIndex < lines.size()) {
                lines.set(lineIndex, linesEntry);
                sendLineChangeUnsafe(getScoreByLineUnsafe(lineIndex), UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                return;
            }

            List<LinesEntry> newLines = new ArrayList<>(lines);
            ListUtils.setOrPad(newLines, lineIndex, line, () -> EMPTY_ENTRY);
            setLines(newLines);
        });
    }

    public void removeLine(int lineIndex) {
        withLock(() -> {
            checkLineIndexUnsafe(lineIndex, true);
            List<LinesEntry> newLines = new ArrayList<>(lines);

            newLines.remove(lineIndex);
            setLines(newLines);
        });
    }

    /**
     * Returns an immutable view of the line's components.
     *
     * <p>Note: To perform mutable operations on the lines, use {@link #setLineComponent(int, Component)},
     * {@link #setLineComponents(Component...)}, {@link #setLineComponents(Collection)}, or {@link #setLinesComponentsSilent(Collection)}
     *
     * @return an unmodifiable list of the current lines
     * @see #getLines
     */
    @Unmodifiable
    public List<Component> getLineComponents() {
        return withLock(() -> lines.stream().map(LinesEntry::getComponent).toList());
    }

    /**
     * Returns an immutable view of the lines.
     *
     * <p>Note: To perform mutable operations on the lines, use {@link #setLine(int, LinesEntry)},
     * {@link #setLines(LinesEntry...)}, {@link #setLines(Collection)} or {@link #setLinesSilent(Collection)}
     *
     * @return an unmodifiable list of the current lines
     * @see #getLineComponents
     */
    @Unmodifiable
    public List<LinesEntry> getLines() {
        return withLock(() -> List.copyOf(lines));
    }

    public int linesSize() {
        return withLock(lines::size);
    }

    public void setLineComponents(Component... lineComponents) {
        setLineComponents(Arrays.asList(lineComponents));
    }

    public void setLineComponents(Collection<Component> lineComponents) {
        Objects.requireNonNull(lineComponents, "lineComponents");
        withLock(() -> {
            checkLineIndexUnsafe(lines.size(), false);
            List<LinesEntry> oldLines = new ArrayList<>(lines);

            setLinesComponentsSilent(lineComponents);

            updateScoreboard(oldLines);
        });
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void setLinesComponentsSilent(Collection<Component> lines) {
        withLock(() -> {
            this.lines.clear();
            this.lines.addAll(lines.stream().map(c -> new LinesEntry(
                    new ComponentHolder(player.getProtocolVersion(), player.translateMessage(c)),
                    null)).toList());
        });
    }

    public void setLines(LinesEntry... lines) {
        setLines(Arrays.asList(lines));
    }

    public void setLines(Collection<LinesEntry> lines) {
        Objects.requireNonNull(lines, "lines");
        withLock(() -> {
            checkLineIndexUnsafe(lines.size(), false);
            List<LinesEntry> oldLines = new ArrayList<>(this.lines);

            this.lines.clear();
            this.lines.addAll(lines);

            updateScoreboard(oldLines);
        });
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     *
     * @see #setLinesSilent(Collection)
     */
    public void setLinesSilent(LinesEntry... lines) {
        setLinesSilent(Arrays.asList(lines));
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void setLinesSilent(Collection<LinesEntry> lines) {
        withLock(() -> {
            this.lines.clear();
            this.lines.addAll(lines);
        });
    }

    public Component getTitle() {
        return withLock(() -> title.getComponent());
    }

    public void setTitle(Component title) {
        Objects.requireNonNull(title, "title");
        withLock(() -> {
            setTitleSilent(title);
            sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
        });
    }

    private void setTitleSilent(Component newTitle) {
        title = new ComponentHolder(player.getProtocolVersion(), player.translateMessage(newTitle));
    }

    public @Nullable ComponentUtils.NumberFormat getDefaultNumberFormat() {
        return withLock(() -> defaultNumberFormat);
    }

    public void setDefaultNumberFormat(@Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        withLock(() -> {
            this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;

            sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
        });
    }

    private void checkLineIndexUnsafe(int lineIndex, boolean checkInRange) {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("Line index must be non-negative");
        }

        if (checkInRange && lineIndex >= lines.size()) {
            throw new IllegalArgumentException("Line index must be within the valid range (index >= 0 && index < " + lines.size() + ")");
        }
    }

    private int getScoreByLineUnsafe(int lineIndex) {
        return lines.size() - lineIndex - 1;
    }

    private static @Nullable LinesEntry getLineByScore(List<LinesEntry> lines, int score) {
        if (score < lines.size()) {
            return lines.get(lines.size() - score - 1);
        } else {
            return null;
        }
    }

    private void updateScoreboard(List<LinesEntry> oldLines) {
        if (oldLines.size() != lines.size() && oldLines.size() > lines.size()) {
            for (int i = oldLines.size(); i > lines.size(); i--) {
                sendLineChangeUnsafe(i, UpdateScorePacket.Action.REMOVE_SCORE);

                oldLines.remove(0);
            }
        } else {
            for (int i = oldLines.size(); i < lines.size(); i++) {
                sendLineChangeUnsafe(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
            }
        }

        for (int i = 0; i < lines.size(); ++i) {
            LinesEntry newLine = getLineByScore(lines, i);
            if (newLine == null) continue;
            LinesEntry oldLine = getLineByScore(oldLines, i);
            if (oldLine == null) continue;
            if (newLine.getComponent().equals(oldLine.getComponent())
                    && Objects.equals(newLine.getFormat(), oldLine.getFormat())) continue;
            sendLineChangeUnsafe(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
        }
    }

    private void sendLineChangeUnsafe(int score, UpdateScorePacket.Action action) {
        if (action == UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE) {
            LinesEntry line = getLineByScore(lines, score);
            sendPacket(
                    new UpdateScorePacket(
                            String.valueOf(score),
                            id,
                            score,
                            line != null ?
                                    line.getHolder() :
                                    EMPTY_ENTRY.getHolder(),
                            line != null ?
                                    line.getFormat() :
                                    null
                    )
            );
        } else {
            sendPacket(new ResetScorePacket(
                    String.valueOf(score),
                    id
            ));
        }
    }

    private void sendObjectivePacket(UpdateObjectivesPacket.Mode mode) {
        sendPacket(
                new UpdateObjectivesPacket(
                        id,
                        mode,
                        title,
                        UpdateObjectivesPacket.Type.INTEGER,
                        defaultNumberFormat
                )
        );
    }

}