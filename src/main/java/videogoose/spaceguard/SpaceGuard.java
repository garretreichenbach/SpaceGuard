package videogoose.spaceguard;

import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import videogoose.spaceguard.data.DiscordWebhook;
import videogoose.spaceguard.data.commands.GetPlayerDataCommand;
import videogoose.spaceguard.data.commands.GlobalBanCommand;
import videogoose.spaceguard.data.commands.TrustPlayerCommand;
import videogoose.spaceguard.manager.ConfigManager;
import videogoose.spaceguard.manager.EventManager;
import videogoose.spaceguard.manager.PacketManager;
import videogoose.spaceguard.manager.SecurityManager;
import videogoose.spaceguard.networking.client.SendClientInfoToServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpaceGuard extends StarMod {

	private static final String[] overwriteClasses = {"Login"};

	private static SpaceGuard instance;

	public SpaceGuard() {
		instance = this;
	}

	public static SpaceGuard getInstance() {
		return instance;
	}

	public static void logDiscordMessage(String message) {
		if("<WEBHOOK_URL>".equals(ConfigManager.getMainConfig().getString("discord_webhook_url"))) {
			instance.logWarning("Discord webhook URL not set. Please set the WEBHOOK_URL in the config.");
		} else {
			try {
				DiscordWebhook webhook = new DiscordWebhook(ConfigManager.getMainConfig().getString("discord_webhook_url"));
				webhook.setContent(message);
				webhook.execute();
			} catch(Exception exception) {
				instance.logException("An error occurred while sending message to Discord", exception);
			}
		}
	}

	@Override
	public void onEnable() {
		super.onEnable();
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		PacketManager.initialize();
		registerCommands();
		registerPackets();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		SecurityManager.initializeClient();
	}

	@Override
	public void logInfo(String message) {
		System.out.println("[SpaceGuard] [INFO]: " + message);
		super.logInfo(message);
	}

	@Override
	public void logWarning(String message) {
		System.out.println("[SpaceGuard] [WARNING]: " + message);
		super.logWarning(message);
	}

	@Override
	public void logException(String message, Exception exception) {
		System.err.println("[SpaceGuard] [EXCEPTION]: " + message + "\n" + exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()));
		exception.printStackTrace();
		super.logException(message, exception);
	}

	@Override
	public void logFatal(String message, Exception exception) {
		System.err.println("[SpaceGuard] [FATAL]: " + message + "\n" + exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()));
		exception.printStackTrace();
		super.logFatal(message, exception);
	}

	@Override
	public byte[] onClassTransform(String className, byte[] byteCode) {
		for(String name : overwriteClasses) {
			if(className.endsWith(name)) return overwriteClass(className, byteCode);
		}
		return super.onClassTransform(className, byteCode);
	}

	private byte[] overwriteClass(String className, byte[] byteCode) {
		byte[] bytes = null;
		try {
			ZipInputStream file = new ZipInputStream(Files.newInputStream(getSkeleton().getJarFile().toPath()));
			while(true) {
				ZipEntry nextEntry = file.getNextEntry();
				if(nextEntry == null) {
					break;
				}
				if(nextEntry.getName().endsWith(className + ".class")) {
					bytes = Files.readAllBytes(new File(getSkeleton().getJarFile().getAbsolutePath() + "!/" + nextEntry.getName()).toPath());
					break;
				}
			}
			file.close();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		if(bytes != null) {
			return bytes;
		} else {
			return byteCode;
		}
	}

	private void registerCommands() {
		StarLoader.registerCommand(new GlobalBanCommand());
		StarLoader.registerCommand(new GetPlayerDataCommand());
		StarLoader.registerCommand(new TrustPlayerCommand());
	}

	private void registerPackets() {
		PacketUtil.registerPacket(SendClientInfoToServer.class);
	}
}
