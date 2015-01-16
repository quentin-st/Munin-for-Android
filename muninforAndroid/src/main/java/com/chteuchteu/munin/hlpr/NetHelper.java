package com.chteuchteu.munin.hlpr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.CustomSSLFactory;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.HTTPResponse;
import com.chteuchteu.munin.obj.HTTPResponse_Bitmap;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ssl.SSLException;

public class NetHelper {
	private static final int CONNECTION_TIMEOUT = 6000; // Default = 6000
	private static final int SOCKET_TIMEOUT = 7000; // Default = 6000

	public static HTTPResponse grabUrl(MuninMaster master, String url, String userAgent) {
		return grabUrl(master, url, userAgent, false);
	}

	/**
	 * Downloads body response of a HTTP(s) request using master auth information
	 * @param master Needed for SSL/Apache basic/digest auth
	 * @param url URL to be downloaded
	 * @return HTTPResponse
	 */
	public static HTTPResponse grabUrl(MuninMaster master, String url, String userAgent, boolean retried) {
		HTTPResponse resp = new HTTPResponse();
		
		MuninFoo.logV("grabUrl:url", url);
		
		try {
			HttpClient client;
			if (master.getSSL()) {
				try {
					KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					trustStore.load(null, null);
					
					CustomSSLFactory sf = new CustomSSLFactory(trustStore, resp);
					sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
					
					HttpParams params = new BasicHttpParams();
					HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
					HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
					HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
					HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
					
					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
					registry.register(new Scheme("https", sf, 443));
					
					ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
					
					client = new DefaultHttpClient(ccm, params);
				} catch (Exception e) {
					e.printStackTrace();
					client = new DefaultHttpClient();
					master.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();

			HttpGet request = new HttpGet(url);
			request.setHeader("User-Agent", userAgent);
			
			if (master.isAuthNeeded()) {
				if (master.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString(
							(master.getAuthLogin() + ":" + master.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (master.getAuthType() == AuthType.DIGEST) {
					// Digest foo:digestedPass, realm="munin", nonce="+RdhgM7qBAA=86e58ecf5cbd672ba8246c4f9eed4a389fe87fd6", algorithm=MD5, qop="auth"
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String header = DigestUtils.getDigestAuthHeader(master, url);
					request.setHeader("Authorization", header);
				}
			}
			
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIMEOUT);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);
			((DefaultHttpClient) client).setParams(httpParameters);

			resp.begin();
			
			HttpResponse response = client.execute(request);
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				str.append(line);
			}
			in.close();
			
			resp.setHtml(str.toString());
			resp.setResponsePhrase(response.getStatusLine().getReasonPhrase());
			resp.setResponseCode(response.getStatusLine().getStatusCode());
			if (response.getHeaders("WWW-Authenticate").length > 0)
				resp.setAuthenticateHeader(response.getHeaders("WWW-Authenticate")[0].getValue());

			resp.end();
			if (BuildConfig.DEBUG)
				MuninFoo.logV("grabUrl", "Downloaded " + (response.getEntity().getContentLength()/1024) + "kb in " + resp.getExecutionTime() + "ms");
		}
		catch (SocketTimeoutException | ConnectTimeoutException e) { resp.setTimeout(true); }
		catch (SSLException e) { // SSLPeerUnverifiedException
			e.printStackTrace();

			if (!master.getSSL()) {
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
			}

			if (!retried)
				return NetHelper.grabUrl(master, url, userAgent, true);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			resp.setResponseCode(HTTPResponse.UnknownHostExceptionError);
			resp.setResponsePhrase(e.getMessage());
		}
		catch (Exception e) { e.printStackTrace(); }
		return resp;
	}
	
	public static HTTPResponse_Bitmap grabBitmap(MuninMaster master, String url, String userAgent) {
		return grabBitmap(master, url, userAgent, false);
	}

	/**
	 * Get a bitmap representation of the image targeted by url parameter,
	 *  using master auth information
	 * @param master Needed for SSL/Apache basic/digest auth
	 * @param url URL of the image
	 * @param retried Retry after getting digest auth information
	 *                (recursive call)
	 * @return Bitmap
	 */
	private static HTTPResponse_Bitmap grabBitmap(MuninMaster master, String url, String userAgent, boolean retried) {
		HTTPResponse_Bitmap respObj = new HTTPResponse_Bitmap();

		MuninFoo.logV("grabBitmap:url", url);

		try {
			HttpClient client;
			if (master.getSSL()) {
				try {
					KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					trustStore.load(null, null);

					CustomSSLFactory sf = new CustomSSLFactory(trustStore, respObj);
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
			request.setHeader("User-Agent", userAgent);
			
			if (master.isAuthNeeded()) {
				if (master.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString(
							(master.getAuthLogin() + ":" + master.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (master.getAuthType() == AuthType.DIGEST) {
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String header = DigestUtils.getDigestAuthHeader(master, url);
					request.setHeader("Authorization", header);
				}
			}
			
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, 7000);
			((DefaultHttpClient) client).setParams(httpParameters);

			respObj.begin();
			
			HttpResponse response = client.execute(request);
			StatusLine statusLine = response.getStatusLine();

            respObj.setResponseCode(statusLine.getStatusCode());
            respObj.setResponsePhrase(statusLine.getReasonPhrase());
			if (respObj.requestSucceeded()) {
				Bitmap bitmap = BitmapFactory.decodeStream(response.getEntity().getContent());
				respObj.setBitmap(bitmap);
			} else {
				if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED && !retried && response.getHeaders("WWW-Authenticate").length > 0) {
					master.setAuthString(response.getHeaders("WWW-Authenticate")[0].getValue());
					return grabBitmap(master, url, userAgent, true);
				} else
					respObj.setBitmap(null);
			}

			respObj.end();
			if (BuildConfig.DEBUG)
				MuninFoo.logV("grabBitmap", "Downloaded bitmap in " + respObj.getExecutionTime() + "ms");
		}
		catch (SocketTimeoutException | ConnectTimeoutException e) {
			e.printStackTrace();
			respObj.setResponseCode(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
			respObj.setResponsePhrase("Timeout");
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			respObj.setResponseCode(HTTPResponse.UnknownHostExceptionError);
			respObj.setResponsePhrase(e.getMessage());
		}
		catch (OutOfMemoryError | Exception e) {
			e.printStackTrace();
			respObj.setResponseCode(HTTPResponse.UnknownError);
			respObj.setResponsePhrase("Unknown error");
		}
		
		return respObj;
	}
}