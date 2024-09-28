package thederpgamer.spaceguard.manager;

import api.listener.Listener;
import api.listener.events.network.ClientLoginEvent;
import api.mod.StarLoader;
import thederpgamer.spaceguard.SpaceGuard;

import java.lang.reflect.Field;

public class EventManager {

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
					}
				} catch(Exception exception) {
					instance.logException("An error occurred while checking IP for " + event.getPlayerName(), exception);
				}
			}
		}, instance);
	}
}
