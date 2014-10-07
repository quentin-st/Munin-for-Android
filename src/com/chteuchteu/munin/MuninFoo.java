package com.chteuchteu.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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
import android.util.Base64;
import android.util.DisplayMetrics;

import com.chteuchteu.munin.hlpr.DigestUtils;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;


public class MuninFoo {
	public static MuninFoo instance;
	
	private List<MuninServer> servers;
	public List<Label> labels;
	public List<MuninMaster> masters;
	
	public SQLite sqlite;
	public MuninServer currentServer;
	
	public boolean drawer;
	
	// === VERSION === //
	// HISTORY		current:	 _______________________________________________________________________________________________________________________________
	// android:versionName:		| 1.1		1.2		1.3		1.4		1.4.1	1.4.2	1.4.5	1.4.6	2.0		2.0.1	2.1		2.2		2.3		2.4		2.5		2.6 |
	// android:versionCode: 	|  1		 2		 3		 4		 5		 6		 7	 	 8	  	 10		11		12		13		14		15		16		17	|
	// MfA version:				| 1.1		1.2		1.3		1.4		1.5		1.6		1.7  	1.8   	1.9		2.0		2.1 	2.2		2.3		2.4		2.5		2.6	|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	//							| 2.6.1		2.6.2	2.6.3	2.6.4	2.6.5	2.7		2.7.1	2.7.5	2.7.6	2.7.7	2.8		2.8.1	2.8.2	2.8.3	2.8.4	3.0 |
	//							|  18		 19		20		21		22		23		24		25		26		27		28		29		30		31		32		33  |
	//							|  2.7		2.8		2.9		3.0		3.1		3.2		3.3		3.4		3.5		3.6		3.7		3.8		3.9		4.0		4.1		4.2 |
	//							|															beta	beta	beta			fix		fix		fix		fix			|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	//							|																																|
	//							|																																|
	//							|																																|
	//							|																																|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	
	public double version = 4.2;
	// =============== //
	public static final boolean DEBUG = true;
	public static final boolean FORCE_NOT_PREMIUM = false;
	public boolean premium;
	
	private MuninFoo() {
		premium = false;
		drawer = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		masters = new ArrayList<MuninMaster>();
		sqlite = new SQLite(null, this);
		instance = null;
		loadInstance();
	}
	
	public MuninFoo(Context c) {
		premium = false;
		drawer = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		masters = new ArrayList<MuninMaster>();
		sqlite = new SQLite(c, this);
		instance = null;
		loadInstance(c);
	}
	
	public void loadInstance() {
		this.masters = sqlite.dbHlpr.getMasters(this.masters);
		this.servers = sqlite.dbHlpr.getServers(this.masters);
		this.labels = sqlite.dbHlpr.getLabels();
		
		attachOrphanServers();
		
		currentServer = null;
		if (servers.size() > 0)
			currentServer = getServerFromFlatPosition(0);
		
		if (DEBUG)
			sqlite.logMasters();
	}
	
	public void loadInstance(Context c) {
		loadInstance();
		if (c != null) {
			// Set default server
			String defaultServerUrl = Util.getPref(c, "defaultServer");
			if (!defaultServerUrl.equals("")) {
				for (MuninServer server : this.servers) {
					if (server.getServerUrl().equals(defaultServerUrl))
						currentServer = server;
				}
			}
			
			this.premium = isPremium(c);
			if (!Util.getPref(c, "drawer").equals("false"))
				this.drawer = true;
			else
				this.drawer = false;
		}
	}
	
	public void resetInstance(Context c) {
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		sqlite = new SQLite(c, this);
		loadInstance(c);
	}
	
	public static synchronized MuninFoo getInstance() {
		if (instance == null)
			instance = new MuninFoo();
		return instance;
	}
	
	public static synchronized MuninFoo getInstance(Context c) {
		if (instance == null)
			instance = new MuninFoo(c);
		return instance;
	}
	
	/**
	 * Set a common parent to the servers which does not
	 * have one after getting them
	 */
	private void attachOrphanServers() {
		int n = 0;
		MuninMaster defMaster = new MuninMaster("Default");
		defMaster.defaultMaster = true;
		
		for (MuninServer s : this.servers) {
			if (s.getParent() == null) {
				s.setParent(defMaster);
				n++;
			}
		}
		
		if (n > 0)
			this.masters.add(defMaster);
	}
	
	public void loadLanguage(Context c) {
		if (!Util.getPref(c, "lang").equals("")) {
			String lang = Util.getPref(c, "lang");
			// lang == "en" || "fr" || "de" || "ru"
			if (!(lang.equals("en") || lang.equals("fr") || lang.equals("de") || lang.equals("ru"))) {
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
		s.getParent().rebuildChildren(this);
		for (int i=0; i<this.servers.size(); i++) {
			if (this.servers.get(i).equalsApprox(s)) {
				this.servers.remove(i); break;
			}
		}
		s.getParent().rebuildChildren(this);
		if (this.currentServer.equals(s) && this.servers.size() > 0)
			this.currentServer = this.servers.get(0);
	}
	public void deleteMuninMaster(MuninMaster m) {
		if (this.masters.remove(m))
			sqlite.dbHlpr.deleteMaster(this, m, false);
	}
	public boolean addLabel(Label l) {
		boolean contains = false;
		for (Label ml : labels) {
			if (ml.getName().equals(l.getName())) {
				contains = true; break;
			}
		}
		if (!contains)
			labels.add(l);
		return !contains;
	}
	public boolean removeLabel(Label label) {
		List<Label> list = new ArrayList<Label>();
		boolean someThingDeleted = false;
		for (Label l : labels) {
			if (!l.equals(label))
				list.add(l);
			else
				someThingDeleted = true;
		}
		labels = list;
		if (someThingDeleted)
			sqlite.saveLabels();
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
	public void deleteServer(int pos) {
		if (pos >= 0 && pos < servers.size()) {
			servers.remove(pos);
		}
		if (getHowManyServers() == 0)
			currentServer = null;
		else
			currentServer = servers.get(0);
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
	public MuninServer getFirstServer() {
		for (MuninServer server : servers) {
			if (server != null)
				return server;
		}
		return null;
	}
	@Deprecated
	public MuninServer getServerFromPosition(int position) {
		for (MuninServer s : servers) {
			if (s != null && s.getPosition() == position)
				return s;
		}
		return null;
	}
	@Deprecated
	public MuninServer getServerFromFlatPosition(int position) {
		// si pos -> 0 1 4 8 9 11
		// gSFFP(2) -> 4 (!= null)
		if (position >= 0 && position < getOrderedServers().size())
			return getOrderedServers().get(position);
		return null;
	}
	public MuninServer getServersInstanceFromMuninMasterInstance(MuninServer s) {
		for (MuninServer server : this.servers) {
			if (server.getId() == s.getId())
				return server;
		}
		// Couldn't get server from its id, trying with equals method
		for (MuninServer server : this.servers) {
			if (server.equalsApprox(s))
				return server;
		}
		return null;
	}
	public List<MuninServer> getOrderedServers() {
		List<MuninServer> l = new ArrayList<MuninServer>();
		for (MuninMaster m : this.masters) {
			for (MuninServer s : m.getOrderedChildren())
				l.add(getServersInstanceFromMuninMasterInstance(s));
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
		for (MuninServer s : servers) {
			if (s.getServerUrl().equals(url))
				return s;
		}
		return null;
	}
	
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
	
	public ArrayList<MuninMaster> getMasters() { return (ArrayList<MuninMaster>) this.masters; }
	
	public List<String> getMastersNames() {
		List<String> l = new ArrayList<String>();
		for (MuninMaster m : this.masters)
			l.add(m.getName());
		return l;
	}
	
	public MuninMaster getMasterById(int id) {
		for (MuninMaster m : this.masters) {
			if (m.getId() == id)
				return m;
		}
		return null;
	}
	
	public int getMasterPosition(MuninMaster m) {
		int i = 0;
		for (MuninMaster mas : this.masters) {
			if (mas.getId() == m.getId())
				return i;
			i++;
		}
		return 0;
	}
	
	public boolean containsLabel(String lname) {
		for (Label l : labels) {
			if (l.getName().equals(lname))
				return true;
		}
		return false;
	}
	
	public Label getLabel(String lname) {
		for (Label l : labels) {
			if (l.getName().equals(lname))
				return l;
		}
		return null;
	}
	
	/**
	 * Old way to get the servers from a master one
	 * @deprecated use {@link fetchServersListRecursive(MuninServer s)} instead.
	 */
	@Deprecated
	public int fetchServersList(MuninServer server) {
		int nbServers = 0;
		//List<MuninServer> mp = new ArrayList<MuninServer>();
		
		// Graph html content
		String html = "";
		try {
			html = grabUrl(server).html;
		} catch (Exception e) { }
		
		if (!html.equals("") && !html.equals("error")) {
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
				
				//mp.add(currentServ);
				addServer(currentServ);
				nbServers++;
			}
		}
		return nbServers;
	}
	
	public int fetchServersListRecursive(MuninServer s) {
		int nbServers = 0;
		
		// Grab HTML content
		String html = grabUrl(s).html;
		
		if (!html.equals("") && !html.equals("error")) {
			Document doc = Jsoup.parse(html, s.getServerUrl());
			
			// Check if Munin or MunStrap
			if (html.contains("MunStrap")) { // Munstrap
				Elements domains = doc.select("ul.groupview > li > a");
				
				for (Element domain : domains) {
					MuninMaster m = new MuninMaster();
					// Get the domain name
					m.setName(domain.text());
					m.setUrl(domain.attr("abs:href"));
					this.masters.add(m);
					
					int pos = 0;
					// Get every host for that domain
					Elements hosts = domain.parent().select("ul>li");
					for (Element host : hosts) {
						MuninServer serv = new MuninServer(host.child(0).text(), host.child(0).attr("abs:href"));
						if (s.isAuthNeeded()) {
							serv.setAuthIds(s.getAuthLogin(), s.getAuthPassword(), s.getAuthType());
							serv.setAuthType(s.getAuthType());
							serv.setAuthString(s.getAuthString());
						}
						serv.setSSL(s.getSSL());
						serv.setParent(m);
						serv.setPosition(pos);
						pos++;
						this.addServer(serv);
						nbServers++;
					}
				}
			} else { // Munin
				Elements domains = doc.select("span.domain");
				
				// I like you, Steve Schnepp, but fuck you for that fucked up HTML.
				for (Element domain : domains) {
					MuninMaster m = new MuninMaster();
					// Get the domain name
					Element a = domain.child(0);
					m.setName(a.text());
					m.setUrl(a.attr("abs:href"));
					this.masters.add(m);
					
					int pos = 0;
					// Get every host for that domain
					Elements hosts = domain.parent().select("span.host");
					for (Element host : hosts) {
						MuninServer serv = new MuninServer(host.child(0).text(), host.child(0).attr("abs:href"));
						if (s.isAuthNeeded()) {
							serv.setAuthIds(s.getAuthLogin(), s.getAuthPassword(), s.getAuthType());
							serv.setAuthType(s.getAuthType());
							serv.setAuthString(s.getAuthString());
						}
						serv.setSSL(s.getSSL());
						serv.setParent(m);
						serv.setPosition(pos);
						pos++;
						this.addServer(serv);
						nbServers++;
					}
				}
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
				if (s.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString((s.getAuthLogin() + ":" + s.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (s.getAuthType() == AuthType.DIGEST) {
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
	
	public static Bitmap grabBitmap(MuninServer server, String url) {
		return grabBitmap(server, url, false);
	}
	
	public static Bitmap grabBitmap(MuninServer server, String url, boolean retried) {
		Bitmap b = null;
		
		try {
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
					server.setSSL(false);
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			if (server.isAuthNeeded()) {
				if (server.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString((server.getAuthLogin() + ":" + server.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (server.getAuthType() == AuthType.DIGEST) {
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String userName = server.getAuthLogin();
					String password = server.getAuthPassword();
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
					realmName = DigestUtils.match(server.getAuthString(), "realm");
					nonce = DigestUtils.match(server.getAuthString(), "nonce");
					opaque = DigestUtils.match(server.getAuthString(), "opaque");
					qop = DigestUtils.match(server.getAuthString(), "qop");
					
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
					server.setAuthString(response.getHeaders("WWW-Authenticate")[0].getValue());
					return grabBitmap(server, url, true);
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
	
	public static boolean isPackageInstalled (String packageName, Context c) {
		PackageManager pm = c.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	public static boolean isPremium(Context c) {
		if (isPackageInstalled("com.chteuchteu.muninforandroidfeaturespack", c)) {
			if (DEBUG && FORCE_NOT_PREMIUM)
				return false;
			if (DEBUG)
				return true;
			PackageManager manager = c.getPackageManager();
			if (manager.checkSignatures("com.chteuchteu.munin", "com.chteuchteu.muninforandroidfeaturespack")
					== PackageManager.SIGNATURE_MATCH) {
				return true;
			}
			return false;
		}
		return false;
	}
}