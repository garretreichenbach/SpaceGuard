package thederpgamer.spaceguard.networking.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.spaceguard.manager.SecurityManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SendClientInfoToServer extends Packet {

	private byte[] data = new byte[0];
	private Set<Integer> mods = new HashSet<>();

	public SendClientInfoToServer(byte[] data, Set<Integer> mods) {
		this.data = data;
		this.mods = mods;
	}

	public SendClientInfoToServer() {}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		data = packetReadBuffer.readByteArray();
		mods.addAll(packetReadBuffer.readIntList());
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeByteArray(data);
		packetWriteBuffer.writeIntList(mods);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		List<Integer> illegalMods = SecurityManager.approveMods(mods);
		if(illegalMods.isEmpty()) SecurityManager.assignUniqueID(playerState, data);
		else SecurityManager.kickPlayerForIllegalMods(playerState, illegalMods);
	}
}
