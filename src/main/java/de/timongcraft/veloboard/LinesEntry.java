package de.timongcraft.veloboard;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import de.timongcraft.velopacketimpl.utils.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class LinesEntry {

    private final ComponentHolder holder;
    private final ComponentUtils.@Nullable NumberFormat format;

    LinesEntry(ComponentHolder holder, @Nullable ComponentUtils.NumberFormat format) {
        this.holder = holder;
        this.format = format;
    }

    public LinesEntry(Component component, @Nullable ComponentUtils.NumberFormat format, Player player) {
        this.holder = new ComponentHolder(player.getProtocolVersion(), component);
        this.format = format;
    }

    ComponentHolder getHolder() {
        return holder;
    }

    public Component getComponent() {
        return holder.getComponent();
    }

    public @Nullable ComponentUtils.NumberFormat getFormat() {
        return format;
    }

    ComponentUtils.@Nullable NumberFormat formatCompiled(ProtocolVersion version) {
        return format != null ? format.compiled(version) : null;
    }

}