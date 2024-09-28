package thederpgamer.spaceguard.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class EncryptionUtils {

	public static byte[] encryptData(byte[] data) {
		try {
			SecretKey secretKey = generateSecretKey();
			return encryptData(secretKey, data);
		} catch(Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public static byte[] decryptData(SecretKey secretKey, byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return cipher.doFinal(data);
	}

	private static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256, new SecureRandom());
		return keyGen.generateKey();
	}

	private static byte[] encryptData(SecretKey secretKey, byte[] data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return cipher.doFinal(data);
	}
}
