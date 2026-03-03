package videogoose.spaceguard.manager;

import videogoose.spaceguard.SpaceGuard;
import videogoose.spaceguard.data.DiscordWebhook;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sends colour-coded Discord embed notifications for security events.
 * Uses a dedicated daemon thread so webhook calls never block the server.
 *
 * Colours used:
 *   RED    — player blocked / banned
 *   ORANGE — suspicious activity / warning
 *   GREEN  — informational (player allowed, hardware ID assigned)
 *   BLUE   — system events (mod check, client connected)
 */
public final class NoticeManager {

	// -----------------------------------------------------------------------
	// Colours
	// -----------------------------------------------------------------------
	private static final Color RED = new Color(0xE74C3C);
	private static final Color ORANGE = new Color(0xE67E22);
	private static final Color GREEN = new Color(0x2ECC71);
	private static final Color BLUE = new Color(0x3498DB);

	// -----------------------------------------------------------------------
	// Async executor — daemon so it won't block server shutdown
	// -----------------------------------------------------------------------
	private static final ScheduledExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "SpaceGuard-Webhook");
		t.setDaemon(true);
		return t;
	});

	private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

	// -----------------------------------------------------------------------
	// Public API — one method per security event
	// -----------------------------------------------------------------------

	/** Player was blocked at login (VPN / proxy / Tor / alt / ban). */
	public static void loginBlocked(String playerName, String ip, String reason) {
		send("🚫 Login Blocked", RED, "**" + escape(playerName) + "** was prevented from logging in.", field("Player", playerName, true), field("IP", ip, true), field("Reason", reason, false));
	}

	/** Player was kicked after connecting. */
	public static void playerKicked(String playerName, String reason) {
		send("👢 Player Kicked", ORANGE, "**" + escape(playerName) + "** was kicked from the server.", field("Player", playerName, true), field("Reason", reason, false));
	}

	/** Player (and all alts) were globally banned. */
	public static void playerBanned(String playerName, String bannedBy) {
		send("🔨 Player Banned", RED, "**" + escape(playerName) + "** was banned.", field("Player", playerName, true), field("Banned By", bannedBy, true));
	}


	public static void playerUnbanned(String playerIp, String unbannedBy) {
		send("♻️ Player Unbanned", GREEN, "A player IP was unbanned.", field("IP", playerIp, true), field("Unbanned By", unbannedBy, true));
	}

	/** An illegal client mod was detected on a connecting player. */
	public static void illegalModDetected(String playerName, String ip, String modIds) {
		send("⚠️ Illegal Mod Detected", ORANGE, "**" + escape(playerName) + "** has unapproved mods loaded.", field("Player", playerName, true), field("IP", ip, true), field("Mod IDs", modIds, false));
	}

	/** Hardware ID was assigned / updated for a player. */
	public static void hardwareIdAssigned(String playerName, long hardwareId) {
		send("🖥️ Hardware ID Assigned", GREEN, "A hardware fingerprint was linked to **" + escape(playerName) + "**.", field("Player", playerName, true), field("Hardware ID", String.valueOf(hardwareId), true));
	}

	/** A VPN / proxy / Tor was detected on an IP (even if the player was trusted). */
	public static void vpnDetected(String playerName, String ip, boolean vpn, boolean proxy, boolean tor) {
		StringBuilder flags = new StringBuilder();
		if(vpn) {
			flags.append("VPN ");
		}
		if(proxy) {
			flags.append("Proxy ");
		}
		if(tor) {
			flags.append("Tor ");
		}
		send("🕵️ VPN/Proxy Detected", ORANGE, "A flagged IP was detected for **" + escape(playerName) + "**.", field("Player", playerName, true), field("IP", ip, true), field("Flags", flags.toString().trim(), false));
	}

	/** Player's IP was resolved as a known alt of another account. */
	public static void altDetected(String playerName, String knownAlt) {
		send("👥 Alt Account Detected", ORANGE, "**" + escape(playerName) + "** appears to be an alt of **" + escape(knownAlt) + "**.", field("Player", playerName, true), field("Known Alt", knownAlt, true));
	}

	/** A new client connected and sent hardware info. */
	public static void clientConnected(String playerName, String ip) {
		send("🟢 Client Connected", BLUE, "**" + escape(playerName) + "** connected and sent client info.", field("Player", playerName, true), field("IP", ip, true));
	}

	/** Generic security warning that doesn't fit another category. */
	public static void securityWarning(String title, String description) {
		send("⚠️ " + title, ORANGE, description);
	}

	// -----------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------

	private static FieldData field(String name, String value, boolean inline) {
		return new FieldData(name, value == null ? "N/A" : value, inline);
	}

	private static void send(String title, Color color, String description, FieldData... fields) {
		if(!isEnabled()) {
			return;
		}
		String webhookUrl = getWebhookUrl();
		if(webhookUrl == null) {
			return;
		}

		// Snapshot values for the lambda
		WEBHOOK_EXECUTOR.submit(() -> {
			try {
				DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
				DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject().setTitle(title).setDescription(description).setColor(color).setFooter("SpaceGuard • " + TIMESTAMP_FMT.format(Instant.now()), null);
				for(FieldData f : fields) {
					embed.addField(f.name, f.value, f.inline);
				}
				webhook.addEmbed(embed);
				webhook.execute();
			} catch(Exception e) {
				SpaceGuard.getInstance().logException("Failed to send security notice to Discord", e);
			}
		});
	}

	private static boolean isEnabled() {
		try {
			return ConfigManager.getMainConfig().getBoolean("security_alerts_enabled");
		} catch(Exception e) {
			return false;
		}
	}

	private static String getWebhookUrl() {
		try {
			String url = ConfigManager.getMainConfig().getString("security_alert_webhook_url");
			if(url == null || url.trim().isEmpty() || "<WEBHOOK_URL>".equals(url.trim())) {
				return null;
			}
			return url.trim();
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Escape Discord markdown in player-supplied strings.
	 */
	private static String escape(String s) {
		if(s == null) {
			return "N/A";
		}
		return s.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_").replace("`", "\\`").replace("~", "\\~");
	}

	private static class FieldData {
		final String name;
		final String value;
		final boolean inline;

		FieldData(String name, String value, boolean inline) {
			this.name = name;
			this.value = value;
			this.inline = inline;
		}
	}
}

