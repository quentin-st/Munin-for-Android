package com.chteuchteu.munin.hlpr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.HTTPResponse.BaseResponse;
import com.chteuchteu.munin.obj.HTTPResponse.BitmapResponse;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

public class NetHelper {
	private static final int CONNECTION_TIMEOUT = 6000; // Default = 6000
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
					/*KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					trustStore.load(null, null);*/

					/*CustomSSLFactory sslFactory = new CustomSSLFactory(trustStore, resp);
					sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);*/

					/*SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
					registry.register(new Scheme("https", sslFactory, 443));*/

					//ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

					SSLContext sslContext = SSLContext.getInstance("SSL");

					sslContext.init(null, new javax.net.ssl.TrustManager[] { new TrustAllTrustManagers() }, new java.security.SecureRandom());

					connection = (HttpsURLConnection) url.openConnection();
					((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
				} catch (Exception e) {
					e.printStackTrace();
					connection = (HttpURLConnection) url.openConnection();
					master.setSSL(false);
				}
			} else
				connection = (HttpURLConnection) url.openConnection();

			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setRequestProperty("User-Agent", userAgent);

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
			resp.setResponseCode(responseCode);
			String responseMessage = connection.getResponseMessage();
			resp.setResponseMessage(responseMessage);

			if (connection.getHeaderFields().containsKey("WWW-Authenticate"))
				resp.setAuthenticateHeader(connection.getHeaderField("WWW-Authenticate"));

			if (responseCode == HttpURLConnection.HTTP_OK) {
				if (downloadType == DownloadType.HTML) {
					InputStream in = url.openStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder html = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null)
						html.append(line);

					in.close();
					reader.close();

					((HTMLResponse) resp).setHtml(html.toString());
				} else {
					Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
					((BitmapResponse) resp).setBitmap(bitmap);
				}
			} else {
				if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED && !retried
						&& connection.getHeaderFields().containsKey("WWW-Authenticate")) {
					master.setAuthString(connection.getHeaderField("WWW-Authenticate"));
					return download(downloadType, master, strUrl, userAgent, true);
				}
			}

			resp.end();

			// Get current URL (detect redirection)
			resp.setLastUrl(connection.getURL().toString());

			MuninFoo.logV("grabUrl", "Downloaded " + (connection.getContentLength()/1024) + "kb in " + resp.getExecutionTime() + "ms");
		}
		catch (SocketTimeoutException | ConnectException e) { resp.setTimeout(true); }
		catch (SSLException e) { // SSLPeerUnverifiedException
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
			e.printStackTrace();
			resp.setResponseCode(BaseResponse.UnknownHostExceptionError);
			resp.setResponseMessage(e.getMessage());
		}
		catch (OutOfMemoryError | Exception e) {
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

	public static String getAuthenticationHeader(MuninMaster master, String url) {
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
}
