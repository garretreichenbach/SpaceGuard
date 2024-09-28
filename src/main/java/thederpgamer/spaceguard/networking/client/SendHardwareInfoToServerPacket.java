package thederpgamer.spaceguard.networking.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.manager.SecurityManager;

import java.io.IOException;

public class SendHardwareInfoToServerPacket extends Packet {

	private byte[] data;

	public SendHardwareInfoToServerPacket(byte[] data) {
		this.data = data;
	}

	public SendHardwareInfoToServerPacket() {}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		data = packetReadBuffer.readByteArray();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeByteArray(data);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		SecurityManager.assignUniqueID(playerState, data);
	}
}
