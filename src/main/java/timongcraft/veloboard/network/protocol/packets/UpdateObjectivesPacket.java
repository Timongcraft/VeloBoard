package timongcraft.veloboard.network.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.github._4drian3d.vpacketevents.api.register.PacketRegistration;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import timongcraft.veloboard.network.protocol.ComponentUtils;
import timongcraft.veloboard.utils.annotations.Since;

import static com.velocitypowered.api.network.ProtocolVersion.*;

@SuppressWarnings("unused")
public class UpdateObjectivesPacket implements MinecraftPacket {

    public static void register() {
        PacketRegistration.of(UpdateObjectivesPacket.class)
                .direction(ProtocolUtils.Direction.CLIENTBOUND)
                .packetSupplier(UpdateObjectivesPacket::new)
                .stateRegistry(StateRegistry.PLAY)
                .mapping(0x53, MINECRAFT_1_18_2, false)
                .mapping(0x53, MINECRAFT_1_19, false)
                .mapping(0x56, MINECRAFT_1_19_1, false)
                .mapping(0x54, MINECRAFT_1_19_3, false)
                .mapping(0x58, MINECRAFT_1_19_4, false)
                .mapping(0x58, MINECRAFT_1_20, false)
                .mapping(0x5A, MINECRAFT_1_20_2, false)
                .mapping(0x5C, MINECRAFT_1_20_3, false)
                .register();
    }

    private String objectiveName;
    private Mode mode;
    private Component objectiveValue;
    private Type type;
    @Since(MINECRAFT_1_20_3)
    private boolean hasNumberFormat;
    @Since(MINECRAFT_1_20_3)
    private ComponentUtils.NumberFormat numberFormat;

    public UpdateObjectivesPacket() {
    }

    public UpdateObjectivesPacket(String objectiveName, Mode mode) {
        this(objectiveName, mode, null, null);
    }

    public UpdateObjectivesPacket(String objectiveName, Mode mode, Component objectiveValue, Type type) {
        this.objectiveName = objectiveName;
        this.mode = mode;
        this.objectiveValue = objectiveValue;
        this.type = type;
    }

    @Since(MINECRAFT_1_20_3)
    public UpdateObjectivesPacket(String objectiveName, Mode mode, Component objectiveValue, Type type, ComponentUtils.NumberFormat numberFormat) {
        this.objectiveName = objectiveName;
        this.mode = mode;
        this.objectiveValue = objectiveValue;
        this.type = type;
        if (numberFormat != null) {
            this.hasNumberFormat = true;
            this.numberFormat = numberFormat;
        }
    }

    @Override
    public void decode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        objectiveName = ProtocolUtils.readString(buffer);
        mode = Mode.values()[buffer.readByte()];
        if (mode != Mode.REMOVE_SCOREBOARD) {
            objectiveValue = ComponentHolder.read(buffer, protocolVersion).getComponent();
            type = Type.values()[ProtocolUtils.readVarInt(buffer)];
            if (protocolVersion.compareTo(MINECRAFT_1_20_2) > 0) {
                hasNumberFormat = buffer.readBoolean();
                if (hasNumberFormat) {
                    numberFormat = switch (ProtocolUtils.readVarInt(buffer)) {
                        case 0 -> ComponentUtils.NumberFormatBlank.getInstance();
                        case 2 -> new ComponentUtils.NumberFormatFixed(ComponentHolder.read(buffer, protocolVersion));
                        default ->
                                throw new IllegalStateException("Invalid number format: " + ProtocolUtils.readVarInt(buffer));
                    };
                }
            }
        }
    }

    @Override
    public void encode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(MINECRAFT_1_20) < 0 && objectiveName.length() > 16)
            throw new IllegalStateException("objective name can only be 16 chars long");
        ProtocolUtils.writeString(buffer, objectiveName);
        buffer.writeByte(mode.ordinal());
        if (mode != Mode.REMOVE_SCOREBOARD) {
            new ComponentHolder(protocolVersion, objectiveValue).write(buffer);
            buffer.writeByte(type.ordinal());
            if (protocolVersion.compareTo(MINECRAFT_1_20_2) > 0) {
                buffer.writeBoolean(hasNumberFormat);
                if (hasNumberFormat) {
                    ProtocolUtils.writeVarInt(buffer, numberFormat.getType().ordinal());
                    if (numberFormat instanceof ComponentUtils.NumberFormatFixed numberFormatFixed) {
                        numberFormatFixed.getContent().write(buffer);
                    }
                }
            }
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Component getObjectiveValue() {
        return objectiveValue;
    }

    public void setObjectiveValue(Component objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Since(MINECRAFT_1_20_3)
    public boolean isHasNumberFormat() {
        return hasNumberFormat;
    }

    @Since(MINECRAFT_1_20_3)
    public void setHasNumberFormat(boolean hasNumberFormat) {
        this.hasNumberFormat = hasNumberFormat;
    }

    @Since(MINECRAFT_1_20_3)
    public ComponentUtils.NumberFormat getNumberFormat() {
        return numberFormat;
    }

    @Since(MINECRAFT_1_20_3)
    public void setNumberFormat(ComponentUtils.NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    public enum Mode {
        CREATE_SCOREBOARD, REMOVE_SCOREBOARD, UPDATE_SCOREBOARD
    }

    public enum Type {
        INTEGER, HEARTS
    }

}
