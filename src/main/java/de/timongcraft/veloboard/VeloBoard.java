package de.timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import de.timongcraft.velopacketimpl.network.protocol.packets.*;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import de.timongcraft.velopacketimpl.utils.annotations.Since;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;

@SuppressWarnings("unused")
public class VeloBoard {

    public static final String VELOBOARD_ID = "veloboard";
    protected static final String[] COLOR_CODES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r"};

    private final Player player;
    private final String id;
    private Component title;
    @Since(MINECRAFT_1_20_3)
    private ComponentUtils.NumberFormat numberFormat;
    private final List<Component> lines = new ArrayList<>();

    private boolean deleted = false;

    public VeloBoard(Player player) {
        this(player, Component.empty());
    }

    public VeloBoard(Player player, Component title) {
        this.player = player;
        this.title = title;
        this.id = VELOBOARD_ID;
    }

    @Since(MINECRAFT_1_20_3)
    public VeloBoard(Player player, Component title, ComponentUtils.NumberFormat numberFormat) {
        this.player = player;
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
        for (int i = 0; i < lines.size(); ++i) {
            sendScorePacket(i, UpdateScorePacket.Action.CREATE_OR_UPDATE_SCORE);
            sendTeamPacket(i, UpdateTeamsPacket.Mode.CREATE_TEAM);
            sendLineChange(i);
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

    public List<Component> getLines() {
        return lines;
    }

    public Component getLine(int lineNumber) {
        checkLineNumber(lineNumber, true, false);
        return lines.get(lineNumber);
    }

    /**
     * @see #updateLines(Component...)
     * @see #updateLines(Collection)
     */
    public synchronized void updateLine(int lineNumber, Component lineText) {
        checkLineNumber(lineNumber, false, true);

        if (lineNumber < linesSize()) {
            lines.set(lineNumber, lineText);
            sendLineChange(getScoreByLine(lineNumber));
            return;
        }

        List<Component> newLines = new ArrayList<>(lines);
        if (lineNumber > linesSize())
            for (int i = linesSize(); i < lineNumber; ++i)
                newLines.add(Component.empty());

        newLines.add(lineText);
        updateLines(newLines);
    }

    public synchronized void removeLine(int lineNumber) {
        checkLineNumber(lineNumber, false, false);

        if (lineNumber >= linesSize())
            return;

        List<Component> newLines = new ArrayList<>(lines);
        newLines.remove(lineNumber);
        updateLines(newLines);
    }

    public void updateLines(Component... lines) {
        updateLines(Arrays.asList(lines));
    }

    public synchronized void updateLines(Collection<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        checkLineNumber(lines.size(), false, true);
        List<Component> oldLines = new ArrayList<>(this.lines);
        this.lines.clear();
        this.lines.addAll(lines);
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

        for (int i = 0; i < linesSize; ++i)
            if (!Objects.equals(getLineByScore(oldLines, i), getLineByScore(i)))
                sendLineChange(i);
    }

    private void sendLineChange(int score) {
        Component line = getLineByScore(score);
        sendTeamPacket(score, UpdateTeamsPacket.Mode.UPDATE_TEAM_INFO, line, Component.empty());
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
                        translateComponent(title),
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
                        translateComponent(prefix),
                        translateComponent(suffix),
                        Collections.singletonList(COLOR_CODES[score])
                )
        );
    }

    private void sendPacket(MinecraftPacket packet) {
        if (deleted) {
            throw new IllegalStateException("This VeloBoard is deleted");
        } else {
            if (player.isActive())
                ((ConnectedPlayer) player).getConnection().write(packet);
        }
    }

    public Component translateComponent(Component component) {
        return ((ConnectedPlayer) player).translateMessage(component);
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

    public int linesSize() {
        return lines.size();
    }

    public void clear() {
        for (int i = 0; i < this.lines.size(); ++i)
            this.sendTeamPacket(i, UpdateTeamsPacket.Mode.REMOVE_TEAM);

        this.sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
    }

    public void delete() {
        clear();
        title = null;
        lines.clear();
        numberFormat = null;

        this.deleted = true;
    }

}