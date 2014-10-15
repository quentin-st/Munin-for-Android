package com.chteuchteu.munin.hlpr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.security.KeyStore;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.chteuchteu.munin.CustomSSLFactory;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.HTTPResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

public class NetHelper {
	/**
	 * Downloads body response of a HTTP(s) request
	 * @param master Needed for SSL/Apache basic/digest auth
	 * @param url URL to be downloaded
	 * @return
	 */
	public static HTTPResponse grabUrl(MuninMaster master, String url) {
		HTTPResponse resp = new HTTPResponse("", -1);
		
		if (MuninFoo.DEBUG)
			Log.v("grabUrl:url", url);
		
		try {
			HttpClient client = null;
			if (master.getSSL()) {
				try {
					KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					trustStore.load(null, null);
					
					CustomSSLFactory sf = new CustomSSLFactory(trustStore);
					sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
					
					HttpParams params = new BasicHttpParams();
					HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
					HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
					HttpConnectionParams.setConnectionTimeout(params, 5000);
					HttpConnectionParams.setSoTimeout(params, 7000);
					
					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
					registry.register(new Scheme("https", sf, 443));
					
					ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
					
					client = new DefaultHttpClient(ccm, params);
				} catch (Exception e) {
					client = new DefaultHttpClient();
					master.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			if (master.isAuthNeeded()) {
				if (master.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString(
							(master.getAuthLogin() + ":" + master.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (master.getAuthType() == AuthType.DIGEST) {
					// Digest foo:digestedPass, realm="munin", nonce="+RdhgM7qBAA=86e58ecf5cbd672ba8246c4f9eed4a389fe87fd6", algorithm=MD5, qop="auth"
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String userName = master.getAuthLogin();
					String password = master.getAuthPassword();
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
					
					// Parse header
					realmName = DigestUtils.match(master.getAuthString(), "realm");
					nonce = DigestUtils.match(master.getAuthString(), "nonce");
					opaque = DigestUtils.match(master.getAuthString(), "opaque");
					qop = DigestUtils.match(master.getAuthString(), "qop");
					
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
					
					request.setHeader("Authorization", header);
				}
			}
			
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, 7000);
			((DefaultHttpClient) client).setParams(httpParameters);
			
			HttpResponse response = client.execute(request);
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null) {
				str.append(line);
			}
			in.close();
			
			resp.html = str.toString();
			resp.responseReason = response.getStatusLine().getReasonPhrase();
			resp.responseCode = response.getStatusLine().getStatusCode();
			if (response.getHeaders("WWW-Authenticate").length > 0)
				resp.header_wwwauthenticate = response.getHeaders("WWW-Authenticate")[0].getValue();
		}
		catch (SocketTimeoutException e) { e.printStackTrace(); resp.timeout = true; }
		catch (ConnectTimeoutException e) { e.printStackTrace(); resp.timeout = true; }
		catch (SSLPeerUnverifiedException e) {
			master.setSSL(true);
			// Update the URL of master / child server if needed
			if (master.getUrl().equals(url))
				master.setUrl(Util.URLManipulation.setHttps(url));
			else {
				for (MuninServer server : master.getChildren()) {
					if (server.getServerUrl().equals(url)) {
						server.setServerUrl(Util.URLManipulation.setHttps(url));
						break;
					}
				}
			}
			
			url = Util.URLManipulation.setHttps(url);
			return NetHelper.grabUrl(master, url);
		}
		catch (SSLException e) {
			master.setSSL(true);
			// Update the URL of master / child server if needed
			if (master.getUrl().equals(url))
				master.setUrl(Util.URLManipulation.setHttps(url));
			else {
				for (MuninServer server : master.getChildren()) {
					if (server.getServerUrl().equals(url)) {
						server.setServerUrl(Util.URLManipulation.setHttps(url));
						break;
					}
				}
			}
			
			url = Util.URLManipulation.setHttps(url);
			return NetHelper.grabUrl(master, url);
		}
		catch (Exception e) { e.printStackTrace(); resp.html = ""; }
		return resp;
	}
	
	public static Bitmap grabBitmap(MuninMaster master, String url) {
		return grabBitmap(master, url, false);
	}
	
	public static Bitmap grabBitmap(MuninMaster master, String url, boolean retried) {
		Bitmap b = null;
		
		try {
			HttpClient client = null;
			if (master.getSSL()) {
				try {
					KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					trustStore.load(null, null);
					
					CustomSSLFactory sf = new CustomSSLFactory(trustStore);
					sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
					
					HttpParams params = new BasicHttpParams();
					HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
					HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
					
					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
					registry.register(new Scheme("https", sf, 443));
					
					ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
					
					client = new DefaultHttpClient(ccm, params);
				} catch (Exception e) {
					client = new DefaultHttpClient();
					master.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			if (master.isAuthNeeded()) {
				if (master.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString(
							(master.getAuthLogin() + ":" + master.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (master.getAuthType() == AuthType.DIGEST) {
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String userName = master.getAuthLogin();
					String password = master.getAuthPassword();
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
					// Parse header
					realmName = DigestUtils.match(master.getAuthString(), "realm");
					nonce = DigestUtils.match(master.getAuthString(), "nonce");
					opaque = DigestUtils.match(master.getAuthString(), "opaque");
					qop = DigestUtils.match(master.getAuthString(), "qop");
					
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
					
					request.setHeader("Authorization", header);
				}
			}
			
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, 7000);
			((DefaultHttpClient) client).setParams(httpParameters);
			
			HttpResponse response = client.execute(request);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == HttpURLConnection.HTTP_OK) {
				HttpEntity entity = response.getEntity();
				byte[] bytes = EntityUtils.toByteArray(entity);
				b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} else {
				if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED && !retried && response.getHeaders("WWW-Authenticate").length > 0) {
					master.setAuthString(response.getHeaders("WWW-Authenticate")[0].getValue());
					return grabBitmap(master, url, true);
				} else
					throw new IOException("Download failed for URL " + url + " HTTP response code "
							+ statusCode + " - " + statusLine.getReasonPhrase());
			}
		}
		catch (SocketTimeoutException e) { e.printStackTrace(); return null; }
		catch (ConnectTimeoutException e) { e.printStackTrace(); return null; }
		catch (OutOfMemoryError e) { e.printStackTrace(); return null; }
		catch (Exception e) { e.printStackTrace(); return null; }
		
		return b;
	}
}