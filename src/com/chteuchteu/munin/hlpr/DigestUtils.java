package com.chteuchteu.munin.hlpr;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

public class DigestUtils {
	private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	public static String getDigestAuthHeader(MuninServer s, String url) {
		if (s.getAuthType() == AuthType.DIGEST) {
			// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
			String userName = s.getAuthLogin();
			String password = s.getAuthPassword();
			String realmName = "";
			String nonce = "";
			String algorithm = "MD5";
			String opaque = "";
			String qop = "auth";
			String nc = "00000001";
			String cnonce = "";
			String uri = url;
			String methodName = "GET";
			
			cnonce = DigestUtils.newCnonce();
			
			// Parser le header
			realmName = DigestUtils.match(s.getAuthString(), "realm");
			nonce = DigestUtils.match(s.getAuthString(), "nonce");
			opaque = DigestUtils.match(s.getAuthString(), "opaque");
			qop = DigestUtils.match(s.getAuthString(), "qop");
			
			String a1 = DigestUtils.md5Hex(userName + ":" + realmName + ":" + password);
			String a2 = DigestUtils.md5Hex(methodName + ":" + uri);
			String responseSeed = a1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + a2;
			String response = DigestUtils.md5Hex(responseSeed);
			
			String header = "Digest ";
			header += DigestUtils.formatField("username", userName, false);
			header += DigestUtils.formatField("realm", realmName, false);
			header += DigestUtils.formatField("nonce", nonce, false);
			if (!opaque.equals(""))
				header += DigestUtils.formatField("opaque", opaque, false);
			header += DigestUtils.formatField("uri", uri, false);
			header += DigestUtils.formatField("response", response, false);
			header += DigestUtils.formatField("cnonce", cnonce, false);
			header += DigestUtils.formatField("nc", nc, false);
			if (!qop.equals(""))
				header += DigestUtils.formatField("qop", qop, false);
			header += DigestUtils.formatField("charset", "utf-8", false);
			header += DigestUtils.formatField("algorithm", algorithm, true);
			
			return header;
		}
		
		return "";
	}
	
	public static String match(String headerLine, String token) {
		if (headerLine == null) {
			return "";
		}
		
		int match = headerLine.indexOf(token);
		if (match <= 0) return "";
		
		// = to skip
		match += token.length() + 1;
		int traillingComa = headerLine.indexOf(",", match);
		String value = headerLine.substring(match, traillingComa > 0 ? traillingComa : headerLine.length());
		value = value.endsWith("\"") ? value.substring(0, value.length() - 1) : value;
		return value.startsWith("\"") ? value.substring(1) : value;
	}
	
	public static String toBase16(byte[] bytes) {
		int base = 16;
		StringBuilder buf = new StringBuilder();
		for (byte b : bytes) {
			int bi = 0xff & b;
			int c = '0' + (bi / base) % base;
			if (c > '9')
				c = 'a' + (c - '0' - 10);
			buf.append((char) c);
			c = '0' + bi % base;
			if (c > '9')
				c = 'a' + (c - '0' - 10);
			buf.append((char) c);
		}
		return buf.toString();
	}
	
	public static String newCnonce() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] b = md.digest(String.valueOf(System.currentTimeMillis()).getBytes("ISO-8859-1"));
			return toHexString(b);
		} catch (Exception e) {
			return "";
		}
	}
	
	public static String toHexString(byte[] data) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			buffer.append(Integer.toHexString((data[i] & 0xf0) >>> 4));
			buffer.append(Integer.toHexString(data[i] & 0x0f));
		}
		return buffer.toString();
	}
	
	public static String formatField(String f, String v, boolean last) {
		// f="v",
		String s = "";
		s = s + f + "=\"" + v + "\"";
		if (!last)
			s += ", ";
		return s;
	}
	
	static MessageDigest getDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	protected static String encodeHex(byte[] data) {
		int l = data.length;
		char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = HEX_DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = HEX_DIGITS[0x0F & data[i]];
		}
		return new String(out);
	}
	
	public static byte[] md5(byte[] data) {
		return getDigest("MD5").digest(data);
	}
	
	public static String md5Hex(String data) {
		return encodeHex(md5(utf8Bytes(data)));
	}
	
	private static byte[] utf8Bytes(String data) {
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}