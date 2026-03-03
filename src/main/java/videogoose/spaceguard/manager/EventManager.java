package videogoose.spaceguard.manager;

import api.listener.Listener;
import api.listener.events.network.ClientLoginEvent;
import api.mod.StarLoader;
import org.schema.schine.network.commands.Login;
import videogoose.spaceguard.SpaceGuard;

public class EventManager {

	public static void initialize(SpaceGuard instance) {
		StarLoader.registerListener(ClientLoginEvent.class, new Listener<ClientLoginEvent>() {
			@Override
			public void onEvent(ClientLoginEvent event) {
				int reason = SecurityManager.checkPlayer(SecurityManager.getPlayer(event.getRegisteredClientOnServer()));
				if(reason < 0) {
					Login.LoginCode code = Login.LoginCode.getById(reason);
					(new Thread(() -> {
						try {
							Thread.sleep(2000);
							SecurityManager.kickPlayer(event.getPlayerName(), code.msg);
						} catch(InterruptedException exception) {
							exception.printStackTrace();
							SecurityManager.kickPlayer(event.getPlayerName(), code.msg);
						}
					})).start();
				}
			}
		}, instance);
	}
}