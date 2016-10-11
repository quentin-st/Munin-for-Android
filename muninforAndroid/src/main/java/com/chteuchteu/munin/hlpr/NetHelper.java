package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Pair;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.HTTPResponse.BaseResponse;
import com.chteuchteu.munin.obj.HTTPResponse.BitmapResponse;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetHelper {
	private static final int CONNECTION_TIMEOUT = 8000; // Default = 6000
	private static final int READ_TIMEOUT = 7000; // Default = 6000

	private enum DownloadType { HTML, BITMAP }

	public static HTMLResponse downloadUrl(MuninMaster master, String url, String userAgent) {
		return (HTMLResponse) download(DownloadType.HTML, master, url, userAgent, false);
	}

	public static BitmapResponse downloadBitmap(MuninMaster master, String url, String userAgent) {
		return (BitmapResponse) download(DownloadType.BITMAP, master, url, userAgent, false);
	}

	/**
	 * Downloads:
	 * 		* the bitmap representation of the image
	 * 		* the HTML content
	 * 	of the target page (strUrl), depending on the downloadType.
	 * @param downloadType HTML/Bitmap
	 * @param master MuninMaster
	 * @param strUrl Target URL
	 * @param userAgent UserAgent to be sent to the server
	 * @param retried Retry after getting digest auth information / SSL fail (recursive call)
	 * @return BaseResponse
	 */
	private static BaseResponse download(DownloadType downloadType, MuninMaster master, String strUrl, String userAgent, boolean retried) {
		BaseResponse resp;
		if (downloadType == DownloadType.HTML)
			resp = new HTMLResponse(strUrl);
		else
			resp = new BitmapResponse(strUrl);

		MuninFoo.logV("grabUrl:url", strUrl);

		HttpURLConnection connection = null;

		try {
			URL url = new URL(strUrl);

			if (master.getSSL()) {
				try {
					// Trust all certificates and hosts
					// Create a trust manager that does not validate certificate chains
					TrustManager[] trustAllCerts = new TrustManager[] {
							new X509TrustManager() {
								public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
								@SuppressLint("TrustAllX509TrustManager")
								public void checkClientTrusted(X509Certificate[] certs, String authType) { }
								@SuppressLint("TrustAllX509TrustManager")
								public void checkServerTrusted(X509Certificate[] certs, String authType) { }
							}
					};

					// Install the all-trusting trust manager
					SSLContext sslContext = SSLContext.getInstance("SSL");
					sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

					// Create all-trusting host name verifier
					HostnameVerifier allHostsValid = new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) { return true; }
					};

					connection = (HttpsURLConnection) url.openConnection();
					((HttpsURLConnection) connection).setHostnameVerifier(allHostsValid);
					((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
				} catch (Exception e) {
					e.printStackTrace();
					connection = (HttpURLConnection) url.openConnection();
					master.setSSL(false);
				}
			} else
				connection = (HttpURLConnection) url.openConnection();

			// Set connection timeout & user agent
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setRequestProperty("User-Agent", userAgent);
			connection.setRequestProperty("Accept", "*/*");

			// Apache Basic/Digest auth
			if (master.isAuthNeeded()) {
				String header = getAuthenticationHeader(master, strUrl);
				if (header != null)
					connection.setRequestProperty("Authorization", header);
			}

			resp.begin();
			connection.connect();

			// Read response headers
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();

			resp.setResponseCode(responseCode);
			resp.setResponseMessage(responseMessage);

			if (connection.getHeaderFields().containsKey("WWW-Authenticate"))
				resp.setAuthenticateHeader(connection.getHeaderField("WWW-Authenticate"));

			if (BuildConfig.DEBUG)
				MuninFoo.log(responseCode + " - " + responseMessage);

			switch (responseCode) {
				case HttpURLConnection.HTTP_UNAUTHORIZED:
					if (connection.getHeaderFields().containsKey("WWW-Authenticate"))
						master.setAuthString(connection.getHeaderField("WWW-Authenticate"));

					if (master.isAuthNeeded() && !retried)
						return download(downloadType, master, strUrl, userAgent, true);
					else if (!master.isAuthNeeded()) // Unauthorized & no auth information: abort
						return resp;
					break;
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
				case HttpURLConnection.HTTP_SEE_OTHER:
				case 307: // Temporary Redirect, but keep POST data
				case 308: // Permanent Redirect, but keep POST data
					// That's a redirection
					String newUrl = connection.getHeaderField("Location");
					return download(downloadType, master, newUrl, userAgent, true);
				default:
					if (downloadType == DownloadType.HTML) {
						InputStream in = responseCode == 200 ? connection.getInputStream() : connection.getErrorStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(in));
						StringBuilder html = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null)
							html.append(line);

						in.close();
						reader.close();

						((HTMLResponse) resp).setHtml(html.toString());
					} else {
						InputStream in = responseCode == 200 ? connection.getInputStream() : connection.getErrorStream();

						Bitmap bitmap = BitmapFactory.decodeStream(in);
						((BitmapResponse) resp).setBitmap(bitmap);

						if (in != null)
							in.close();
					}
					break;
			}

			resp.end();

			// Get current URL (detect redirection)
			resp.setLastUrl(connection.getURL().toString());

			MuninFoo.logV("grabUrl", "Downloaded " + (connection.getContentLength()/1024) + "kb in " + resp.getExecutionTime() + "ms");
		}
		catch (SocketTimeoutException | ConnectException e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setTimeout(true);
		}
		catch (SSLException e) { // SSLPeerUnverifiedException
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			if (!master.getSSL()) {
				master.setSSL(true);
				// Update the URL of master / child node if needed
				if (master.getUrl().equals(strUrl))
					master.setUrl(Util.URLManipulation.setHttps(strUrl));
				else {
					for (MuninNode node : master.getChildren()) {
						if (node.getUrl().equals(strUrl)) {
							node.setUrl(Util.URLManipulation.setHttps(strUrl));
							break;
						}
					}
				}

				strUrl = Util.URLManipulation.setHttps(strUrl);
			}

			if (!retried)
				return download(downloadType, master, strUrl, userAgent, true);
		}
		catch (UnknownHostException e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setResponseCode(BaseResponse.UnknownHostExceptionError);
			resp.setResponseMessage(e.getMessage());
		}
		catch (OutOfMemoryError | Exception e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setResponseCode(BaseResponse.UnknownError);
			resp.setResponseMessage("Unknown error");
		}
		finally {
			if (connection != null)
				connection.disconnect();
		}

		return resp;
	}

	private static String getAuthenticationHeader(MuninMaster master, String url) {
		if (!master.isAuthNeeded())
			return null;

		switch (master.getAuthType()) {
			case BASIC:
				return "Basic " + Base64.encodeToString((master.getAuthLogin() + ":" + master.getAuthPassword()).getBytes(), Base64.NO_WRAP);
			case DIGEST:
				// Digest foo:digestedPass, realm="munin", nonce="+RdhgM7qBAA=86e58ecf5cbd672ba8246c4f9eed4a389fe87fd6", algorithm=MD5, qop="auth"
				// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
				return DigestUtils.getDigestAuthHeader(master, url);
		}
		return null;
	}

	public static HTMLResponse simplePost(String strUrl, List<Pair<String, String>> params, String userAgent) {
		HTMLResponse resp = new HTMLResponse(strUrl);

		MuninFoo.logV("grabUrl:url", strUrl);

		HttpURLConnection connection = null;

		try {
			URL url = new URL(strUrl);

			connection = (HttpURLConnection) url.openConnection();

			// Set connection timeout & user agent
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setRequestProperty("User-Agent", userAgent);
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Write POST
			OutputStream os = connection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(getQuery(params));
			writer.flush();
			writer.close();

			resp.begin();
			connection.connect();

			// Read response headers
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();

			resp.setResponseCode(responseCode);
			resp.setResponseMessage(responseMessage);

			if (BuildConfig.DEBUG)
				MuninFoo.log(responseCode + " - " + responseMessage);

			// Handle redirects
			switch (responseCode) {
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
				case HttpURLConnection.HTTP_SEE_OTHER:
				case 307: // Temporary Redirect, but keep POST data
				case 308: // Permanent Redirect, but keep POST data
					String newUrl = connection.getHeaderField("Location");
					return simplePost(newUrl, params, userAgent);
				default:
					// Read response
					InputStream in = responseCode == 200 ? connection.getInputStream() : connection.getErrorStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder html = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null)
						html.append(line);

					in.close();
					reader.close();

					resp.setHtml(html.toString());
			}

			resp.end();

			// Get current URL (detect redirection)
			resp.setLastUrl(connection.getURL().toString());

			MuninFoo.logV("grabUrl", "Downloaded " + (connection.getContentLength()/1024) + "kb in " + resp.getExecutionTime() + "ms");
		}
		catch (SocketTimeoutException | ConnectException e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setTimeout(true);
		}
		catch (UnknownHostException e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setResponseCode(BaseResponse.UnknownHostExceptionError);
			resp.setResponseMessage(e.getMessage());
		}
		catch (Exception e) {
			if (BuildConfig.DEBUG)
				e.printStackTrace();
			resp.setResponseCode(BaseResponse.UnknownError);
			resp.setResponseMessage("Unknown error");
		}
		finally {
			if (connection != null)
				connection.disconnect();
		}

		return resp;
	}

	private static String getQuery(List<Pair<String, String>> params) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;

		for (Pair<String, String> param : params) {
			if (first)
				first = false;
			else
				builder.append("&");

			builder.append(URLEncoder.encode(param.first, "UTF-8"));
			builder.append("=");
			builder.append(URLEncoder.encode(param.second, "UTF-8"));
		}

		return builder.toString();
	}
}
