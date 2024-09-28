package thederpgamer.spaceguard.data;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.SpaceGuard;

import java.util.ArrayList;
import java.util.List;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerData {

	public static final int NO_ALTS = 1;
	public static final int NO_VPN = 2;
	public static final int CHECK_HARDWARE_IDS = 4;
	public static final int CHECK_FOR_ALT_IPS_NP = 8;

	private final String accountName;
	private final String playerName;
	private final List<String> knownIPs = new ArrayList<>();
	private final List<String> knownAlts = new ArrayList<>();
	private int restrictions;
	private long hardwareID;
	private long lastLogin = -1;

	public static PlayerData createDefault(PlayerState playerState) {
		List<String> knownIPs = new ArrayList<>();
		knownIPs.add(playerState.getIp());
		List<String> knownAlts = new ArrayList<>();
		knownAlts.add(playerState.getName());
		PlayerData playerData = new PlayerData(playerState.getStarmadeName(), playerState.getName(), knownIPs, knownAlts, NO_VPN | CHECK_HARDWARE_IDS, 0);
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	private PlayerData(String accountName, String playerName, List<String> knownIPs, List<String> knownAlts, int restrictions, long lastLogin) {
		this.accountName = accountName;
		this.playerName = playerName;
		this.knownIPs.addAll(knownIPs);
		this.knownAlts.addAll(knownAlts);
		this.restrictions = restrictions;
		this.lastLogin = lastLogin;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public List<String> getKnownIPs() {
		return knownIPs;
	}

	public void addIP(String ip) {
		if(!knownIPs.contains(ip)) knownIPs.add(ip);
	}

	public List<String> getKnownAlts() {
		return knownAlts;
	}

	public void addAlt(String playerName) {
		if(!knownAlts.contains(playerName)) knownAlts.add(playerName);
	}

	public int getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(int restrictions) {
		this.restrictions = restrictions;
	}

	public void assignHardwareID(long hardwareID) {
		this.hardwareID = hardwareID;
	}

	public long getHardwareID() {
		return hardwareID;
	}

	public long getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}
}