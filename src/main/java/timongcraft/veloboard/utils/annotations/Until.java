package timongcraft.veloboard.utils.annotations;

import com.velocitypowered.api.network.ProtocolVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks code as valid only until a specified version of Minecraft.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Until {
    ProtocolVersion value();
}
