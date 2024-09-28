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

	private String accountName;
	private String playerName;
	private final List<String> knownIPs = new ArrayList<>();
	private final List<String> knownAlts = new ArrayList<>();
	private long hardwareID;

	public static PlayerData createDefault(PlayerState playerState) {
		List<String> knownIPs = new ArrayList<>();
		knownIPs.add(playerState.getIp());
		List<String> knownAlts = new ArrayList<>();
		knownAlts.add(playerState.getName());
		PlayerData playerData = new PlayerData(playerState.getStarmadeName(), playerState.getName(), knownIPs, knownAlts);
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	public static PlayerData createDefault(RegisteredClientOnServer client) {
		List<String> knownIPs = new ArrayList<>();
		knownIPs.add(client.getIp());
		List<String> knownAlts = new ArrayList<>();
		knownAlts.add(client.getPlayerName());
		PlayerData playerData = new PlayerData(client.getStarmadeName(), client.getPlayerName(), knownIPs, knownAlts);
		PersistentObjectUtil.addObject(SpaceGuard.getInstance().getSkeleton(), playerData);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		return playerData;
	}

	private PlayerData(String accountName, String playerName, List<String> knownIPs, List<String> knownAlts) {
		this.accountName = accountName;
		this.playerName = playerName;
		this.knownIPs.addAll(knownIPs);
		this.knownAlts.addAll(knownAlts);
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

	public void assignHardwareID(long hardwareID) {
		this.hardwareID = Math.abs(hardwareID);
	}

	public long getHardwareID() {
		return hardwareID;
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
		data.put("hardwareID", hardwareID);
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
		hardwareID = data.getLong("hardwareID");
	}
}