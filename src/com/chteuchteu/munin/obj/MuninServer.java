package com.chteuchteu.munin.obj;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Base64;
import android.util.Log;

import com.chteuchteu.munin.CustomSSLFactory;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.hlpr.DigestUtils;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;

public class MuninServer {
	private long id;
	private String name;
	private String serverUrl;
	private List<MuninPlugin> plugins;
	private String authLogin;
	private String authPassword;
	private String graphURL;
	private Boolean ssl;
	private int position;
	private AuthType authType;
	private String authString;
	public MuninMaster master;
	public boolean isPersistant = false;
	
	private List<MuninPlugin> 	erroredPlugins;
	private List<MuninPlugin> 	warnedPlugins;
	
	public MuninServer() {
		this.name = "";
		this.serverUrl = "";
		this.plugins = new ArrayList<MuninPlugin>();
		this.authLogin = "";
		this.authPassword = "";
		this.graphURL = "";
		this.ssl = false;
		this.authType = AuthType.UNKNOWN;
		this.position = -1;
		this.authString = "";
	}
	public MuninServer (String name, String serverUrl) {
		this.name = name;
		this.serverUrl = serverUrl;
		this.plugins = new ArrayList<MuninPlugin>();
		this.authLogin = "";
		this.authPassword = "";
		this.graphURL = "";
		this.ssl = false;
		this.authType = AuthType.UNKNOWN;
		this.position = -1;
		this.authString = "";
		generatePosition();
	}
	public MuninServer (String name, String serverUrl, String authLogin, String authPassword, String graphUrl,
			boolean SSL, int position, AuthType authType, String authString) {
		this.name = name;
		this.serverUrl = serverUrl;
		this.plugins = new ArrayList<MuninPlugin>();
		this.authLogin = authLogin;
		this.authPassword = authPassword;
		this.graphURL = graphUrl;
		this.ssl = SSL;
		this.position = position;
		this.authType = authType;
		this.authString = authString;
	}
	
	public enum AuthType {
		UNKNOWN(-2), NONE(-1), BASIC(1), DIGEST(2);
		private int val = -2;
		
		AuthType(int val) { this.val = val; }
		public int getVal() { return this.val; }
		public String toString() { return val + ""; }
		public static AuthType get(int val) {
			for (AuthType t : AuthType.values())
				if (t.val == val)
					return t;
			return UNKNOWN;
		}
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
	
	public void setServerUrl(String u) { this.serverUrl = u; }
	public String getServerUrl() { return this.serverUrl; }
	
	public void setName(String n) { this.name = n; }
	public String getName() { return this.name; }
	
	public void setGraphURL(String url) { this.graphURL = url; }
	public String getGraphURL() { return this.graphURL; }
	
	public void setPluginsList(List<MuninPlugin> pL) { this.plugins = pL; }
	public List<MuninPlugin> getPlugins() { return this.plugins; }
	
	public void setAuthType(AuthType t) { this.authType = t; }
	public AuthType getAuthType() { return this.authType; }
	public boolean isAuthNeeded() { return this.authType != AuthType.NONE; }
	public void setAuthIds(String login, String password) {
		this.authLogin = login;
		this.authPassword = password;
		
		if (login.equals("") && password.equals(""))
			this.authType = AuthType.NONE;
	}
	public void setAuthIds(String login, String password, AuthType authType) {
		setAuthIds(login, password);
		
		if (login.equals("") || password.equals(""))
			this.authType = AuthType.NONE;
		else
			this.authType = authType;
	}
	public String getAuthLogin() { return this.authLogin; }
	public String getAuthPassword() { return this.authPassword; }
	
	public void setAuthString(String s) { this.authString = s; }
	public String getAuthString() { return this.authString; }
	
	public void setSSL(boolean value) { this.ssl = value; }
	public boolean getSSL() { return this.ssl; }
	
	public void setPosition(int position) { this.position = position; }
	public int getPosition() { return this.position; }
	
	public void setErroredPlugins(List<MuninPlugin> mp) { this.erroredPlugins = mp; }
	public List<MuninPlugin> getErroredPlugins() { return this.erroredPlugins; }
	public void setWarnedPlugins(List<MuninPlugin> mp) { this.warnedPlugins = mp; }
	public List<MuninPlugin> getWarnedPlugins() { return this.warnedPlugins; }
	
	public void setParent(MuninMaster p) {
		this.master = p;
		if (p != null)
			p.addChild(this);
	}
	public MuninMaster getParent() { return this.master; }
	
	
	public boolean fetchPluginsList() {
		List<MuninPlugin> mp = new ArrayList<MuninPlugin>();
		String html = grabUrl(this.getServerUrl()).html;
		
		if (html == "" || html == "error") {
			this.plugins = new ArrayList<MuninPlugin>();
			return false;
		}
		
		MuninPlugin currentPl;
		
		//						   code  base_uri
		Document doc = Jsoup.parse(html, this.getServerUrl());
		Elements images = doc.select("img[src$=-day.png]");
		
		currentPl = new MuninPlugin(null, this);
		String fancyName;
		String nomPlugin;
		String pluginPageUrl;
		
		for (Element image : images) {
			nomPlugin = image.attr("src").substring(image.attr("src").lastIndexOf('/') + 1, image.attr("src").lastIndexOf('-'));
			// Suppression des caractères spéciaux
			nomPlugin = nomPlugin.replace("&", "");
			nomPlugin = nomPlugin.replace("^", "");
			nomPlugin = nomPlugin.replace("\"", "");
			nomPlugin = nomPlugin.replace(",", "");
			nomPlugin = nomPlugin.replace(";", "");
			fancyName = image.attr("alt");
			
			// Récupération de la page du graph
			Element link = image.parent();
			pluginPageUrl = link.attr("abs:href");
			
			// Récupération du nom du groupe
			// Munin 2.X
			boolean is2 = true;
			Element table = image.parent().parent().parent().parent().parent();
			String group = "";
			if (table != null) {
				Element h3 = table.previousElementSibling();
				if (h3 != null)
					group = h3.html();
				else
					is2 = false;
			} else
				is2 = false;
			
			// Munin 1.4
			if (!is2) {
				try {
					Element h3 = image.parent().parent().parent().parent().child(0).child(0).child(0);
					group = h3.html();
				}
				catch (Exception e) { }
			}
			
			if (nomPlugin != null && !nomPlugin.equals("")) {
				currentPl = new MuninPlugin(nomPlugin, this);
				// Deleting quotes
				fancyName = fancyName.replaceAll("\"", "");
				currentPl.setFancyName(fancyName);
				currentPl.setCategory(group);
				currentPl.setPluginPageUrl(pluginPageUrl);
				
				mp.add(currentPl);
				
				if (this.graphURL == null || (this.graphURL != null && this.graphURL.equals("")))
					this.graphURL = image.attr("abs:src").substring(0, image.attr("abs:src").lastIndexOf('/') + 1);
			}
		}
		
		this.plugins = mp;
		return true;
	}
	public void fetchPluginsStates() {
		erroredPlugins = new ArrayList<MuninPlugin>();
		warnedPlugins = new ArrayList<MuninPlugin>();
		
		String html = grabUrl(this.getServerUrl()).html;
		
		if (!html.equals("")) {
			Document doc = Jsoup.parse(html, this.getServerUrl());
			Elements images = doc.select("img[src$=-day.png]");
			String nomPlugin = "";
			MuninPlugin mp = null;
			
			for (Element image : images) {
				nomPlugin = image.attr("src").substring(image.attr("src").lastIndexOf('/') + 1, image.attr("src").lastIndexOf('-'));
				// Recherche du plugin en question
				for (MuninPlugin m : this.plugins) {
					if (m != null && m.getName().equals(nomPlugin)) {
						mp = m; break;
					}
				}
				if (mp != null) {
					if (image.hasClass("crit") || image.hasClass("icrit")) {
						mp.setState(AlertState.CRITICAL);
						erroredPlugins.add(mp);
					}
					else if (image.hasClass("warn") || image.hasClass("iwarn")) {
						mp.setState(AlertState.WARNING);
						warnedPlugins.add(mp);
					}
					else
						mp.setState(AlertState.OK);
				}
			}
		}
		
		// Passage du reste à MuninPlugin.ALERTS_STATE_UNDEFINED
		for (int i=0; i<this.plugins.size(); i++) {
			if (this.getPlugin(i) != null && (this.getPlugin(i).getState() == null || (this.getPlugin(i).getState() != null && this.getPlugin(i).getState().equals(""))))
				this.getPlugin(i).setState(AlertState.UNDEFINED);
		}
	}
	
	public void importData(MuninServer source) {
		this.name = source.name;
		this.serverUrl = source.serverUrl;
		this.plugins = source.plugins;
		this.authLogin = source.authLogin;
		this.authPassword = source.authPassword;
		this.authType = source.authType;
		this.authString = source.authString;
		this.graphURL = source.graphURL;
		this.ssl = source.ssl;
		this.position = source.position;
		this.erroredPlugins = source.erroredPlugins;
		this.warnedPlugins = source.warnedPlugins;
		this.master = source.master;
	}
	
	/**
	 * Get the type of the given page:
	 * 	- munin/		: list of servers
	 * 	- munin/x/		: list of plugins
	 * 	- err_code		: if error -> error code
	 * @return
	 */
	public String detectPageType() {
		HTTPResponse res = grabUrl(this.serverUrl);
		String page = res.html;
		if (!res.header_wwwauthenticate.equals("")) {
			// Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
			this.authString = res.header_wwwauthenticate;
			if (res.header_wwwauthenticate.contains("Digest"))
				this.authType = AuthType.DIGEST;
			else if (res.header_wwwauthenticate.contains("Basic"))
				this.authType = AuthType.BASIC;
		}
		
		if (res.timeout)
			return "timeout";
		else if (res.responseCode == 200) {
			Document doc = Jsoup.parse(page, this.getServerUrl());
			Elements images = doc.select("img[src$=-day.png]");
			
			if (images.size() > 0)
				return "munin/x/";
			else {
				// Munin normal
				Elements muninHosts = doc.select("span.host");
				
				// MunStrap
				Elements munstrapHosts = doc.select("ul.groupview");
				
				if (muninHosts.size() > 0 || munstrapHosts.size() > 0)
					return "munin/";
				else
					return res.responseCode + " - " + res.responseReason;
			}
		} else
			return res.responseCode + " - " + res.responseReason;
	}
	
	public void createTitle () {
		// http://demo.munin-monitoring.org/munin-monitoring.org/demo.munin-monitoring.org
		//														 -------------------------
		// http://demo.munin-monitoring.org/munin-monitoring.org/demo.munin-monitoring.org/index.html
		//														 -------------------------
		// http://88.180.108.193/munin/localdomain/localhost.localdomain/index.html
		//		  --------------
		// http://88.180.108.193/munin/localdomain/localhost.localdomain/
		//		  --------------
		
		String[] s = this.serverUrl.split("/");
		
		// Accès à des tableaux, try{} catch(){} au cas où
		try {
			if (s[s.length-1].contains(".htm") || s[s.length-1].equals("")) {
				if (s.length >= 3 && s[s.length-2].equals("localhost.localdomain") && s[s.length-3].equals("localdomain")) {
					if (s.length >= 5 && s[s.length-4].equals("munin"))
						this.name = s[s.length-5];
					else
						this.name = s[s.length-4];
				} else
					this.name = s[s.length-2];
			} else {
				if (s.length >= 2 && s[s.length-1].equals("localhost.localdomain") && s[s.length-2].equals("localdomain")) {
					if (s.length >= 4 && s[s.length-3].equals("munin"))
						this.name = s[s.length-4];
					else
						this.name = s[s.length-3];
				} else
					this.name = s[s.length-1];
			}
		} catch (Exception ex) { }
		if (this.name == null || this.name.equals(""))
			this.name = s[s.length-2];
	}
	
	public HTTPResponse grabUrl(String url) {
		HTTPResponse resp = new HTTPResponse("", -1);
		try {
			HttpClient client = null;
			if (this.ssl) {
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
					this.ssl = false;
				}
			} else
				client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			if (this.isAuthNeeded()) {
				if (this.getAuthType() == AuthType.BASIC)
					request.setHeader("Authorization", "Basic " + Base64.encodeToString((authLogin + ":" + authPassword).getBytes(), Base64.NO_WRAP));
				else if (this.getAuthType() == AuthType.DIGEST) {
					// Digest foo:digestedPass, realm="munin", nonce="+RdhgM7qBAA=86e58ecf5cbd672ba8246c4f9eed4a389fe87fd6", algorithm=MD5, qop="auth"
					// WWW-Authenticate   Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
					String userName = this.authLogin;
					String password = this.authPassword;
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
					realmName = DigestUtils.match(this.authString, "realm");
					nonce = DigestUtils.match(this.authString, "nonce");
					opaque = DigestUtils.match(this.authString, "opaque");
					qop = DigestUtils.match(this.authString, "qop");
					
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
			if (MuninFoo.debug)
				Log.v("", "Downloaded content : " + resp.html);
			resp.responseReason = response.getStatusLine().getReasonPhrase();
			resp.responseCode = response.getStatusLine().getStatusCode();
			if (response.getHeaders("WWW-Authenticate").length > 0)
				resp.header_wwwauthenticate = response.getHeaders("WWW-Authenticate")[0].getValue();
		}
		catch (SocketTimeoutException e) { e.printStackTrace(); resp.timeout = true; }
		catch (ConnectTimeoutException e) { e.printStackTrace(); resp.timeout = true; }
		catch (SSLPeerUnverifiedException e) {
			this.ssl = true;
			if (this.serverUrl.equals(url))
				this.serverUrl = Util.setHttps(url);
			url = Util.setHttps(url);
			return grabUrl(url);
		}
		catch (SSLException e) {
			this.ssl = true;
			if (this.serverUrl.equals(url))
				this.serverUrl = Util.setHttps(url);
			url = Util.setHttps(url);
			return grabUrl(url);
		}
		catch (Exception e) { e.printStackTrace(); resp.html = ""; }
		return resp;
	}
	
	
	
	public MuninPlugin getPlugin(int pos) {
		if (pos < this.plugins.size() && pos >= 0)
			return this.plugins.get(pos);
		else
			return null;
	}
	
	public int getPosition(MuninPlugin p) {
		for (int i=0; i<this.plugins.size(); i++) {
			if (p.equalsApprox(this.plugins.get(i)))
				return i;
		}
		return 0;
	}
	
	
	public void generatePosition() {
		MuninFoo muninFoo = MuninFoo.getInstance();
		// Si toutes positions == -1 -> pos = 0
		int nbNotNull = 0;
		
		for (int i=0; i<muninFoo.getHowManyServers(); i++) {
			if (muninFoo.getServer(i) != null && muninFoo.getServer(i).getPosition() != -1)
				nbNotNull++;
		}
		if (nbNotNull == 0)
			this.position = 0;
		
		if (muninFoo != null) {
			// Sauvegarde la toute dernière position
			int higherPosition = -1;
			for (int i=0; i<muninFoo.getHowManyServers(); i++) {
				if (muninFoo.getServer(i) != null && muninFoo.getServer(i).getPosition() > higherPosition)
					higherPosition = muninFoo.getServer(i).getPosition();
			}
			this.position = higherPosition + 1;
		}
	}
	
	public int getFlatPosition() {
		// si pos -> 0 1 4 8 9 11
		// gFP(2) -> 4 (!= null)
		for (int i=0; i<MuninFoo.getInstance().getOrderedServers().size(); i++) {
			if (MuninFoo.getInstance().getOrderedServers().get(i).equalsApprox(this))
				return i;
		}
		return 0;
	}
	
	
	public List<MuninPlugin> getPluginsByCategory(String c) {
		List<MuninPlugin> l = new ArrayList<MuninPlugin>();
		for (MuninPlugin p : plugins) {
			if (p.getCategory() != null && p.getCategory().equals(c))
				l.add(p);
		}
		return l;
	}
	
	public List<String> getDistinctCategories() {
		List<String> l = new ArrayList<String>();
		
		for (MuninPlugin p : plugins) {
			boolean contains = false;
			for (String s : l) {
				if (p.getCategory().equals(s))
					contains = true;
			}
			if (!contains)
				l.add(p.getCategory());
		}
		return l;
	}
	
	public List<List<MuninPlugin>> getPluginsListWithCategory() {
		List<List<MuninPlugin>> l = new ArrayList<List<MuninPlugin>>();
		for (String s : getDistinctCategories()) {
			l.add(getPluginsByCategory(s));
		}
		return l;
	}
	
	public int getPluginPosition(MuninPlugin p) {
		int i = 0;
		for (MuninPlugin pl : plugins) {
			if (pl.equalsApprox(p))
				return i;
			i++;
		}
		return 0;
	}
	
	
	public boolean equalsApprox (MuninServer server2) {
		String address1 = this.getServerUrl();
		String address2 = server2.getServerUrl();
		// transformations
		if (address1.length() > 11) {
			if (address1.contains("index.html"))
				address1 = address1.substring(0, address1.length()-11);
			if (address1.substring(address1.length()-1).equals("/"))
				address1 = address1.substring(0, address1.length()-1);
		}
		if (address2.length() > 11) {
			if (address2.contains("index.html"))
				address2 = address2.substring(0, address2.length()-11);
			if (address2.substring(address2.length()-1).equals("/"))
				address2 = address2.substring(0, address2.length()-1);
		}
		if (!address1.equals(address2))	return false;
		return true;
	}
	
	public boolean equalsApprox (String server2) {
		String address1 = this.getServerUrl();
		String address2 = server2;
		// transformations
		if (address1.length() > 11) {
			if (address1.contains("index.html"))
				address1 = address1.substring(0, address1.length()-11);
			if (address1.substring(address1.length()-1).equals("/"))
				address1 = address1.substring(0, address1.length()-1);
		}
		if (address2.length() > 11) {
			if (address2.contains("index.html"))
				address2 = address2.substring(0, address2.length()-11);
			if (address2.substring(address2.length()-1).equals("/"))
				address2 = address2.substring(0, address2.length()-1);
		}
		if (!address1.equals(address2))	return false;
		return true;
	}
}