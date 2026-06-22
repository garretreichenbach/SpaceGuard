package videogoose.spaceguard.manager;

import api.mod.ModSkeleton;
import api.mod.StarLoader;
import api.mod.config.FileConfiguration;
import videogoose.spaceguard.SpaceGuard;

import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"vpn_checker_api_key: <API_KEY> # Get your API key from https://vpnapi.io/api-documentation",
			"block_vpn: false # Block VPN connections",
			"block_proxy: false # Block proxy connections",
			"block_alts: true # Block known alt accounts",
			"discord_webhook_url: <WEBHOOK_URL> # Webhook URL to send Discord notifications to a staff only channel. Leave blank to disable.",
			"approved_client_mods: # List of approved client mods. These numbers are mod ids and are the 4 digits at the end of the URL for a mod page on the StarMade Dock. Leave blank to disable.",
			"collect_hardware_fingerprint: false # Opt-in: collect and send a one-way hardware fingerprint to help detect alts.",
			"security_alerts_enabled: false # Send security event notifications to a Discord channel via webhook.",
			"security_alert_webhook_url: <WEBHOOK_URL> # Separate webhook URL for security alerts (e.g. a private staff channel). Leave blank to disable.",
	};

	public static void initialize(SpaceGuard instance) {
		mainConfig = instance.getConfig("config");
		saveDefaultConfig(defaultMainConfig);
		populateServerMods();
	}

	/**
	 * Adds any server-installed mod IDs that are not already in the approved_client_mods config
	 * list, then saves the config. This ensures mods running on the server are always permitted
	 * for connecting clients without requiring manual config edits.
	 */
	public static void populateServerMods() {
		try {
			List<String> approved = mainConfig.getList("approved_client_mods");
			// Work on a mutable copy; getList may return an unmodifiable view
			List<String> updated = (approved != null) ? new ArrayList<>(approved) : new ArrayList<>();
			boolean changed = false;
			for(ModSkeleton mod : StarLoader.starMods) {
				String id = String.valueOf(mod.getSmdResourceId());
				if(!updated.contains(id)) {
					updated.add(id);
					SpaceGuard.getInstance().logInfo("Auto-approved server mod ID " + id + " (" + mod.getName() + ") in approved_client_mods.");
					changed = true;
				}
			}
			if(changed) {
				mainConfig.set("approved_client_mods", updated);
				mainConfig.saveConfig();
			}
		} catch(Exception e) {
			SpaceGuard.getInstance().logException("Failed to populate server mods into approved_client_mods", e);
		}
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


