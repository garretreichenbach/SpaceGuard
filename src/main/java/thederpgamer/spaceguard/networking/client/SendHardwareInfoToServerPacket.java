package thederpgamer.spaceguard.networking.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.manager.SecurityManager;

import java.io.IOException;

public class SendHardwareInfoToServerPacket extends Packet {

	private String vendor;
	private String processorSerialNumber;
	private String processorID;
	private int processors;

	public SendHardwareInfoToServerPacket(String vendor, String processorSerialNumber, String processorID, int processors) {
		this.vendor = vendor;
		this.processorSerialNumber = processorSerialNumber;
		this.processorID = processorID;
		this.processors = processors;
	}

	public SendHardwareInfoToServerPacket() {}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		vendor = packetReadBuffer.readString();
		processorSerialNumber = packetReadBuffer.readString();
		processorID = packetReadBuffer.readString();
		processors = packetReadBuffer.readInt();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(vendor);
		packetWriteBuffer.writeString(processorSerialNumber);
		packetWriteBuffer.writeString(processorID);
		packetWriteBuffer.writeInt(processors);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		SecurityManager.assignUniqueID(playerState, vendor, processorSerialNumber, processorID, processors);
	}
}
