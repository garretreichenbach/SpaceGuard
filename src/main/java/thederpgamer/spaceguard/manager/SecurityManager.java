package thederpgamer.spaceguard.manager;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import thederpgamer.spaceguard.SpaceGuard;
import thederpgamer.spaceguard.data.PlayerData;
import thederpgamer.spaceguard.networking.client.SendHardwareInfoToServerPacket;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class SecurityManager {

	/**
	 * Checks a player's data to see if they are allowed to log in
	 *
	 * @param playerState The player's state
	 * @param playerData The player's data
	 * @return null if the player can log in, otherwise returns a message explaining why they cannot
	 */
	public static String checkPlayer(PlayerState playerState, PlayerData playerData) {
		if(checkIfPlayerIsBanned(playerState, playerData)) return "You are banned from this server!";

		List<String> knownIPs = playerData.getKnownIPs();
		List<String> knownAlts = playerData.getKnownAlts();

		int restrictions = playerData.getRestrictions();
		if((restrictions & PlayerData.NO_VPN) == PlayerData.NO_VPN) {
			//Check for VPNs
			for(String ip : knownIPs) {
				if(isVPN(ip)) return "You are not allowed to use a VPN or proxy on this server!";
			}
		}
		if((restrictions & PlayerData.CHECK_HARDWARE_IDS) == PlayerData.CHECK_HARDWARE_IDS) {
			//Check hardware IDs
			long hardwareID = playerData.getHardwareID();
			if(hardwareID != 0) {
				List<PlayerData> matchingPlayers = getPlayersWithMatchingHIDs(hardwareID);
				for(PlayerData player : matchingPlayers) {
					player.addAlt(playerData.getPlayerName());
					playerData.addAlt(player.getPlayerName());
					PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
				}
				for(String playerName : playerData.getKnownAlts()) { //Admins can have alt accounts
					if(GameServer.getServerState().isAdmin(playerName)) return null;
				}
				return "You are not allowed to have alternate accounts on this server! Accounts: " + playerData.getKnownAlts().toString();
			}
		}
		if((restrictions & PlayerData.NO_ALTS) == PlayerData.NO_ALTS) {
			//Check for alternate players under the same account
			String accountName = playerData.getAccountName();
			for(PlayerData player : getAllPlayers()) {
				if(player.getAccountName().equals(accountName) && !player.getPlayerName().equals(playerData.getPlayerName())) {
					player.addAlt(playerData.getPlayerName());
					playerData.addAlt(player.getPlayerName());
					PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
					return "You are not allowed to have alternate accounts on this server! Accounts: " + player.getKnownAlts().toString();
				}
			}
		}

		if(((restrictions & PlayerData.CHECK_FOR_ALT_IPS_NP) == PlayerData.CHECK_FOR_ALT_IPS_NP) && !GameServer.getServerState().isAdmin(playerData.getPlayerName())) {
			if(playerData.getLastLogin() < ConfigManager.getMainConfig().getInt("new_player_login_threshold")) {
				//Check for alternate IPs on new players
				for(PlayerData player : getAllPlayers()) {
					if(player.getLastLogin() == -1) {
						for(String ip : player.getKnownIPs()) {
							if(knownIPs.contains(ip)) {
								player.addAlt(playerData.getPlayerName());
								playerData.addAlt(player.getPlayerName());
								PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
								return "You are not allowed to have alternate accounts on this server! Accounts: " + player.getKnownAlts().toString();
							}
						}
					}
				}
			} else {
				playerData.setLastLogin(System.currentTimeMillis());
				PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
			}
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
			if(playerData.getPlayerName().equals(player.getName())) {
				playerData.addIP(player.getIp());
				return playerData;
			}
		}
		return PlayerData.createDefault(player);
	}

	public static void initializeClient() {
		sendHardwareInfoToServer();
	}

	public static void initializeServer() {

	}

	private static List<PlayerData> getPlayersWithMatchingHIDs(long hardwareID) {
		List<PlayerData> matchingPlayers = new ArrayList<>();
		for(PlayerData playerData : getAllPlayers()) {
			if(playerData.getHardwareID() == hardwareID) matchingPlayers.add(playerData);
		}
		return matchingPlayers;
	}

	private static void sendHardwareInfoToServer() {
		SystemInfo systemInfo = new SystemInfo();
		OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
		CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
		ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();

		String vendor = operatingSystem.getManufacturer();
		String processorSerialNumber = computerSystem.getSerialNumber();
		CentralProcessor.ProcessorIdentifier processorIdentifier = centralProcessor.getProcessorIdentifier();
		int processors = centralProcessor.getLogicalProcessorCount();

		PacketUtil.sendPacketToServer(new SendHardwareInfoToServerPacket(vendor, processorSerialNumber, processorIdentifier.getProcessorID(), processors));
	}

	public static void assignUniqueID(PlayerState playerState, String vendor, String processorSerialNumber, String processorID, int processors) {
		PlayerData playerData = getPlayer(playerState);
		if(playerData.getHardwareID() == 0) { //First time logging in
			long newID = (vendor + processorSerialNumber + processorID + processors).hashCode();
			playerData.assignHardwareID(newID);
			PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		} else {
			long currentID = playerData.getHardwareID();
			long newID = (vendor + processorSerialNumber + processorID + processors).hashCode();
			if(currentID != newID) {
				playerData.assignHardwareID(newID);
				PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
			}
		}
	}

	public static boolean checkIfPlayerIsBanned(PlayerState playerState, PlayerData playerData) {
		return true;
	}
}
