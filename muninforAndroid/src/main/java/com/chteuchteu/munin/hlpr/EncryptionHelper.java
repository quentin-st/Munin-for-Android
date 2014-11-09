package com.chteuchteu.munin.hlpr;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class EncryptionHelper {
	private String charsetName = "UTF8";
	private String algorithm = "DES";
	private int base64Mode = Base64.DEFAULT;

	public String getCharsetName() {
		return charsetName;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public int getBase64Mode() {
		return base64Mode;
	}

	public void setBase64Mode(int base64Mode) {
		this.base64Mode = base64Mode;
	}

	public String encrypt(String key, String data) {
		if (key == null || data == null)
			return null;
		try {
			DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(charsetName));
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
			SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
			byte[] dataBytes = data.getBytes(charsetName);
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.encodeToString(cipher.doFinal(dataBytes), base64Mode);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String decrypt(String key, String data) {
		if (key == null || data == null)
			return null;
		try {
			byte[] dataBytes = Base64.decode(data, base64Mode);
			DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(charsetName));
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
			SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] dataBytesDecrypted = (cipher.doFinal(dataBytes));
			return new String(dataBytesDecrypted);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}