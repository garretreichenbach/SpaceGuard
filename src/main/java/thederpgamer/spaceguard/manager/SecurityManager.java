package thederpgamer.spaceguard.manager;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.lwjgl.Sys;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerAccountEntrySet;
import org.schema.schine.network.RegisteredClientOnServer;
import org.schema.schine.network.StateInterface;
import org.schema.schine.network.commands.Login;
import thederpgamer.spaceguard.SpaceGuard;
import thederpgamer.spaceguard.data.PlayerData;
import thederpgamer.spaceguard.networking.client.SendHardwareInfoToServerPacket;
import thederpgamer.spaceguard.utils.DataUtils;

import java.io.File;
import java.io.FileWriter;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public final class SecurityManager {

	/**
	 * Checks a player's data to see if they are allowed to log in
	 *
	 * @param playerData The player's data
	 * @return -1 if the player can log in, otherwise returns a message explaining why they cannot
	 */
	public static int checkPlayer(PlayerData playerData) {
		if(checkIfPlayerIsBanned(playerData)) return Login.LoginCode.ERROR_YOU_ARE_BANNED.code;
		if(isAnyAltsAdmin(playerData)) return -1;
		//Check for VPNs
		for(String ip : playerData.getKnownIPs()) {
			if(!ip.contains("127.0.0.1") && !ip.contains("localhost")) {
				boolean[] vpnData = checkIP(ip);
				if(vpnData != null) {
					if(vpnData[0] && ConfigManager.getMainConfig().getBoolean("block_vpn")) return Login.LoginCode.ERROR_VPN.code;
					if(vpnData[1] && ConfigManager.getMainConfig().getBoolean("block_proxy")) return Login.LoginCode.ERROR_PROXY.code;
					if(vpnData[2] && ConfigManager.getMainConfig().getBoolean("block_tor")) return Login.LoginCode.ERROR_TOR.code;
				}
			}
		}

		try {
			if(ConfigManager.getMainConfig().getBoolean("block_alt_usernames")) {
				//Check for alternate players under the same account
				String accountName = playerData.getAccountName();
				for(PlayerData player : getAllPlayers()) {
					if(player.getAccountName().equals(accountName) && !player.getPlayerName().equals(playerData.getPlayerName())) {
						player.addAlt(playerData.getPlayerName());
						playerData.addAlt(player.getPlayerName());
						PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
						return Login.LoginCode.ERROR_NO_ALTS.code;
					}
				}
			}
		} catch(NullPointerException ignored) {
		}

		if(ConfigManager.getMainConfig().getBoolean("check_hardware_ids")) {
			//Check hardware IDs
			for(long hardwareID : playerData.getHardwareIDs()) {
				if(hardwareID != 0) {
					for(PlayerData player : getAllPlayers()) {
						if(player.getHardwareIDs().contains(hardwareID) && !player.getPlayerName().equals(playerData.getPlayerName())) {
							player.addAlt(playerData.getPlayerName());
							playerData.addAlt(player.getPlayerName());
							PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
							return Login.LoginCode.ERROR_NO_ALTS.code;
						}
					}
				}
			}
		}
		return -1;
	}

	private static boolean[] checkIP(String ip) {
		if(ConfigManager.getMainConfig().getString("vpn_checker_api_key").equals("<API_KEY>")) {
			SpaceGuard.getInstance().logWarning("VPN Checker API key not set. Please make an account at https://vpnapi.io/api-documentation and set the API_KEY in the config.");
			return null;
		}
		String url = "https://vpnapi.io/api/" + ip + "?key=" + ConfigManager.getMainConfig().getString("vpn_checker_api_key");
		try {
			JSONObject jsonObject = new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8));
			JSONObject security = jsonObject.getJSONObject("security");
			boolean vpn = security.getBoolean("vpn");
			boolean proxy = security.getBoolean("proxy");
			boolean tor = security.getBoolean("tor");
			return new boolean[]{vpn, proxy, tor};
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while checking IP for " + ip, exception);
			return null;
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

	public static PlayerData getPlayer(String name) {
		for(PlayerData playerData : getAllPlayers()) {
			if(playerData.getPlayerName().equals(name)) {
				return playerData;
			}
		}
		try {
			return getPlayer(GameServer.getServerState().getPlayerFromName(name));
		} catch(Exception ignored) {
		}
		return null;
	}

	public static PlayerData getPlayer(RegisteredClientOnServer client) {
		for(PlayerData playerData : getAllPlayers()) {
			if(playerData.getPlayerName().equals(client.getPlayerName())) {
				playerData.addIP(client.getIp());
				return playerData;
			}
		}
		return PlayerData.createDefault(client);
	}

	public static void initializeClient() {
		(new Thread() {
			@Override
			public void run() {
				try {
					sleep(5000);
					sendHardwareInfoToServer();
				} catch(InterruptedException exception) {
					SpaceGuard.getInstance().logException("An error occurred while initializing client", exception);
				}
			}
		}).start();
	}

	private static HashSet<PlayerData> getPlayersWithMatchingData(PlayerData playerData) {
		HashSet<PlayerData> matchingPlayers = new HashSet<>();
		for(PlayerData pd : getAllPlayers()) {
			Set<Long> hardwareIDs = pd.getHardwareIDs();
			Set<String> knownIPs = pd.getKnownIPs();
			Set<String> knownAlts = pd.getKnownAlts();
			for(long hardwareID : hardwareIDs) {
				if(playerData.getHardwareIDs().contains(hardwareID)) {
					matchingPlayers.add(pd);
					break;
				}
			}

			for(String ip : knownIPs) {
				if(playerData.getKnownIPs().contains(ip)) {
					matchingPlayers.add(pd);
					break;
				}
			}

			for(String alt : knownAlts) {
				if(playerData.getKnownAlts().contains(alt)) {
					matchingPlayers.add(pd);
					break;
				}
			}
		}
		return matchingPlayers;
	}

	private static void sendHardwareInfoToServer() {
		byte[] hardwareInfo = getHardwareInfo();
		if(hardwareInfo != null) PacketUtil.sendPacketToServer(new SendHardwareInfoToServerPacket(hardwareInfo));
	}

	public static void assignUniqueID(PlayerState playerState, byte[] data) {
		PlayerData playerData = getPlayer(playerState);
		long id = Math.abs(Arrays.hashCode(data)) + Math.abs(Objects.requireNonNull(getServerUUID(playerState.getState())).hashCode());
		playerData.addHardwareID(id);
		PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
	}

	public static boolean checkIfPlayerIsBanned(PlayerData playerData) {
		PlayerAccountEntrySet accounts = GameServer.getServerState().getBlackListedAccounts();
		if(accounts.containsAndIsValid(playerData.getAccountName())) return true;
		PlayerAccountEntrySet ips = GameServer.getServerState().getBlackListedIps();
		for(String ip : playerData.getKnownIPs()) {
			if(ips.containsAndIsValid(ip)) return true;
		}
		PlayerAccountEntrySet names = GameServer.getServerState().getBlackListedNames();
		for(String alt : playerData.getKnownAlts()) {
			if(names.containsAndIsValid(alt)) return true;
		}
		return names.containsAndIsValid(playerData.getPlayerName());
	}

	public static void globalBanPlayer(PlayerData playerData) {
		HashSet<PlayerData> matchingPlayers = getPlayersWithMatchingData(playerData);
		for(PlayerData player : matchingPlayers) {
			try {
				for(String ip : player.getKnownIPs()) GameServer.getServerState().getController().addBannedIp("Server", ip, -1);
				GameServer.getServerState().getController().addBannedAccount("Server", player.getAccountName(), -1);
				GameServer.getServerState().getController().addBannedName("Server", player.getPlayerName(), -1);
			} catch(Exception exception) {
				SpaceGuard.getInstance().logException("An error occurred while banning player " + player.getPlayerName(), exception);
			}
		}
	}

	private static String getServerUUID(StateInterface stateInterface) {
		assert stateInterface instanceof GameServerState : new IllegalAccessException("Server UUID can only be retrieved on the server");
		try {
			File serverSecret = new File(DataUtils.getWorldDataPath() + "/server_secret.smdat");
			if(!serverSecret.exists()) {
				String uuid = UUID.randomUUID().toString();
				serverSecret.createNewFile();
				FileWriter writer = new FileWriter(serverSecret);
				writer.write(uuid);
				writer.close();
				return uuid;
			} else return IOUtils.toString(serverSecret.toURI(), StandardCharsets.UTF_8);
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while getting server UUID", exception);
		}
		return null;
	}

	private static boolean isAnyAltsAdmin(PlayerData playerData) {
		for(String playerName : playerData.getKnownAlts()) {
			if(GameServer.getServerState().isAdmin(playerName)) return true;
		}
		return false;
	}

	private static byte[] getHardwareInfo() {
		List<String> macAddresses = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while(networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if(!networkInterface.isLoopback()) {
					byte[] mac = networkInterface.getHardwareAddress();
					if(mac != null) {
						StringBuilder sb = new StringBuilder();
						for(int i = 0; i < mac.length; i++) sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
						macAddresses.add(sb.toString());
					}
				}
			}
		} catch(SocketException exception) {
			SpaceGuard.getInstance().logException("An error occurred while getting MAC address", exception);
		}

		try {
			String processorID = System.getenv("PROCESSOR_IDENTIFIER");
			String processorArch = System.getenv("PROCESSOR_ARCHITECTURE");
			int processors = Runtime.getRuntime().availableProcessors();
			String os = System.getProperty("os.name");
			String osArch = System.getProperty("os.arch");
//			String osVersion = System.getProperty("os.version"); //This is changed by the system upon updating, so it's not a good idea to use it
			String userName = System.getProperty("user.name"); //This technically can be changed by the user, but it's usually a pain in the ass to do so
			String userHome = System.getProperty("user.home"); //This technically can be changed by the user, but it's usually a pain in the ass to do so
			return (macAddresses + processorID + processorArch + processors + os + osArch + userName + userHome).getBytes(StandardCharsets.UTF_8);
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while sending hardware info to server", exception);
		}
		return null;
	}
}
