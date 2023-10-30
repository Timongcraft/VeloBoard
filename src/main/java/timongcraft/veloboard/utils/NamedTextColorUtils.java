package timongcraft.veloboard.utils;

import net.kyori.adventure.text.format.NamedTextColor;

public class NamedTextColorUtils {

    public static NamedTextColor getNamedTextColorById(int id) {
        return switch (id) {
            case 0 -> NamedTextColor.BLACK;
            case 1 -> NamedTextColor.DARK_BLUE;
            case 2 -> NamedTextColor.DARK_GREEN;
            case 3 -> NamedTextColor.DARK_AQUA;
            case 4 -> NamedTextColor.DARK_RED;
            case 5 -> NamedTextColor.DARK_PURPLE;
            case 6 -> NamedTextColor.GOLD;
            case 7 -> NamedTextColor.GRAY;
            case 8 -> NamedTextColor.DARK_GRAY;
            case 9 -> NamedTextColor.BLUE;
            case 10 -> NamedTextColor.GREEN;
            case 11 -> NamedTextColor.AQUA;
            case 12 -> NamedTextColor.RED;
            case 13 -> NamedTextColor.LIGHT_PURPLE;
            case 14 -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };
    }

    public static int getIdByNamedTextColor(NamedTextColor color) {
        return switch (color.value()) {
            case 0 -> 0;
            case 170 -> 1;
            case 43520 -> 2;
            case 43690 -> 3;
            case 5592405 -> 8;
            case 5592575 -> 9;
            case 5635925 -> 10;
            case 5636095 -> 11;
            case 11141120 -> 4;
            case 11141290 -> 5;
            case 11184810 -> 7;
            case 16733525 -> 12;
            case 16733695 -> 13;
            case 16755200 -> 6;
            case 16777045 -> 14;
            default -> 15;
        };
    }

}
