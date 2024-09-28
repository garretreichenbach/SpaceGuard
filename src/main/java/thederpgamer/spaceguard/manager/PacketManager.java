package thederpgamer.spaceguard.manager;

import api.network.packets.PacketUtil;
import thederpgamer.spaceguard.networking.client.SendHardwareInfoToServerPacket;

public class PacketManager {

	public static void initialize() {
		PacketUtil.registerPacket(SendHardwareInfoToServerPacket.class);
	}
}