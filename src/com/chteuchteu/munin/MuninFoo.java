package com.chteuchteu.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;


public class MuninFoo {
	public static MuninFoo instance;
	
	private List<MuninServer> servers;
	public List<MuninLabel> labels;
	public List<MuninLabelRelation> labels_relations;
	
	public SQLite			sqlite;
	public MuninServer 		currentServer;
	public boolean			drawer;
	
	// === VERSION === //
	// HISTORY		current:	 ___________________________________________________________________________________________________________________________---_
	// android:versionName:		| 1.1		1.2		1.3		1.4		1.4.1	1.4.2	1.4.5	1.4.6	2.0		2.0.1	2.1		2.2		2.3		2.4		2.5		2.6 |
	// android:versionCode: 	|  1		 2		 3		 4		 5		 6		 7	 	 8	  	 10		11		12		13		14		15		16		17	|
	// MfA version:				| 1.1		1.2		1.3		1.4		1.5		1.6		1.7  	1.8   	1.9		2.0		2.1 	2.2		2.3		2.4		2.5		2.6	|
	
	
	public double version = 2.6;
	// =============== //
	public boolean debug = true;
	public boolean premium;
	
	private MuninFoo() {
		premium = false;
		drawer = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<MuninLabel>();
		sqlite = new SQLite(this);
		instance = null;
		loadInstance();
	}
	
	MuninFoo(Context c) {
		premium = false;
		drawer = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<MuninLabel>();
		sqlite = new SQLite(this);
		instance = null;
		loadInstance(c);
	}
	
	public void loadInstance() {
		for (MuninServer s : sqlite.getServers()) {
			MuninServer server = s;
			server.setPluginsList(sqlite.getPlugins(s));
			this.addServer(server);
		}
		
		sqlite.fetchMuninLabels();
		
		if (servers.size() > 0)
			currentServer = getServerFromFlatPosition(0);
		else
			currentServer = null;
		
		if (debug)
			sqlite.logServersTable();
	}
	
	public void loadInstance(Context c) {
		loadInstance();
		String tmps = "";
		if (c==null)
			tmps = "null";
		else
			tmps = "not null";
		Log.v("", "instance : " + tmps);
		if (c != null) {
			this.premium = isPremium(c);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !getPref("drawer", c).equals("false"))
				this.drawer = true;
			else
				this.drawer = false;
		}
		if (drawer)
			tmps = "true";
		else
			tmps = "false";
		Log.v("", "drawer : " + tmps);
	}
	
	public void resetInstance(Context c) {
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<MuninLabel>();
		sqlite = new SQLite(this);
		loadInstance(c);
	}
	
	public static synchronized MuninFoo getInstance() {
		if (instance == null)
			instance = new MuninFoo();
		return instance;
	}
	
	public static synchronized MuninFoo getInstance(Context c) {
		Log.v("", "getInstance(c)");
		if (instance == null)
			instance = new MuninFoo(c);
		else
			Log.v("", "instance was not null");
		return instance;
	}
	
	public void loadLanguage(Context c) {
		if (!getPref("lang", c).equals("")) {
			String lang = getPref("lang", c);
			// lang == "en" || "fr" || "de"
			if (!(lang.equals("en") || lang.equals("fr") || lang.equals("de"))) {
				lang = "en";
			}
			Resources res = c.getApplicationContext().getResources();
			DisplayMetrics dm = res.getDisplayMetrics();
			Configuration conf = res.getConfiguration();
			conf.locale = new Locale(lang);
			res.updateConfiguration(conf, dm);
		}
		// else: lang set according to device locale
	}
	
	
	
	public void addServer(MuninServer server) {
		boolean contains = false;
		int pos = -1;
		for (int i=0; i<getHowManyServers(); i++) {
			if (servers.get(i) != null && servers.get(i).equalsApprox(server)) {
				contains = true; pos = i; break;
			}
		}
		if (contains) // Remplacement
			servers.set(pos, server);
		else
			servers.add(server);
	}
	public void updateServer(String oldServerAddress, MuninServer newServer) {
		for (int i=0; i<servers.size(); i++) {
			if (servers.get(i) != null && servers.get(i).equalsApprox(oldServerAddress))
				servers.set(i, newServer);
		}
	}
	public void deleteServer(MuninServer s) {
		deleteServer(s.getServerUrl());
	}
	public boolean addLabel(MuninLabel l) {
		boolean contains = false;
		for (MuninLabel ml : labels) {
			if (ml.getName().equals(l.getName())) {
				contains = true; break;
			}
		}
		if (!contains)
			labels.add(l);
		return !contains;
	}
	public boolean removeLabel(MuninLabel label) {
		List<MuninLabel> list = new ArrayList<MuninLabel>();
		boolean someThingDeleted = false;
		for (MuninLabel l : labels) {
			if (!l.equals(label))
				list.add(l);
			else
				someThingDeleted = true;
		}
		labels = list;
		return someThingDeleted;
	}
	public void unLinkAll() {
		// Permet d'éviter qu'une récupération de la BDD entraîne une modif locale
		List<MuninServer> newServers = new ArrayList<MuninServer>();
		for (MuninServer s : this.servers) {
			MuninServer serv = new MuninServer();
			serv.importData(s);
			newServers.add(serv);
		}
		this.servers = newServers;
	}
	public void deleteServer(String serverAddress) {
		// Vérifie si un serveur correspond à cette adresse
		for (int i=0; i<servers.size(); i++) {
			if (servers.get(i) != null && servers.get(i).equalsApprox(serverAddress))
				servers.remove(i);
		}
		if (getHowManyServers() == 0)
			currentServer = null;
		else
			currentServer = servers.get(0);
	}
	public void deleteServer(int pos) {
		if (pos >= 0 && pos < servers.size()) {
			servers.remove(pos);
		}
	}
	
	public int getHowManyServers() {
		return servers.size();
	}
	public List<MuninServer> getServers() {
		return this.servers;
	}
	public MuninServer getServer(int pos) {
		if (pos >= 0 && pos < servers.size())
			return servers.get(pos);
		else
			return null;
	}
	public MuninServer getServerFromPosition(int position) {
		for (MuninServer s : servers) {
			if (s != null && s.getPosition() == position)
				return s;
		}
		return null;
	}
	public MuninServer getServerFromFlatPosition(int position) {
		// si pos -> 0 1 4 8 9 11
		// gSFFP(2) -> 4 (!= null)
		if (position >= 0 && position < getOrderedServers().size())
			return getOrderedServers().get(position);
		return null;
	}
	public List<MuninServer> getOrderedServers() {
		List<MuninServer> l = new ArrayList<MuninServer>();
		int pos = 0;
		int remainingServers = getHowManyServers();
		
		int maxPos = 0;
		for (MuninServer s : getServers()) {
			if (s.getPosition() > maxPos)
				maxPos = s.getPosition();
		}
		
		while(remainingServers > 0 && pos <= maxPos) {
			if (getServerFromPosition(pos) != null) {
				l.add(getServerFromPosition(pos));
				remainingServers--;
			}
			pos++;
		}
		
		return l;
	}
	public List<MuninServer> getServersFromPlugin(MuninPlugin pl) {
		List<MuninServer> l = new ArrayList<MuninServer>();
		for (MuninServer s : getOrderedServers()) {
			for (MuninPlugin p : s.getPlugins()) {
				if (p.equalsApprox(pl)) {
					l.add(s); break;
				}
			}
		}
		return l;
	}
	public MuninServer getServer(String url) {
		if (!url.equals("")) {
			for (MuninServer s : servers) {
				if (s.getServerUrl().equals(url))
					return s;
			}
		}
		return null;
	}
	/*public MuninServer getServerByName(String name) {<<<<<<< HEAD
		if (!name.equals("")) {
			for (MuninServer s : servers) {
				if (s.getName().equals(name))
					return s;
			}
		}
		return null;
	}*/
	
	public int getServerRange(MuninServer server) {
		for (int i=0; i<getHowManyServers(); i++) {
			if (servers.get(i).getServerUrl().equals(server.getServerUrl()))
				return i;
		}
		return 0;
	}
	
	public int getServerFlatRange(MuninServer s) {
		for (int i=0; i<getHowManyServers(); i++) {
			if (getServerFromFlatPosition(i).getServerUrl().equals(s.getServerUrl()))
				return i;
		}
		return 0;
	}
	
	public boolean containsLabel(String lname) {
		for (MuninLabel l : labels) {
			if (l.getName().equals(lname))
				return true;
		}
		return false;
	}
	
	public MuninLabel getLabel(String lname) {
		for (MuninLabel l : labels) {
			if (l.getName().equals(lname))
				return l;
		}
		return null;
	}
	
	public int fetchServersList(MuninServer server) {
		int nbServers = 0;
		List<MuninServer> mp = new ArrayList<MuninServer>();
		//MuninServer[] mp = new MuninServer[300];
		// Graph html content
		String html = "";
		try {
			html = grabUrl(server).html;
		} catch (Exception e) { }
		
		if (html != null && html != "" && html != "error") {
			MuninServer currentServ;
			Document doc = Jsoup.parse(html, server.getServerUrl());
			
			Elements hosts = doc.select("span.host");
			
			nbServers = 0;
			
			for (Element host : hosts) {
				currentServ = new MuninServer(host.child(0).text(), host.child(0).attr("abs:href"));
				if (server.isAuthNeeded()) {
					currentServ.setAuthIds(server.getAuthLogin(), server.getAuthPassword(), server.getAuthType());
					currentServ.setAuthType(server.getAuthType());
					currentServ.setAuthString(server.getAuthString());
				}
				currentServ.setSSL(server.getSSL());
				
				mp.add(currentServ);
				//mp[nbServers] = currentServ;
				addServer(currentServ);
				nbServers++;
			}
		}
		return nbServers;
	}
	
	public HTTPResponse grabUrl(String url) {
		return grabUrl(new MuninServer("", url));
	}
	public HTTPResponse grabUrl(MuninServer s) {
		HTTPResponse resp = new HTTPResponse("", -1);
		try {
			// Création du HTTP Client
			HttpClient client = null;
			if (s.getSSL()) {
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
					s.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(s.getServerUrl());
			
			if (s.isAuthNeeded()) {
				if (s.getAuthType() == MuninServer.AUTH_BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString((s.getAuthLogin() + ":" + s.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (s.getAuthType() == MuninServer.AUTH_DIGEST) {
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
					String uri = s.getServerUrl();
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
					
					request.setHeader("Authorization", header);
				}
			}
			
			// Digest foo:digestedPass, realm="munin", nonce="+RdhgM7qBAA=86e58ecf5cbd672ba8246c4f9eed4a389fe87fd6", algorithm=MD5, qop="auth"
			
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
			if (response.getHeaders("WWW-Authenticate").length > 0) {
				resp.header_wwwauthenticate = response.getHeaders("WWW-Authenticate")[0].getValue();
			}
		}
		catch (SocketTimeoutException e) { resp.timeout = true; }
		catch (Exception e) { resp.html = ""; }
		return resp;
	}
	
	public static Bitmap grabBitmap(MuninServer s, String url) {
		Bitmap b = null;
		try {
			// Création du HTTP Client
			HttpClient client = null;
			if (s.getSSL()) {
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
					s.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			if (s.isAuthNeeded()) {
				if (s.getAuthType() == MuninServer.AUTH_BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString((s.getAuthLogin() + ":" + s.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (s.getAuthType() == MuninServer.AUTH_DIGEST) {
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
					
					Log.v("authstring", "= " + s.getAuthString());
					
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
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				byte[] bytes = EntityUtils.toByteArray(entity);
				b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} else {
				throw new IOException("Download failed, HTTP response code "
						+ statusCode + " - " + statusLine.getReasonPhrase());
			}
		}
		catch (SocketTimeoutException e) { return null; }
		catch (ConnectTimeoutException e) { return null; }
		catch (Exception e) { return null; }
		
		return b;
	}
	
	/*public String grabUrl(MuninServer server) {
		String html = "";
		try {
			// Création du HTTP Client
			HttpClient client = null;
			if (server.getSSL()) {
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
				}
			} else {
				client = new DefaultHttpClient();
			}
			
			HttpGet request = new HttpGet(server.getServerUrl());
			if (server.getAuthNeeded())
				request.setHeader("Authorization", "Basic " + Base64.encodeToString((server.getAuthLogin() + ":" + server.getAuthPassword()).getBytes(), Base64.NO_WRAP));
			
			HttpResponse response = client.execute(request);
			
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null) {
				str.append(line);
			}
			in.close();
			
			html = str.toString();
		} catch (Exception e) { html = "error"; }
		return html;
	}*/
	
	/*public String grabUrl(String url) {
		html_source = "";
		grabUrl check = new grabUrl();
		check.execute(url);
		int timeout = 0;
		while (html_source == "") {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) { e.printStackTrace(); }
			timeout++;
			if (timeout > 40)
				break;
		}
		return html_source;
	}
	
	public class grabUrl extends AsyncTask<String, Void, Void> {
		@TargetApi(8)
		@Override
		protected Void doInBackground(String... url) {
			//String source = "";
			String html = "";
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(url[0]);
				//if (authNeeded)
				//request.setHeader("Authorization", "Basic " + Base64.encodeToString((authLogin + ":" + authPassword).getBytes(), Base64.NO_WRAP));
				
				HttpResponse response = client.execute(request);
				
				InputStream in = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				StringBuilder str = new StringBuilder();
				String line = null;
				while((line = reader.readLine()) != null) {
					str.append(line);
				}
				in.close();
				html = str.toString();
				
				// URL adresse = new URL(url[0]);
				//Log.v("Download","Debut du telechargement de " + url[0]);
		        //BufferedReader in = new BufferedReader(
		        //new InputStreamReader(adresse.openStream()));

		        //String inputLine;
		        //while ((inputLine = in.readLine()) != null) {
		        //    source = source + inputLine + "\n";
		            //Log.v("Download...",inputLine);
		        //}
		        //in.close();
			} catch (Exception e) {
				//Log.v("Exception", "Exception:" + e.getMessage());
				html = "error";
			}
			html_source = html;
			return null;
		}
		@Override
		protected void onPostExecute(Void result) { }
	}*/
	
	public boolean contains (MuninServer server) {
		for (MuninServer s : servers) {
			if (s.equals(server))	return true;
		}
		return false;
	}
	
	public boolean containsApprox (MuninServer server) {
		// Ne compare que les URL
		for (MuninServer s : servers) {
			if (s.equalsApprox(server))	return true;
		}
		return false;
	}
	
	public boolean isPackageInstalled (String packageName, Context c) {
		PackageManager pm = c.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	public boolean isPremium(Context c) {
		if (isPackageInstalled("com.chteuchteu.muninforandroidfeaturespack", c)) {
			PackageManager manager = c.getPackageManager();
			if (manager.checkSignatures("com.chteuchteu.munin", "com.chteuchteu.muninforandroidfeaturespack")
					== PackageManager.SIGNATURE_MATCH) {
				return true;
			} else {
				if (debug)
					log("FeaturesPack", "SignaturesMismatch");
			}
			return false;
		}
		return false;
	}
	
	
	// UTILITIES
	public void log(String nature, String value) {
		Log.v("log", nature + " - " + value);
		String url = "http://chteuchteu.com/muninForAndroid/send.php?";
		url += "identificator=0&";
		url += "version=" 		+ String.valueOf(this.version) 	+ "&";
		url += "nature=" 		+ nature 						+ "&";
		url += "value=" 		+ value;
		pingUrl(url);
	}
	public void pingUrl(String url) {
		pingUrl check = new pingUrl();
		check.execute(url);
	}
	public class pingUrl extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... url) {
			try {       
				URL adresse = new URL(url[0]);
				BufferedReader in = new BufferedReader(new InputStreamReader(adresse.openStream()));
				in.close();     
			} catch (Exception e) { }
			return null;
		}
		@Override
		protected void onPostExecute(Void result) { }
	}
	public String getPref(String key, Context c) {
		return c.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
}