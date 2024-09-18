package thederpgamer.spaceguard.manager;

import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.mod.StarLoader;
import thederpgamer.spaceguard.SpaceGuard;

public class EventManager {

	public static void initialize(final SpaceGuard instance) {
		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(PlayerJoinWorldEvent event) {
				try {
					assert event.getPlayerState().isOnServer();
					String reason = SecurityManager.checkPlayer(SecurityManager.getPlayer(event.getPlayerState()));
					if(reason != null) GameServer.getServerState().getController().sendLogout(event.getPlayerState().getClientId(), reason);
				} catch(Exception exception) {
					instance.logException("An error occurred while checking IP for " + event.getPlayerName(), exception);
				}
			}
		}, instance);
	}
}
