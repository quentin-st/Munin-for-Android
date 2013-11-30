package com.chteuchteu.munin;

import android.graphics.Bitmap;


public class MuninGraph
{
	public static Bitmap bitmap;
	
	private MuninPlugin plugin;
	private MuninServer server;
	
	public MuninGraph (MuninPlugin plugin, MuninServer server) {
		this.plugin = plugin;
		this.server = server;
	}
	
	public String getImgUrl(String period) {
		return this.plugin.getInstalledOn().getGraphURL() + this.plugin.getName() + "-" + period + ".png";
	}
	
	public Bitmap getGraph (String period, MuninServer server) {
		this.server = server;
		//return grabBitmapSameThread(getImgUrl(period));
		return getGraph(getImgUrl(period));
	}
	
	public Bitmap getGraph(String url) {
		//return grabBitmapSameThread(url);
		return MuninFoo.grabBitmap(this.server, url);
	}
	
	/*public Bitmap grabBitmapSameThread(String url) {
		Bitmap bm = null;
		
		try {
			HttpUriRequest request = new HttpGet(url.toString());
			Log.v("", url.toString());
			
			// Création du client
			DefaultHttpClient httpClient;
			if (this.server.getSSL()) {
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
					
					httpClient = new DefaultHttpClient(ccm, params);
				} catch (Exception e) {
					e.printStackTrace();
					httpClient = new DefaultHttpClient();
				}
			} else {
				httpClient = new DefaultHttpClient();
			}
			
			if (this.server.getAuthNeeded())
				httpClient.getCredentialsProvider().setCredentials(
						new AuthScope(null, -1), new UsernamePasswordCredentials(this.server.getAuthLogin(), this.server.getAuthPassword()));
			
			HttpResponse response = httpClient.execute(request);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				byte[] bytes = EntityUtils.toByteArray(entity);
				
				Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
						bytes.length);
				bm = bitmap;
			} else {
				throw new IOException("Download failed, HTTP response code "
						+ statusCode + " - " + statusLine.getReasonPhrase());
			}
		} catch (Exception ex) { ex.printStackTrace(); }
		
		return bm;
	}*/
}