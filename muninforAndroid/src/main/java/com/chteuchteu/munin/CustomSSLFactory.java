package com.chteuchteu.munin;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import com.chteuchteu.munin.obj.HTTPResponse;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CustomSSLFactory extends SSLSocketFactory {
	private TrustManager trustManager;
	private HTTPResponse httpResponse;
	
	public CustomSSLFactory(KeyStore truststore, HTTPResponse httpResponse)
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
		super(truststore);
		
		this.trustManager = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
			
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
			
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		this.httpResponse = httpResponse;
	}
	
	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
		if (autoClose) // we don't need the plainSocket
			socket.close();

		// Create and connect SSL socket, but don't do hostname/certificate verification yet
		SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
		sslSocketFactory.setTrustManagers(new TrustManager[] { trustManager });
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

		// Verify hostname and certificate, but don't throw exception
		SSLSession session = ssl.getSession();
		HostnameVerifier hostnameVerifier = new BrowserCompatHostnameVerifier();
		if (hostnameVerifier.verify(host, session))
			httpResponse.setConnectionType(HTTPResponse.ConnectionType.SECURE);
		else
			httpResponse.setConnectionType(HTTPResponse.ConnectionType.INSECURE);

		return ssl;
	}
}