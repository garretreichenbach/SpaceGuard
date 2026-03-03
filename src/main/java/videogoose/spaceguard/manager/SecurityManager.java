package videogoose.spaceguard.manager;

import api.common.GameServer;
import api.mod.ModSkeleton;
import api.mod.StarLoader;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerAccountEntrySet;
import org.schema.schine.network.RegisteredClientOnServer;
import org.schema.schine.network.StateInterface;
import org.schema.schine.network.commands.Login;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import videogoose.spaceguard.SpaceGuard;
import videogoose.spaceguard.data.PlayerData;
import videogoose.spaceguard.networking.client.SendClientInfoToServer;
import videogoose.spaceguard.utils.DataUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SecurityManager {

	// Small in-memory cache for IP check results to avoid hitting the external API repeatedly.
	private static final Map<String, CacheEntry> IP_CHECK_CACHE = new ConcurrentHashMap<>();
	private static final long IP_CHECK_TTL_MS = 10 * 60 * 1000; // 10 minutes
	// Single scheduled executor for delayed client tasks
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "SpaceGuard-Scheduler");
		t.setDaemon(true);
		return t;
	});

	public static List<Integer> approveMods(Set<Integer> mods) {
		List<Integer> illegalMods = new ArrayList<>();
		List<String> configuredApproved = ConfigManager.getMainConfig().getList("approved_client_mods");
		if(configuredApproved != null && !configuredApproved.isEmpty()) {
			// Build a local, mutable set that contains the configured approved mods and server-installed mod IDs.
			Set<String> approvedMods = new HashSet<>(configuredApproved);
			for(ModSkeleton serverMod : StarLoader.starMods) {
				approvedMods.add(String.valueOf(serverMod.getSmdResourceId()));
			}
			for(int modId : mods) {
				String mod = String.valueOf(modId);
				if(!approvedMods.contains(mod)) {
					illegalMods.add(modId);
				}
			}
		} else {
			SpaceGuard.getInstance().logWarning("Approved client mods list is null or empty in config, so we can't detect illegal client mods!");
		}
		return illegalMods;
	}

	public static Login.LoginCode getByName(String name) {
		for(Login.LoginCode code : Login.LoginCode.values()) {
			if(code.name().equals(name)) {
				return code;
			}
		}
		return null;
	}

	/** Returns the login code int for the given name, falling back to ERROR_YOU_ARE_BANNED if the name is not found. */
	private static int getCode(String name) {
		Login.LoginCode code = getByName(name);
		return code != null ? code.code : Login.LoginCode.ERROR_YOU_ARE_BANNED.code;
	}

	public static boolean checkIfPlayerIsBanned(PlayerData playerData) {
		PlayerAccountEntrySet accounts = GameServer.getServerState().getBlackListedAccounts();
		if(accounts.containsAndIsValid(playerData.getAccountName())) {
			return true;
		}
		PlayerAccountEntrySet ips = GameServer.getServerState().getBlackListedIps();
		for(String ip : playerData.getKnownIPs()) {
			if(ips.containsAndIsValid(ip)) {
				return true;
			}
		}
		PlayerAccountEntrySet names = GameServer.getServerState().getBlackListedNames();
		for(String alt : playerData.getKnownAlts()) {
			if(names.containsAndIsValid(alt)) {
				return true;
			}
		}
		return names.containsAndIsValid(playerData.getPlayerName());
	}

	public static void globalBanPlayer(PlayerData playerData) {
		HashSet<PlayerData> matchingPlayers = getPlayersWithMatchingData(playerData);
		for(PlayerData player : matchingPlayers) {
			try {
				player.addAlt(playerData.getPlayerName());
				playerData.addAlt(player.getPlayerName());
				PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
				for(String ip : player.getKnownIPs()) {
					GameServer.getServerState().getController().addBannedIp("Server", ip, -1);
				}
				GameServer.getServerState().getController().addBannedAccount("Server", player.getAccountName(), -1);
				GameServer.getServerState().getController().addBannedName("Server", player.getPlayerName(), -1);
				kickPlayer(player.getPlayerName(), "You have been banned from this server.");
			} catch(Exception exception) {
				SpaceGuard.getInstance().logException("An error occurred while banning player " + player.getPlayerName(), exception);
			}
		}
	}

	/**
	 * Checks a player's data to see if they are allowed to log in
	 *
	 * @param playerData The player's data
	 * @return -1 if the player can log in, otherwise returns a message explaining why they cannot
	 */
	public static int checkPlayer(PlayerData playerData) {
		if(checkIfPlayerIsBanned(playerData)) {
			return Login.LoginCode.ERROR_YOU_ARE_BANNED.code;
		}

		if(isAnyAltsAdmin(playerData)) {
			return -1;
		}

		//Check for VPNs
		for(String ip : playerData.getKnownIPs()) {
			if(!ip.contains("127.0.0.1") && !ip.contains("localhost")) {
				boolean[] vpnData = checkIP(ip);
				if(vpnData != null) {
					if(vpnData[0] && ConfigManager.getMainConfig().getBoolean("block_vpn") && !playerData.isTrusted(PlayerData.TRUSTED_VPN)) {
						return getCode("ERROR_VPN");
					}
					if(vpnData[1] && ConfigManager.getMainConfig().getBoolean("block_proxy") && !playerData.isTrusted(PlayerData.TRUSTED_PROXY)) {
						return getCode("ERROR_PROXY");
					}
					if(vpnData[2] && ConfigManager.getMainConfig().getBoolean("block_tor") && !playerData.isTrusted(PlayerData.TRUSTED_TOR)) {
						return getCode("ERROR_TOR");
					}
				}
			}
		}

		if(ConfigManager.getMainConfig().getBoolean("block_alts") && !playerData.isTrusted(PlayerData.TRUSTED_ALT)) {
			HashSet<PlayerData> matchingPlayers = getPlayersWithMatchingData(playerData);
			if(matchingPlayers.size() > 1) {
				for(PlayerData player : matchingPlayers) {
					player.addAlt(playerData.getPlayerName());
					playerData.addAlt(player.getPlayerName());
					PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
					return getCode("ERROR_ALT");
				}
			}
		}
		return -1;
	}

	private static boolean[] checkIP(String ip) {
		try {
			// Check cache first
			CacheEntry cached = IP_CHECK_CACHE.get(ip);
			long now = System.currentTimeMillis();
			if(cached != null && cached.expiresAt > now) {
				return cached.result;
			}

			String apiKey = ConfigManager.getMainConfig().getString("vpn_checker_api_key");
			if(apiKey == null || apiKey.trim().isEmpty() || "<API_KEY>".equals(apiKey.trim())) {
				SpaceGuard.getInstance().logWarning("VPN Checker API key not set; skipping IP checks. Set vpn_checker_api_key in config to enable.");
				return null;
			}

			// Build request URL safely
			String encodedIp = URLEncoder.encode(ip, StandardCharsets.UTF_8.name());
			String urlStr = "https://vpnapi.io/api/" + encodedIp + "?key=" + apiKey;
			HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);

			int status = conn.getResponseCode();
			if(status != HttpURLConnection.HTTP_OK) {
				SpaceGuard.getInstance().logWarning("VPN check returned HTTP " + status + " for IP: " + ip);
				return null;
			}

			StringBuilder sb = new StringBuilder();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while((line = reader.readLine()) != null) {
					sb.append(line);
				}
			}

			JSONObject jsonObject = new JSONObject(sb.toString());
			JSONObject security = jsonObject.optJSONObject("security");
			if(security == null) {
				SpaceGuard.getInstance().logWarning("VPN check response missing 'security' object for IP: " + ip);
				return null;
			}
			boolean vpn = security.optBoolean("vpn", false);
			boolean proxy = security.optBoolean("proxy", false);
			boolean tor = security.optBoolean("tor", false);
			boolean[] result = {vpn, proxy, tor};
			IP_CHECK_CACHE.put(ip, new CacheEntry(result, now + IP_CHECK_TTL_MS));
			return result;
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
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while getting player data for " + name, exception);
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

	private static void sendClientInfoToServer() {
		byte[] hardwareInfo = getHardwareInfo();
		List<ModSkeleton> mods = StarLoader.starMods;
		Set<Integer> modIds = new HashSet<>();
		for(int i = 0; i < mods.size(); i++) {
			modIds.add(i);
		}
		if(hardwareInfo != null) {
			PacketUtil.sendPacketToServer(new SendClientInfoToServer(hardwareInfo, modIds));
		}
	}

	public static void assignUniqueID(PlayerState playerState, byte[] data) {
		PlayerData playerData = getPlayer(playerState);
		String serverUuid = getServerUUID(playerState.getState());
		if(serverUuid == null) {
			SpaceGuard.getInstance().logWarning("Server UUID unavailable; skipping hardware ID assignment for " + playerState.getName());
			return;
		}
		try {
			String dataB64 = Base64.getEncoder().encodeToString(data == null ? new byte[0] : data);
			String preimage = serverUuid + ":" + dataB64;
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(preimage.getBytes(StandardCharsets.UTF_8));
			// Use first 8 bytes to produce a stable long (big-endian). Make non-negative by masking the sign bit.
			long raw = ByteBuffer.wrap(hash).getLong();
			long id = raw & Long.MAX_VALUE;
			playerData.addHardwareID(id);
			PersistentObjectUtil.save(SpaceGuard.getInstance().getSkeleton());
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while assigning unique ID", exception);
		}
	}

	private static String getServerUUID(StateInterface stateInterface) {
		assert stateInterface instanceof GameServerState : new IllegalAccessException("Server UUID can only be retrieved on the server");
		Path path = Paths.get(DataUtils.getWorldDataPath(), "server_secret.smdat");
		try {
			if(Files.exists(path)) {
				byte[] existingBytes = Files.readAllBytes(path);
				String existing = new String(existingBytes, StandardCharsets.UTF_8).trim();
				if(!existing.isEmpty()) {
					return existing;
				}
			}

			// Not present or empty: create atomically
			String uuid = UUID.randomUUID().toString();
			Path tmp = Paths.get(path + ".tmp");
			Files.write(tmp, uuid.getBytes(StandardCharsets.UTF_8));
			try {
				Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE);
			} catch(AtomicMoveNotSupportedException amnse) {
				// Fallback to replace existing
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}

			// Try to set restrictive POSIX permissions if supported
			try {
				Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				Files.setPosixFilePermissions(path, perms);
			} catch(UnsupportedOperationException ignored) {
				// Not POSIX (e.g., Windows) -- ignore
			}

			return uuid;
		} catch(FileAlreadyExistsException faee) {
			// Race: another thread created it; read the existing file
			try {
				byte[] existingBytes = Files.readAllBytes(path);
				return new String(existingBytes, StandardCharsets.UTF_8).trim();
			} catch(Exception e) {
				SpaceGuard.getInstance().logException("An error occurred while reading server UUID after race", e);
				return null;
			}
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while getting server UUID", exception);
		}
		return null;
	}

	private static boolean isAnyAltsAdmin(PlayerData playerData) {
		for(String playerName : playerData.getKnownAlts()) {
			if(GameServer.getServerState().isAdmin(playerName)) {
				return true;
			}
		}
		return false;
	}

	private static byte[] getHardwareInfo() {
		// Opt-in: do not collect hardware fingerprint unless explicitly enabled in config
		try {
			boolean enabled = false;
			try {
				enabled = ConfigManager.getMainConfig().getBoolean("collect_hardware_fingerprint");
			} catch(Exception ignored) {
			}
			if(!enabled) {
				SpaceGuard.getInstance().logInfo("Hardware fingerprinting disabled by config; not collecting hardware info.");
				return null;
			}
			byte[] raw;
			try {
				raw = getHardwareInfoFromOSHI();
			} catch(LinkageError | RuntimeException exception) {
				SpaceGuard.getInstance().logWarning("OSHI hardware probe unavailable, using fallback fingerprint method: " + exception);
				raw = getHardwareInfoFallback();
			}
			if(raw == null || raw.length == 0) {
				return null;
			}
			// Compute one-way SHA-256 hash of the hardware fingerprint; do not send raw identifiers
			try {
				return MessageDigest.getInstance("SHA-256").digest(raw);
			} catch(NoSuchAlgorithmException e) {
				SpaceGuard.getInstance().logException("SHA-256 not available for hardware hashing", e);
				return null;
			}
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while collecting hardware info", exception);
			return null;
		}
	}

	private static byte[] getHardwareInfoFromOSHI() {
		SystemInfo systemInfo = new oshi.SystemInfo();
		OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		HardwareAbstractionLayer hardware = systemInfo.getHardware();
		String processorID = hardware.getProcessor().getProcessorIdentifier().getProcessorID();
		String processorArch = hardware.getProcessor().getProcessorIdentifier().getMicroarchitecture();
		int processors = hardware.getProcessor().getLogicalProcessorCount();
		String os = operatingSystem.getFamily();
		String hardwareUUID = hardware.getComputerSystem().getHardwareUUID();
		String firmware = hardware.getComputerSystem().getFirmware().getName();
		return (processorID + processorArch + processors + os + hardwareUUID + firmware).getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] getHardwareInfoFallback() {
		List<String> macAddresses = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while(networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if(!networkInterface.isLoopback()) {
					byte[] mac = networkInterface.getHardwareAddress();
					if(mac != null) {
						StringBuilder sb = new StringBuilder();
						for(int i = 0; i < mac.length; i++) {
							sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
						}
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
			return (macAddresses + processorID + processorArch + processors + os + osArch).getBytes(StandardCharsets.UTF_8);
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while collecting fallback hardware info", exception);
		}
		return null;
	}

	public static void initializeClient() {
		// Schedule the client info send after 5 seconds using a daemon scheduler to avoid raw threads
		SCHEDULER.schedule(() -> {
			try {
				sendClientInfoToServer();
			} catch(Exception e) {
				SpaceGuard.getInstance().logException("An error occurred while initializing client", e);
			} catch(Throwable t) {
				SpaceGuard.getInstance().logWarning("An error occurred while initializing client: " + t);
			}
		}, 5, TimeUnit.SECONDS);
	}

	public static void kickPlayer(String playerName, String reason) {
		try {
			PlayerState playerState = GameServer.getServerState().getPlayerFromName(playerName);
			SpaceGuard.getInstance().logInfo("Kicking player " + playerName + " for reason: " + reason);
			SpaceGuard.logDiscordMessage("Kicking player " + playerName + " for reason: " + reason);
			GameServer.getServerState().getController().sendLogout(playerState.getClientId(), reason);
			GameServer.getServerState().getController().unregister(playerState.getClientId());
		} catch(Exception exception) {
			SpaceGuard.getInstance().logException("An error occurred while kicking player " + playerName, exception);
		}
	}

	private static class CacheEntry {
		final boolean[] result;
		final long expiresAt;

		CacheEntry(boolean[] result, long expiresAt) {
			this.result = result;
			this.expiresAt = expiresAt;
		}
	}
}

