package username.modtemplate.utils;

import api.common.GameCommon;
import username.modtemplate.ModTemplate;

public class DataUtils {

	public static String getWorldDataPath() {
		return getResourcesPath() + "/data/" + GameCommon.getUniqueContextId();
	}

	public static String getResourcesPath() {
		return ModTemplate.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}
}
