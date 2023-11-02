package timongcraft.veloboard;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import timongcraft.veloboard.network.protocol.packets.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@SuppressWarnings("unused")
public class VeloBoard {

    protected static final String[] COLOR_CODES = {
            "§0", // BLACK
            "§1", // DARK_BLUE
            "§2", // DARK_GREEN
            "§3", // DARK_AQUA
            "§4", // DARK_RED
            "§5", // DARK_PURPLE
            "§6", // GOLD
            "§7", // GRAY
            "§8", // DARK_GRAY
            "§9", // BLUE
            "§a", // GREEN
            "§b", // AQUA
            "§c", // RED
            "§d", // LIGHT_PURPLE
            "§e", // YELLOW
            "§f", // WHITE
            "§k", // OBFUSCATED
            "§l", // BOLD
            "§m", // STRIKETHROUGH
            "§n", // UNDERLINE
            "§o", // ITALIC
            "§r"  // RESET
    };

    private final Player player;
    private final String id;
    private Component title;
    private final List<Component> lines = new ArrayList<>();

    private boolean deleted = false;

    public VeloBoard(Player player) {
        this(player, Component.empty());
    }

    public VeloBoard(Player player, Component title) {
        this.player = player;
        this.title = title;
        id = "sbsvb-" + Long.toHexString(LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset()));
    }

    public void initialize() {
        sendObjectivePacket(UpdateObjectivesPacket.Mode.CREATE_SCOREBOARD);
        sendPacket(new DisplayObjectivePacket(1, id));
    }

    public void resend() {
        delete();
        deleted = false;

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

    public List<Component> getLines() {
        return lines;
    }

    public Component getLine(int lineNumber) {
        checkLineNumber(lineNumber, true, false);
        return lines.get(lineNumber);
    }

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
            List<Component> oldLinesCopy = new ArrayList<>(oldLines);

            if (oldLines.size() > linesSize) {
                for (int i = oldLinesCopy.size(); i > linesSize; i--) {
                    sendTeamPacket(i - 1, UpdateTeamsPacket.Mode.REMOVE_TEAM);
                    sendScorePacket(i - 1, UpdateScorePacket.Action.REMOVE_SCORE);

                    oldLines.remove(0);
                }
            } else {
                for (int i = oldLinesCopy.size(); i < linesSize; i++) {
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
                        title,
                        UpdateObjectivesPacket.Type.INTEGER
                )
        );
    }

    private void sendScorePacket(int score, UpdateScorePacket.Action action) {
        sendPacket(
                new UpdateScorePacket(
                        COLOR_CODES[score],
                        action,
                        id,
                        score
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
                        new ArrayList<>(),
                        UpdateTeamsPacket.NameTagVisibility.ALWAYS,
                        UpdateTeamsPacket.CollisionRule.ALWAYS,
                        NamedTextColor.WHITE,
                        prefix,
                        suffix,
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

    public void delete() {
        int i = 0;

        while (true) {
            if (i >= lines.size()) {
                sendObjectivePacket(UpdateObjectivesPacket.Mode.REMOVE_SCOREBOARD);
                break;
            }

            sendTeamPacket(i, UpdateTeamsPacket.Mode.REMOVE_TEAM);
            ++i;
        }

        deleted = true;
    }

}
