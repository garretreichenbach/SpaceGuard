package username.modtemplate.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import username.modtemplate.ModTemplate;

import java.io.IOException;

public class ExampleClientPacket extends Packet {

	private String message;

	public ExampleClientPacket() { //Packets require an empty default constructor for the packet registry

	}

	public ExampleClientPacket(String message) {
		this.message = message;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		message = packetReadBuffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(message);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		ModTemplate.logInfo("Received packet from " + playerState.getName() + " with message: " + message);
	}
}
