package username.modtemplate.manager;

import api.network.packets.PacketUtil;
import username.modtemplate.network.client.ExampleClientPacket;

public class PacketManager {

	public static void initialize() {
		PacketUtil.registerPacket(ExampleClientPacket.class);
	}
}