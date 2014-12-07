package com.chteuchteu.munin;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CustomSSLFactory extends SSLSocketFactory {
	private SSLContext sslContext = SSLContext.getInstance("TLS");
	
	public CustomSSLFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		super(truststore);
		
		TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
			
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
			
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		
		sslContext.init(null, new TrustManager[] { tm }, null);
	}
	
	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
		//return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);

		if (autoClose) // we don't need the plainSocket
			socket.close();

		// Create and connect SSL socket, but don't do hostname/certificate verification yet
		SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
		SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		// enable TLSv1.1/1.2 if available
		// (see https://github.com/rfc2822/davdroid/issues/229)
		ssl.setEnabledProtocols(ssl.getSupportedProtocols());

		// set up SNI before the handshake
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			sslSocketFactory.setHostname(ssl, host);
		} else {
			// No documented SNI support on Android <4.2, trying with reflection
			try {
				java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
				setHostnameMethod.invoke(ssl, host);
			} catch (Exception ex) {
				// SNI not useable
				ex.printStackTrace();
			}
		}

		// We don't want to verify hostname and certificates actually
		// verify hostname and certificate
		//SSLSession session = ssl.getSession();
		//if (!hostnameVerifier.verify(host, session))
		//	throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);

		return ssl;
	}
	
	@Override
	public Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}
}