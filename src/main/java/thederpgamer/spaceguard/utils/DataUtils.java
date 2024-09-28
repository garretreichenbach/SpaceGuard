package thederpgamer.spaceguard.utils;

import api.common.GameCommon;
import thederpgamer.spaceguard.SpaceGuard;

import java.io.File;

public class DataUtils {

	public static String getWorldDataPath() {
		String path = getResourcesPath() + "/data/" + GameCommon.getUniqueContextId();
		File file = new File(path);
		if(!file.exists()) file.mkdirs();
		return path;
	}

	public static String getResourcesPath() {
		return SpaceGuard.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}
}
