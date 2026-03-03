package videogoose.spaceguard.mixin;

import org.schema.game.server.controller.GameServerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.spaceguard.manager.NoticeManager;

@Mixin(value = GameServerController.class, remap = false)
public class GameServerControllerMixin {

	@Inject(method = "addBannedIp", at = @At("TAIL"))
	public void onAddBannedIp(String from, String playerIp, long validUntil, CallbackInfo ci) {
		NoticeManager.playerBanned(playerIp, from);
	}

	@Inject(method = "removeBannedIp", at = @At("TAIL"))
	public void onRemoveBannedIp(String from, String playerIp, CallbackInfoReturnable<Boolean> cir) {
		NoticeManager.playerUnbanned(playerIp, from);
	}

	@Inject(method = "addBannedName", at = @At("TAIL"))
	public void onAddBannedName(String from, String playerName, long validUntil, CallbackInfo ci) {
		NoticeManager.playerBanned(playerName, from);
	}

	@Inject(method = "removeBannedName", at = @At("TAIL"))
	public void onRemoveBannedName(String from, String playerName, CallbackInfoReturnable<Boolean> cir) {
		NoticeManager.playerUnbanned(playerName, from);
	}

	@Inject(method = "addBannedAccount", at = @At("TAIL"))
	public void onAddBannedAccount(String from, String hardwareId, long validUntil, CallbackInfo ci) {
		NoticeManager.playerBanned(hardwareId, from);
	}

	@Inject(method = "removeBannedAccount", at = @At("TAIL"))
	public void onRemoveBannedAccount(String from, String hardwareId, CallbackInfoReturnable<Boolean> cir) {
		NoticeManager.playerUnbanned(hardwareId, from);
	}
}
