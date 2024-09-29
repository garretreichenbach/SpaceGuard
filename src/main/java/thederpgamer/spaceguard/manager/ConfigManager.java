package thederpgamer.spaceguard.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.spaceguard.SpaceGuard;

public final class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"vpn_checker_api_key: <APY_KEY>",
			"block_vpn: true",
			"block_proxy: true",
			"block_tor: true",
			"block_alts: true",
			"discord_webhook_url: <WEBHOOK_URL>"
	};

	public static void initialize(SpaceGuard instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}
