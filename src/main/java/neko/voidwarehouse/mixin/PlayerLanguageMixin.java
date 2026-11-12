package neko.voidwarehouse.mixin;
import neko.compat.PlayerLanguage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class PlayerLanguageMixin implements PlayerLanguage {
 @Unique private String neko$language;
 public String neko$getLanguage() { return neko$language; }
 @Inject(method="updateOptions",at=@At("HEAD"))
 private void neko$captureLanguage(ServerboundClientInformationPacket packet,CallbackInfo ci) { neko$language=packet.language(); }
}
