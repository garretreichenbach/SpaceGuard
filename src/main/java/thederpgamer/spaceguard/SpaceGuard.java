package thederpgamer.spaceguard;

import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarMod;
import org.apache.commons.io.IOUtils;
import thederpgamer.spaceguard.manager.SecurityManager;
import thederpgamer.spaceguard.manager.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SpaceGuard extends StarMod {

	//Instance
	private static SpaceGuard instance;

	//Use this to overwrite specific vanilla classes
	private final String[] overwriteClasses = {};

	public SpaceGuard() {
		instance = this;
	}

	public static SpaceGuard getInstance() {
		return instance;
	}

	@Override
	public void onEnable() {
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		PacketManager.initialize();
	}

	@Override
	public void onServerCreated(ServerInitializeEvent event) {
		super.onServerCreated(event);
		KeyManager.initializeServer(event.getServerState());
		SecurityManager.initializeServer();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		super.onClientCreated(event);
		KeyManager.initializeClient(event.getClientState());
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

	public void logException(String message, Exception exception) {
		System.err.println("[SpaceGuard] [EXCEPTION]: " + message + "\n" + exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()));
		super.logException(message, exception);
	}

	public void logFatal(String message, Exception exception) {
		System.err.println("[SpaceGuard] [FATAL]: " + message + "\n" + exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()));
		super.logFatal(message, exception);
	}

	@Override
	public byte[] onClassTransform(String className, byte[] byteCode) {
		for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);
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
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(bytes != null) return bytes;
		else return byteCode;
	}
}
