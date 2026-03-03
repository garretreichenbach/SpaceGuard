package videogoose.spaceguard.mixin;

import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.admin.AdminCommandQueueElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.spaceguard.manager.NoticeManager;

@Mixin(value = AdminCommandQueueElement.class, remap = false)
public class AdminCommandQueueElementMixin {

	@Inject(method = "kick(Lorg/schema/game/server/data/GameServerState;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"))
	public void onKick(GameServerState state, String reason, String player, CallbackInfo ci) {
		NoticeManager.playerKicked(player, reason);
	}
}
