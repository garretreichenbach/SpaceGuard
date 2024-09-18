package thederpgamer.spaceguard.utils;

import api.common.GameCommon;
import thederpgamer.spaceguard.SpaceGuard;

public class DataUtils {

	public static String getWorldDataPath() {
		return getResourcesPath() + "/data/" + GameCommon.getUniqueContextId();
	}

	public static String getResourcesPath() {
		return SpaceGuard.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}
}
