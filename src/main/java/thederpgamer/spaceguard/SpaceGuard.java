package thederpgamer.spaceguard;

import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.apache.commons.io.IOUtils;
import thederpgamer.spaceguard.data.DiscordWebhook;
import thederpgamer.spaceguard.data.commands.GetPlayerDataCommand;
import thederpgamer.spaceguard.data.commands.GlobalBanCommand;
import thederpgamer.spaceguard.manager.ConfigManager;
import thederpgamer.spaceguard.manager.EventManager;
import thederpgamer.spaceguard.manager.PacketManager;
import thederpgamer.spaceguard.manager.SecurityManager;
import thederpgamer.spaceguard.networking.client.SendClientInfoToServer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpaceGuard extends StarMod {

	//Instance
	private static SpaceGuard instance;
	public SpaceGuard() {
		instance = this;
	}
	public static SpaceGuard getInstance() {
		return instance;
	}
	private static final String[] overwriteClasses = {"Login"};

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
				if(nextEntry == null) break;
				if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
			}
			file.close();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		if(bytes != null) return bytes;
		else return byteCode;
	}

	private void registerCommands() {
		StarLoader.registerCommand(new GlobalBanCommand());
		StarLoader.registerCommand(new GetPlayerDataCommand());
	}

	private void registerPackets() {
		PacketUtil.registerPacket(SendClientInfoToServer.class);
	}

	public static void logDiscordMessage(String message) {
		if(ConfigManager.getMainConfig().getString("discord_webhook_url").equals("<WEBHOOK_URL>")) SpaceGuard.getInstance().logWarning("Discord webhook URL not set. Please set the WEBHOOK_URL in the config.");
		else {
			try {
				DiscordWebhook webhook = new DiscordWebhook(ConfigManager.getMainConfig().getString("discord_webhook_url"));
				webhook.setContent(message);
				webhook.execute();
			} catch(Exception exception) {
				instance.logException("An error occurred while sending message to Discord", exception);
			}
		}
	}
}
