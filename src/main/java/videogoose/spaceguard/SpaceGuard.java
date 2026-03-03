package videogoose.spaceguard;

import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import videogoose.spaceguard.data.DiscordWebhook;
import videogoose.spaceguard.data.commands.GetPlayerDataCommand;
import videogoose.spaceguard.data.commands.GlobalBanCommand;
import videogoose.spaceguard.data.commands.TrustPlayerCommand;
import videogoose.spaceguard.manager.ConfigManager;
import videogoose.spaceguard.manager.EventManager;
import videogoose.spaceguard.manager.PacketManager;
import videogoose.spaceguard.manager.SecurityManager;
import videogoose.spaceguard.networking.client.SendClientInfoToServer;

public final class SpaceGuard extends StarMod {

	private static SpaceGuard instance;

	public SpaceGuard() {
		instance = this;
	}

	public static SpaceGuard getInstance() {
		return instance;
	}

	public static void logDiscordMessage(String message) {
		if("<WEBHOOK_URL>".equals(ConfigManager.getMainConfig().getString("discord_webhook_url"))) {
			instance.logWarning("Discord webhook URL not set. Please set the WEBHOOK_URL in the config.");
		} else {
			try {
				DiscordWebhook webhook = new DiscordWebhook(ConfigManager.getMainConfig().getString("discord_webhook_url"));
				webhook.setContent(message);
				webhook.execute();
			} catch(Exception exception) {
				instance.logException("An error occurred while sending message to Discord", exception);
			}
		}
	}

	@Override
	public void onEnable() {
		super.onEnable();
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		PacketManager.initialize();
		registerCommands();
		registerPackets();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		SecurityManager.initializeClient();
	}

	private void registerCommands() {
		StarLoader.registerCommand(new GlobalBanCommand());
		StarLoader.registerCommand(new GetPlayerDataCommand());
		StarLoader.registerCommand(new TrustPlayerCommand());
	}

	private void registerPackets() {
		PacketUtil.registerPacket(SendClientInfoToServer.class);
	}
}
