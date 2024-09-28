package thederpgamer.spaceguard.networking.server;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class AssignUniqueIDPacket extends Packet {

	private long id;

	public AssignUniqueIDPacket(long id) {
		this.id = id;
	}

	public AssignUniqueIDPacket() {}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		id = packetReadBuffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeLong(id);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {

	}
}
