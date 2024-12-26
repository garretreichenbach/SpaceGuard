package thederpgamer.spaceguard.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.spaceguard.SpaceGuard;

public final class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"vpn_checker_api_key: <APY_KEY> # Get your API key from https://vpnapi.io/api-documentation",
			"block_vpn: true # Block VPN connections",
			"block_proxy: true # Block proxy connections",
			"block_tor: true # Block TOR connections",
			"block_alts: true # Block known alt accounts",
			"discord_webhook_url: <WEBHOOK_URL> # Webhook URL to send Discord notifications to a staff only channel. Leave blank to disable.",
			"approved_client_mods: [8366, 8054, 8219, 8324, 8215] # List of approved client mods. These numbers are mod ids and are the 4 digits at the end of the URL for a mod page on the StarMade Dock. Leave blank to disable.",
	};

	public static void initialize(SpaceGuard instance) {
		mainConfig = instance.getConfig("config");
		saveDefaultConfig(defaultMainConfig);
	}

	private static void saveDefaultConfig(String[] config) {
		for(String line : config) {
			String key = line.substring(0, line.indexOf(':')).trim();
			String value = line.substring(line.indexOf(':') + 1, line.indexOf('#')).trim();
			String comment = line.substring(line.indexOf('#') + 1).trim();
			if(!mainConfig.getKeys().contains(key)) {
				mainConfig.set(key, value);
				mainConfig.setComment(key, comment);
			}
		}
		mainConfig.saveConfig();
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}
