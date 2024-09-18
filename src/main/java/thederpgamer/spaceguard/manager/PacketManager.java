package thederpgamer.spaceguard.manager;

import api.network.packets.PacketUtil;
import thederpgamer.spaceguard.networking.client.ExampleClientPacket;

public class PacketManager {

	public static void initialize() {
		PacketUtil.registerPacket(ExampleClientPacket.class);
	}
}