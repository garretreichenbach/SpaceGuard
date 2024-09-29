package thederpgamer.spaceguard.manager;

import api.listener.Listener;
import api.listener.events.network.ClientLoginEvent;
import api.mod.StarLoader;
import org.schema.schine.network.commands.Login;
import thederpgamer.spaceguard.SpaceGuard;

import java.lang.reflect.Field;

public final class EventManager {

	public static void initialize(final SpaceGuard instance) {
		StarLoader.registerListener(ClientLoginEvent.class, new Listener<ClientLoginEvent>() {
			@Override
			public void onEvent(ClientLoginEvent event) {
				try {
					int reason = SecurityManager.checkPlayer(SecurityManager.getPlayer(event.getRegisteredClientOnServer()));
					if(reason != -1) {
						instance.logWarning("Player " + event.getPlayerName() + " was denied access to the server for the following reason:\n" + reason);
						Field loginCodeField = event.getLoginRequest().getClass().getDeclaredField("returnCode");
						loginCodeField.setAccessible(true);
						loginCodeField.set(event.getLoginRequest(), reason);
						switch(Login.LoginCode.getById(reason)) {
							case ERROR_YOU_ARE_BANNED:
								SecurityManager.kickPlayer(event.getPlayerName(), "You are banned from this server.");
								break;
							case ERROR_VPN:
								SecurityManager.kickPlayer(event.getPlayerName(), "VPN usage is not allowed on this server.");
								break;
							case ERROR_PROXY:
								SecurityManager.kickPlayer(event.getPlayerName(), "Proxy usage is not allowed on this server.");
								break;
							case ERROR_TOR:
								SecurityManager.kickPlayer(event.getPlayerName(), "TOR usage is not allowed on this server.");
								break;
							case ERROR_NO_ALTS:
								SecurityManager.kickPlayer(event.getPlayerName(), "Alt accounts are not allowed on this server.");
								break;
							default:
								SecurityManager.kickPlayer(event.getPlayerName(), "You were denied access to the server for an unknown reason.");
								break;
						}
					}

				} catch(Exception exception) {
					instance.logException("An error occurred while checking IP for " + event.getPlayerName(), exception);
				}
			}
		}, instance);
	}
}
