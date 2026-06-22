package videogoose.spaceguard.networking.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import videogoose.spaceguard.manager.SecurityManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SendClientInfoToServer extends Packet {

	private byte[] data = new byte[0];
	/**
	 * Maps mod resource ID -> "name:sha256hex" — both the reported mod name and
	 * the SHA-256 hash of the client's mod jar, so the server can verify both.
	 */
	private Map<Integer, String> mods = new HashMap<>();

	public SendClientInfoToServer(byte[] data, Map<Integer, String> mods) {
		this.data = data;
		this.mods = mods;
	}

	public SendClientInfoToServer() {
	}

	// Defensive caps — this packet is sent by (untrusted) clients, so reject absurd values
	// rather than letting a malicious client drive unbounded allocation/work on the server.
	private static final int MAX_MODS = 4096;
	private static final int MAX_NAME_LENGTH = 1024;

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		data = packetReadBuffer.readByteArray();
		int size = packetReadBuffer.readInt();
		if(size < 0 || size > MAX_MODS) {
			throw new IOException("SendClientInfoToServer: illegal mod count " + size);
		}
		for(int i = 0; i < size; i++) {
			int id = packetReadBuffer.readInt();
			String name = packetReadBuffer.readString();
			if(name == null) {
				name = "";
			} else if(name.length() > MAX_NAME_LENGTH) {
				name = name.substring(0, MAX_NAME_LENGTH);
			}
			mods.put(id, name);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeByteArray(data);
		packetWriteBuffer.writeInt(mods.size());
		for(Map.Entry<Integer, String> entry : mods.entrySet()) {
			packetWriteBuffer.writeInt(entry.getKey());
			packetWriteBuffer.writeString(entry.getValue());
		}
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		List<Integer> illegalMods = SecurityManager.approveMods(mods);
		if(illegalMods.isEmpty()) {
			SecurityManager.assignUniqueID(playerState, data);
		} else {
			SecurityManager.kickPlayer(playerState.getName(), "You have the following illegal mods installed: " + illegalMods);
		}
	}
}
