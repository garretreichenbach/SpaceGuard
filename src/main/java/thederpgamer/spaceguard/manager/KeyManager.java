package thederpgamer.spaceguard.manager;

import org.schema.game.client.data.GameClientState;
import org.schema.game.client.data.GameStateInterface;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.StateInterface;
import thederpgamer.spaceguard.utils.EncryptionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class KeyManager {

	private final String SERVER_KEY;
	private final String CLIENT_KEY;

	private static KeyManager instance;

	private KeyManager(StateInterface state) {
		if(state instanceof GameServerState) {
			SERVER_KEY = new String(Objects.requireNonNull(EncryptionUtils.encryptData(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
			CLIENT_KEY = null;
		} else if(state instanceof GameStateInterface) {
			SERVER_KEY = null;
			CLIENT_KEY = new String(Objects.requireNonNull(EncryptionUtils.encryptData(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
		} else {
			SERVER_KEY = null;
			CLIENT_KEY = null;
			throw new IllegalArgumentException("Invalid state interface");
		}
	}

	public static void initializeClient(GameClientState state) {

	}

	public static void initializeServer(GameServerState state) {

	}
}
