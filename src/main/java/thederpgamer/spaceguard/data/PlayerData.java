package thederpgamer.spaceguard.data;

import api.mod.config.PersistentObjectUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.spaceguard.SpaceGuard;

import java.util.ArrayList;
import java.util.List;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerData implements JsonSerializer {

	public static final int NO_ALTS = 1;
	public static final int NO_VPN = 2;
	public static final int CHECK_HARDWARE_IDS = 4;
	public static final int CHECK_FOR_ALT_IPS_NP = 8;

	private String accountName;
	private String playerName;
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
		PlayerData playerData = new PlayerData(playerState.getStarmadeName(), playerState.getName(), knownIPs, knownAlts, NO_VPN | NO_ALTS | CHECK_HARDWARE_IDS, 0);
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	public static PlayerData createDefault(RegisteredClientOnServer client) {
		List<String> knownIPs = new ArrayList<>();
		knownIPs.add(client.getIp());
		List<String> knownAlts = new ArrayList<>();
		knownAlts.add(client.getPlayerName());
		PlayerData playerData = new PlayerData(client.getStarmadeName(), client.getPlayerName(), knownIPs, knownAlts, NO_VPN | NO_ALTS | CHECK_HARDWARE_IDS, 0);
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
		if(ip.startsWith("/")) ip = ip.substring(1);
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
		this.hardwareID = Math.abs(hardwareID);
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

	@Override
	public String toString() {
		return serialize().toString();
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("accountName", accountName);
		data.put("playerName", playerName);
		data.put("knownIPs", knownIPs);
		data.put("knownAlts", knownAlts);
		data.put("restrictions", restrictions);
		data.put("hardwareID", hardwareID);
		data.put("lastLogin", lastLogin);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		knownIPs.clear();
		knownAlts.clear();
		accountName = data.getString("accountName");
		playerName = data.getString("playerName");
		JSONArray ipArray = data.getJSONArray("knownIPs");
		for(int i = 0; i < ipArray.length(); i ++) knownIPs.add(ipArray.getString(i));
		JSONArray altArray = data.getJSONArray("knownAlts");
		for(int i = 0; i < altArray.length(); i ++) knownAlts.add(altArray.getString(i));
		restrictions = data.getInt("restrictions");
		hardwareID = data.getLong("hardwareID");
		lastLogin = data.getLong("lastLogin");
	}
}