package thederpgamer.spaceguard.data;

import api.mod.config.PersistentObjectUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.spaceguard.SpaceGuard;

import java.util.HashSet;
import java.util.Set;

/**
 * Storage class for player data.
 *
 * @author TheDerpGamer
 */
public final class PlayerData implements JsonSerializer {

	private static final byte VERSION = 2;
	private String accountName;
	private String playerName;
	private final Set<String> knownIPs = new HashSet<>();
	private final Set<String> knownAlts = new HashSet<>();
	private final Set<Long> hardwareIDs = new HashSet<>();

	public static PlayerData createDefault(PlayerState playerState) {
		PlayerData playerData = new PlayerData(playerState.getStarmadeName(), playerState.getName(), playerState.getIp());
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	public static PlayerData createDefault(RegisteredClientOnServer client) {
		PlayerData playerData = new PlayerData(client.getStarmadeName(), client.getPlayerName(), client.getIp());
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	private PlayerData(String accountName, String playerName, String ip) {
		this.accountName = accountName;
		this.playerName = playerName;
		addIP(ip);
	}

	public String getAccountName() {
		return accountName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public Set<String> getKnownIPs() {
		return knownIPs;
	}

	public void addIP(String ip) {
		if(ip.startsWith("/")) ip = ip.substring(1);
		knownIPs.add(ip);
	}

	public Set<String> getKnownAlts() {
		return knownAlts;
	}

	public void addAlt(String playerName) {
		knownAlts.add(playerName);
	}

	public void addHardwareID(long hardwareID) {
		hardwareIDs.add(Math.abs(hardwareID));
	}

	public Set<Long> getHardwareIDs() {
		return hardwareIDs;
	}

	@Override
	public String toString() {
		return serialize().toString();
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("accountName", accountName);
		data.put("playerName", playerName);
		data.put("knownIPs", knownIPs);
		data.put("knownAlts", knownAlts);
		data.put("hardwareIDs", hardwareIDs);
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
		if(data.has("hardwareID") && !data.has("version")) { //Version 1
			hardwareIDs.add(data.getLong("hardwareID"));
		} else if(data.has("version")) { //Version >= 2
			byte version = (byte) data.getInt("version");
			if(version == 2) {
				JSONArray hardwareArray = data.getJSONArray("hardwareIDs");
				for(int i = 0; i < hardwareArray.length(); i ++) hardwareIDs.add(hardwareArray.getLong(i));
			}
		}
	}
}