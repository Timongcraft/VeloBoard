package timongcraft.veloboard.network.protocol.packets;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import timongcraft.veloboard.network.PacketRegistration;

import static com.velocitypowered.api.network.ProtocolVersion.*;

@SuppressWarnings("unused")
public class UpdateScorePacket implements MinecraftPacket {

    public static void register() {
        PacketRegistration.of(UpdateScorePacket.class)
                .direction(ProtocolUtils.Direction.CLIENTBOUND)
                .packetSupplier(UpdateScorePacket::new)
                .stateRegistry(StateRegistry.PLAY)
                .mapping(0x5D, MINECRAFT_1_20_2, false)
                .mapping(0x5B, MINECRAFT_1_20, false)
                .mapping(0x5B, MINECRAFT_1_19_4, false)
                .mapping(0x57, MINECRAFT_1_19_3, false)
                .mapping(0x59, MINECRAFT_1_19_1, false)
                .mapping(0x56, MINECRAFT_1_19, false)
                .mapping(0x56, MINECRAFT_1_18_2, false)
                .register();
    }

    private String entityName;
    private Action action;
    private String objectiveName;
    private int value;

    public UpdateScorePacket() {}

    public UpdateScorePacket(String entityName, String objectiveName) {
        this(entityName, Action.REMOVE_SCORE, objectiveName, -1);
    }

    public UpdateScorePacket(String entityName, Action action, String objectiveName, int value) {
        this.entityName = entityName;
        this.action = action;
        this.objectiveName = objectiveName;
        this.value = value;
    }

    @Override
    public void decode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        entityName = ProtocolUtils.readString(buffer);
        action = Action.values()[ProtocolUtils.readVarInt(buffer)];
        objectiveName = ProtocolUtils.readString(buffer);
        if (action == Action.CREATE_OR_UPDATE_SCORE)
            value = ProtocolUtils.readVarInt(buffer);
    }

    @Override
    public void encode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(MINECRAFT_1_20) < 0 && entityName.length() > 40)
            throw new IllegalStateException("entity name can only be 40 chars long");
        ProtocolUtils.writeString(buffer, entityName);
        ProtocolUtils.writeVarInt(buffer, action.ordinal());
        if (protocolVersion.compareTo(MINECRAFT_1_20) < 0 && objectiveName.length() > 16)
            throw new IllegalStateException("objective name can only be 16 chars long");
        ProtocolUtils.writeString(buffer, objectiveName);
        if (action == Action.CREATE_OR_UPDATE_SCORE)
            ProtocolUtils.writeVarInt(buffer, value);
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public enum Action {
        CREATE_OR_UPDATE_SCORE, REMOVE_SCORE
    }

}
