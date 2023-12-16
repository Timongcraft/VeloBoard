package timongcraft.veloboard.network.protocol;

import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import timongcraft.veloboard.utils.annotations.Since;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;

@SuppressWarnings("unused")
@Since(MINECRAFT_1_20_3)
public class ComponentUtils {

    public enum NumberFormatType {
        BLANK, FIXED, STYLED
    }

    public abstract static class NumberFormat {

        private final NumberFormatType type;

        protected NumberFormat(NumberFormatType type) {
            this.type = type;
        }

        public NumberFormatType getType() {
            return type;
        }

    }

    public static class NumberFormatBlank extends NumberFormat {

        private static final NumberFormatBlank INSTANCE = new NumberFormatBlank();

        private NumberFormatBlank() {
            super(NumberFormatType.BLANK);
        }

        public static NumberFormatBlank getInstance() {
            return INSTANCE;
        }

    }

    public static class NumberFormatFixed extends NumberFormat {

        private ComponentHolder content;

        public NumberFormatFixed(ComponentHolder content) {
            super(NumberFormatType.FIXED);
            this.content = content;
        }

        public ComponentHolder getContent() {
            return content;
        }

        public void setContent(ComponentHolder content) {
            this.content = content;
        }

    }

}
