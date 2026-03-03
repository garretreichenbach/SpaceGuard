package videogoose.spaceguard.mixin;

import org.schema.schine.network.commands.Login;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.spaceguard.utils.ReflectionUtils;

@Mixin(value = Login.LoginCode.class, remap = false)
public class LoginCodeMixin {

	/**
	 * Injects custom enums into the LoginCode during class initialization.
	 * Targets the static initializer to ensure enum constants are added before the class is fully loaded.
	 */
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void onStaticInit(CallbackInfo ci) {
		try {
			Login.LoginCode[] currentValues = Login.LoginCode.values();
			int ordinal = currentValues.length; // Start ordinal after existing values
			ReflectionUtils.injectEnumValue(Login.LoginCode.class, "ERROR_VPN", ordinal++, -12, "[SpaceGuard] VPN detected. Please disable your VPN to connect to this server.");
			ReflectionUtils.injectEnumValue(Login.LoginCode.class, "ERROR_ALT", ordinal++, -13, "[SpaceGuard] No alternative accounts allowed. Please use your main account to connect to this server.");
			ReflectionUtils.injectEnumValue(Login.LoginCode.class, "ERROR_PROXY", ordinal++, -14, "[SpaceGuard] Proxy detected. Please disable your proxy to connect to this server.");
		} catch(Exception exception) {
			System.err.println("[SpaceGuard] [Mixin] FAILED to inject custom LoginCode enums: " + exception.getMessage());
			System.err.println("[SpaceGuard] [Mixin] Stack trace:");
			exception.printStackTrace(System.err);
		}
	}
}
