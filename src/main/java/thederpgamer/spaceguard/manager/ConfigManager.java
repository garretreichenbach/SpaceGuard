package thederpgamer.spaceguard.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.spaceguard.SpaceGuard;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"vpn_checker_api_key: <API_KEY>",
			"allow_vpn: false",
			"allow_alt_usernames: false",
			"check_hardware_ids: true"
	};

	public static void initialize(SpaceGuard instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}
