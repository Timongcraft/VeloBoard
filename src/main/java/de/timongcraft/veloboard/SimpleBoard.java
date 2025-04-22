package de.timongcraft.veloboard;

import com.google.common.annotations.Beta;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import de.timongcraft.velopacketimpl.network.protocol.packets.DisplayObjectivePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.ResetScorePacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateObjectivesPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.UpdateScorePacket;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import de.timongcraft.velopacketimpl.utils.annotations.Since;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p><b>Note</b>: With this board, you are no longer limited* in line size and avoid the overhead of teams,
 * as the normal {@link de.timongcraft.veloboard.VeloBoard} requires a team to be created for each line, which is no longer necessary due to new display-name introduced in 1.20.3.
 */
@SuppressWarnings("unused")
@Since(ProtocolVersion.MINECRAFT_1_20_3)
@Beta
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
        setTitleSilent(title);
        this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;
        EMPTY_ENTRY = new LinesEntry(new ComponentHolder(player.getProtocolVersion(), Component.empty()), null);
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
                sendLineChange(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
            }
        } finally {
            linesLock.unlock();
        }
    }

    public Component getTitle() {
        return title.getComponent();
    }

    public void setTitle(Component title) {
        setTitleSilent(title);

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
    }

    private void setTitleSilent(Component title) {
        this.title = new ComponentHolder(player.getProtocolVersion(), this.player.translateMessage(title));
    }

    public @Nullable ComponentUtils.NumberFormat getDefaultNumberFormat() {
        return defaultNumberFormat;
    }

    public void setDefaultNumberFormat(@Nullable ComponentUtils.NumberFormat defaultNumberFormat) {
        this.defaultNumberFormat = defaultNumberFormat != null ? defaultNumberFormat.compiled(player.getProtocolVersion()) : null;

        sendObjectivePacket(UpdateObjectivesPacket.Mode.UPDATE_SCOREBOARD);
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
    public List<LinesEntry> getLines() {
        return Collections.unmodifiableList(lines);
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
    public List<Component> getLineComponents() {
        return lines.stream().map(LinesEntry::getComponent).toList();
    }

    public @Nullable LinesEntry getLine(int lineIndex) {
        checkLineIndex(lineIndex, true);
        return lines.get(lineIndex) != null ? lines.get(lineIndex) : null;
    }

    public @Nullable Component getLineComponent(int lineIndex) {
        checkLineIndex(lineIndex, true);
        return lines.get(lineIndex) != null ? lines.get(lineIndex).getComponent() : null;
    }

    /**
     * @see #setLineComponents(Component...)
     * @see #setLines(Collection)
     */
    public void setLineComponent(int lineIndex, Component lineComponent) {
        setLine(lineIndex, new LinesEntry(lineComponent, null, player));
    }

    public void setLine(int lineIndex, LinesEntry line) {
        checkLineIndex(lineIndex, false);
        LinesEntry linesEntry = new LinesEntry(new ComponentHolder(player.getProtocolVersion(), player.translateMessage(line.getComponent())), line.formatCompiled(player.getProtocolVersion()));

        List<LinesEntry> newLines;
        linesLock.lock();
        try {
            if (lineIndex < linesSize()) {
                lines.set(lineIndex, linesEntry);
                sendLineChange(getScoreByLine(lineIndex), UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
                return;
            }
        } finally {
            linesLock.unlock();
        }

        newLines = new ArrayList<>(lines);
        if (lineIndex > linesSize()) {
            for (int i = linesSize(); i < lineIndex; ++i) {
                newLines.add(EMPTY_ENTRY);
            }
        }

        newLines.add(linesEntry);
        setLines(newLines);
    }

    public void removeLine(int lineIndex) {
        checkLineIndex(lineIndex, false);
        if (lineIndex >= linesSize()) return;
        List<LinesEntry> newLines = new ArrayList<>(lines);

        newLines.remove(lineIndex);
        setLines(newLines);
    }

    public void setLineComponents(Component... lineComponents) {
        setLineComponents(Arrays.asList(lineComponents));
    }

    public void setLineComponents(Collection<Component> lineComponents) {
        Objects.requireNonNull(lineComponents, "lines");
        checkLineIndex(linesSize(), false);
        List<LinesEntry> oldLines = new ArrayList<>(this.lines);

        linesLock.lock(); // lock while updating
        try {
            setLinesComponentsSilent(lineComponents);

            updateScoreboard(oldLines);
        } finally {
            linesLock.unlock();
        }
    }

    public void setLines(LinesEntry... lines) {
        setLines(Arrays.asList(lines));
    }

    private void setLines(Collection<LinesEntry> lines) {
        Objects.requireNonNull(lines, "lines");
        checkLineIndex(linesSize(), false);
        List<LinesEntry> oldLines = new ArrayList<>(this.lines);

        linesLock.lock(); // lock while updating
        try {
            setLinesSilent(lines);

            updateScoreboard(oldLines);
        } finally {
            linesLock.unlock();
        }
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void setLinesComponentsSilent(Collection<Component> lines) {
        setLinesSilent(lines.stream().map(c -> new LinesEntry(
                new ComponentHolder(player.getProtocolVersion(), player.translateMessage(c))
                , null)).toList());
    }

    /**
     * Useful for updating lines before a {@link #resend()}.
     */
    public void setLinesSilent(Collection<LinesEntry> lines) {
        linesLock.lock();
        try {
            this.lines.clear();
            this.lines.addAll(lines);
        } finally {
            linesLock.unlock();
        }
    }

    private void updateScoreboard(List<LinesEntry> oldLines) {
        int linesSize = this.linesSize();
        if (oldLines.size() != linesSize && oldLines.size() > linesSize) {
            for (int i = oldLines.size(); i > linesSize; i--) {
                sendLineChange(i, UpdateScorePacket.Action.REMOVE_SCORE);

                oldLines.remove(0);
            }
        } else {
            for (int i = oldLines.size(); i < linesSize; i++) {
                sendLineChange(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
            }
        }

        for (int i = 0; i < linesSize; ++i) {
            LinesEntry newLine = getLineByScore(lines, i);
            if (newLine == null) continue;
            LinesEntry oldLine = getLineByScore(oldLines, i);
            if (oldLine == null) continue;
            if (newLine.getComponent().equals(oldLine.getComponent())
                    && Objects.equals(newLine.getFormat(), oldLine.getFormat())) continue;
            sendLineChange(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
        }
    }

    private void checkLineIndex(int lineIndex, boolean checkInRange) {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("Line index must be non-negative");
        }

        if (checkInRange && lineIndex >= linesSize()) {
            throw new IllegalArgumentException("Line index must be within the valid range (index >= 0 && index < " + linesSize() + ")");
        }
    }

    private int getScoreByLine(int lineIndex) {
        return linesSize() - lineIndex - 1;
    }

    private @Nullable LinesEntry getLineByScore(List<LinesEntry> lines, int score) {
        if (score < linesSize()) {
            return lines.get(linesSize() - score - 1);
        } else {
            return null;
        }
    }

    private void sendLineChange(int score, UpdateScorePacket.Action action) {
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

    public int linesSize() {
        return lines.size();
    }

    @Override
    public void clear() {
        this.sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
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

}