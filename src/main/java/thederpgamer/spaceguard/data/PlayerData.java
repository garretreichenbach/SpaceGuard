package thederpgamer.spaceguard.data;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.SpaceGuard;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerData {

	public static final int NO_ALTS = 1; //The player cannot have alternate accounts
	public static final int NO_VPN = 2; //The player cannot use a VPN or proxy, but can log in from multiple IPs as long as they are not VPNs
	public static final int LAX_IP_RESTRICTION = 4; //The player can log in from multiple IPs
	public static final int MODERATE_IP_RESTRICTION = 8; //The player can log in from a maximum of 3 IPs
	public static final int STRICT_IP_RESTRICTION = 16; //The player can only log in from one IP
	public static final int CHECK_HARDWARE_IDS = 32; //The player's hardware IDs are checked

	private final String accountName;
	private final String playerName; //The player's name
	private final String[] knownIPs; //The player's known IPs
	private final int restrictions; //The player's restrictions

	public static PlayerData createDefault(PlayerState playerState) {
		PlayerData playerData = new PlayerData(playerState.getStarmadeName(), playerState.getName(), new String[] { playerState.getIp() }, NO_VPN | LAX_IP_RESTRICTION | CHECK_HARDWARE_IDS);
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	private PlayerData(String accountName, String playerName, String[] knownIPs, int restrictions) {
		this.accountName = accountName;
		this.playerName = playerName;
		this.knownIPs = knownIPs;
		this.restrictions = restrictions;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String[] getKnownIPs() {
		return knownIPs;
	}

	public int getRestrictions() {
		return restrictions;
	}
}