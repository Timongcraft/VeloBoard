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
public class DisplayObjectivePacket implements MinecraftPacket {

    public static void register() {
        PacketRegistration.of(DisplayObjectivePacket.class)
                .direction(ProtocolUtils.Direction.CLIENTBOUND)
                .packetSupplier(DisplayObjectivePacket::new)
                .stateRegistry(StateRegistry.PLAY)
                .mapping(0x53, MINECRAFT_1_20_2, false)
                .mapping(0x51, MINECRAFT_1_20, false)
                .mapping(0x51, MINECRAFT_1_19_4, false)
                .mapping(0x4D, MINECRAFT_1_19_3, false)
                .mapping(0x4F, MINECRAFT_1_19_1, false)
                .mapping(0x4C, MINECRAFT_1_19, false)
                .mapping(0x4C, MINECRAFT_1_18_2, false)
                .register();
    }

    private int position;
    private String scoreName;

    public DisplayObjectivePacket() {}

    public DisplayObjectivePacket(int position, String scoreName) {
        if (position < 0 || position > 18)
            throw new IllegalStateException("Position can only be 0-18");
        this.position = position;
        this.scoreName = scoreName;
    }

    @Override
    public void decode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0) {
            position = ProtocolUtils.readVarInt(buffer);
        } else {
            position = buffer.readByte();
        }
        scoreName = ProtocolUtils.readString(buffer);
    }

    @Override
    public void encode(ByteBuf buffer, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(MINECRAFT_1_20_2) >= 0) {
            ProtocolUtils.writeVarInt(buffer, position);
        } else {
            buffer.writeByte(position);
        }

        if (protocolVersion.compareTo(MINECRAFT_1_20) < 0 && scoreName.length() > 16)
            throw new IllegalStateException("score name can only be 16 chars long");
        ProtocolUtils.writeString(buffer, scoreName);
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < 0 || position > 18)
            throw new IllegalStateException("Position can only be 0-18");
        this.position = position;
    }

    public String getScoreName() {
        return scoreName;
    }

    public void setScoreName(String scoreName) {
        this.scoreName = scoreName;
    }

}
