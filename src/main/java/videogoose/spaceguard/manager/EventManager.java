package videogoose.spaceguard.manager;

import api.listener.Listener;
import api.listener.events.network.ClientLoginEvent;
import api.mod.StarLoader;
import videogoose.spaceguard.SpaceGuard;
import videogoose.spaceguard.data.PlayerData;
import videogoose.spaceguard.manager.SecurityManager.BlockReason;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventManager {

	private static final ScheduledExecutorService SCHEDULER =
			Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "SpaceGuard-EventScheduler");
				t.setDaemon(true);
				return t;
			});

	public static void initialize(SpaceGuard instance) {
		StarLoader.registerListener(ClientLoginEvent.class, new Listener<ClientLoginEvent>() {
			@Override
			public void onEvent(ClientLoginEvent event) {
				PlayerData playerData = SecurityManager.getPlayer(event.getRegisteredClientOnServer());
				BlockReason reason = SecurityManager.checkPlayer(playerData);

				// null means allowed; any other value is a block reason
				if(reason != null) {
					String msg = reason.getMessage();
					String ip  = playerData.getKnownIPs().isEmpty()
							? "unknown"
							: playerData.getKnownIPs().iterator().next();

					// Emit a notice for server bans (VPN/proxy/alt already emit their own notices in checkPlayer)
					if(reason == BlockReason.BANNED) {
						NoticeManager.loginBlocked(event.getPlayerName(), ip, "Account is banned");
					}

					// Delay kick by 2 s so the client finishes its handshake cleanly
					SCHEDULER.schedule(() -> {
						try {
							SecurityManager.kickPlayer(event.getPlayerName(), msg);
						} catch(Exception e) {
							SpaceGuard.getInstance().logException("Failed to kick player " + event.getPlayerName(), e);
						}
					}, 2, TimeUnit.SECONDS);
				}
			}
		}, instance);
	}
}