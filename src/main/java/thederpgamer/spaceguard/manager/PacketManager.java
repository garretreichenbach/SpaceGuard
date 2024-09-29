package thederpgamer.spaceguard.manager;

import api.network.packets.PacketUtil;
import thederpgamer.spaceguard.networking.client.SendClientInfoToServer;

public final class PacketManager {

	public static void initialize() {
		PacketUtil.registerPacket(SendClientInfoToServer.class);
	}
}