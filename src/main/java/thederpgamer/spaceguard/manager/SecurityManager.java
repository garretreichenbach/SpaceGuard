package thederpgamer.spaceguard.manager;

import api.mod.config.PersistentObjectUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.SpaceGuard;
import thederpgamer.spaceguard.data.PlayerData;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class SecurityManager {

	/**
	 * Checks a player's data to see if they are allowed to log in
	 * @param playerData The player's data
	 * @return null if the player can log in, otherwise returns a message explaining why they cannot
	 */
	public static String checkPlayer(PlayerData playerData) {
		String[] knownIPs = playerData.getKnownIPs();
		int restrictions = playerData.getRestrictions();
		if((restrictions & PlayerData.NO_ALTS) == PlayerData.NO_ALTS) {
			//Check for alternate players under the same account
			String accountName = playerData.getAccountName();
			for(PlayerData player : getAllPlayers()) {
				if(player.getAccountName().equals(accountName) && !player.getPlayerName().equals(playerData.getPlayerName())) return "You are not allowed to have alternate accounts";
			}
		}
		if((restrictions & PlayerData.NO_VPN) == PlayerData.NO_VPN) {
			//Check for VPNs
			for(String ip : knownIPs) {
				if(isVPN(ip)) return "You are not allowed to use a VPN or proxy";
			}
		}
		if((restrictions & PlayerData.LAX_IP_RESTRICTION) == PlayerData.LAX_IP_RESTRICTION) {
			//No IP restrictions
			return null;
		}
		if((restrictions & PlayerData.MODERATE_IP_RESTRICTION) == PlayerData.MODERATE_IP_RESTRICTION) {
			//Moderate IP restrictions
			if(knownIPs.length > 3) return "You are only allowed to log in from a maximum of 3 IPs";
		}
		if((restrictions & PlayerData.STRICT_IP_RESTRICTION) == PlayerData.STRICT_IP_RESTRICTION) {
			//Strict IP restrictions
			if(knownIPs.length > 1) return "You are only allowed to log in from one IP";
		}
		return null;
	}

	private static boolean isVPN(String ip) {
		String url = "https://vpnapi.io/api/" + ip + "?key=" + ConfigManager.getMainConfig().getString("vpn_checker_api_key");
		try {
			JSONObject jsonObject = new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8));
			JSONObject security = jsonObject.getJSONObject("security");
			boolean vpn = security.getBoolean("vpn");
//			boolean proxy = security.getBoolean("proxy");
			boolean tor = security.getBoolean("tor");
			return vpn || tor;
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while checking IP for " + ip, exception);
			return false;
		}
	}

	private static List<PlayerData> getAllPlayers() {
		return PersistentObjectUtil.getCopyOfObjects(SpaceGuard.getInstance().getSkeleton(), PlayerData.class);
	}

	public static PlayerData getPlayer(PlayerState player) {
		for(PlayerData playerData : getAllPlayers()) {
			if(playerData.getPlayerName().equals(player.getName())) return playerData;
		}
		return PlayerData.createDefault(player);
	}
}
