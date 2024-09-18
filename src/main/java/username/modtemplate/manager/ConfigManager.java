package username.modtemplate.manager;

import api.mod.config.FileConfiguration;
import username.modtemplate.ModTemplate;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false"
	};

	public static void initialize(ModTemplate instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}
