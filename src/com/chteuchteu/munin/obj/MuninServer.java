package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;

public class MuninServer {
	private long id;
	private String name;
	private String serverUrl;
	private List<MuninPlugin> plugins;
	private String graphURL;
	private int position;
	public MuninMaster master;
	public boolean isPersistant = false;
	
	private List<MuninPlugin> erroredPlugins;
	private List<MuninPlugin> warnedPlugins;
	
	public MuninServer() {
		this.name = "";
		this.serverUrl = "";
		this.plugins = new ArrayList<MuninPlugin>();
		this.graphURL = "";
		this.position = -1;
	}
	public MuninServer (String name, String serverUrl) {
		this.name = name;
		this.serverUrl = serverUrl;
		this.plugins = new ArrayList<MuninPlugin>();
		this.graphURL = "";
		this.position = -1;
		generatePosition();
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
	
	public void setPosition(int position) { this.position = position; }
	public int getPosition() { return this.position; }
	
	public void setErroredPlugins(List<MuninPlugin> mp) { this.erroredPlugins = mp; }
	public List<MuninPlugin> getErroredPlugins() { return this.erroredPlugins; }
	public void setWarnedPlugins(List<MuninPlugin> mp) { this.warnedPlugins = mp; }
	public List<MuninPlugin> getWarnedPlugins() { return this.warnedPlugins; }
	
	public void addPlugin(MuninPlugin plugin) {
		plugin.setInstalledOn(this);
		this.plugins.add(plugin);
	}
	
	public void setParent(MuninMaster p) {
		this.master = p;
		if (p != null && !p.getChildren().contains(this))
			p.addChild(this);
	}
	public MuninMaster getParent() { return this.master; }
	
	
	public List<MuninPlugin> getPluginsList() {
		List<MuninPlugin> mp = new ArrayList<MuninPlugin>();
		String html = this.master.grabUrl(this.getServerUrl()).html;
		
		if (html.equals(""))
			return null;
		
		//						   code  base_uri
		Document doc = Jsoup.parse(html, this.getServerUrl());
		Elements images = doc.select("img[src$=-day.png]");
		
		for (Element image : images) {
			String pluginName = image.attr("src").substring(image.attr("src").lastIndexOf('/') + 1, image.attr("src").lastIndexOf('-'));
			// Delete special chars
			pluginName = pluginName.replace("&", "");
			pluginName = pluginName.replace("^", "");
			pluginName = pluginName.replace("\"", "");
			pluginName = pluginName.replace(",", "");
			pluginName = pluginName.replace(";", "");
			String fancyName = image.attr("alt");
			// Delete quotes
			fancyName = fancyName.replaceAll("\"", "");
			
			// Get graphUrl
			Element link = image.parent();
			String pluginPageUrl = link.attr("abs:href");
			
			// Get groupName
			String group = "";
			if (html.contains("MunStrap")) {
				Element tab = image.parent().parent().parent().parent();
				group = tab.id();
			} else {
				// Munin 2.X
				boolean is2 = true;
				Element table = image.parent().parent().parent().parent().parent();
				
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
					catch (Exception e) { e.printStackTrace(); }
				}
			}
			
			MuninPlugin currentPl = new MuninPlugin(pluginName, this);
			currentPl.setFancyName(fancyName);
			currentPl.setCategory(group);
			currentPl.setPluginPageUrl(pluginPageUrl);
			
			mp.add(currentPl);
			
			if (this.graphURL.equals(""))
				this.graphURL = image.attr("abs:src").substring(0, image.attr("abs:src").lastIndexOf('/') + 1);
		}
		return mp;
	}
	
	public boolean fetchPluginsList() {
		List<MuninPlugin> plugins = getPluginsList();
		
		if (plugins != null) {
			this.plugins = plugins;
			return true;
		}
		return false;
	}
	public void fetchPluginsStates() {
		erroredPlugins = new ArrayList<MuninPlugin>();
		warnedPlugins = new ArrayList<MuninPlugin>();
		
		String html = master.grabUrl(this.getServerUrl()).html;
		
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
		
		// Set others to MuninPlugin.ALERTS_STATE_UNDEFINED
		for (int i=0; i<this.plugins.size(); i++) {
			if (this.getPlugin(i) != null && (this.getPlugin(i).getState() == null || (this.getPlugin(i).getState() != null && this.getPlugin(i).getState().equals(""))))
				this.getPlugin(i).setState(AlertState.UNDEFINED);
		}
	}
	
	public void importData(MuninServer source) {
		this.name = source.name;
		this.serverUrl = source.serverUrl;
		this.plugins = source.plugins;
		this.graphURL = source.graphURL;
		this.position = source.position;
		this.erroredPlugins = source.erroredPlugins;
		this.warnedPlugins = source.warnedPlugins;
		this.master = source.master;
	}
	
	public MuninPlugin getPlugin(int pos) {
		if (pos < this.plugins.size() && pos >= 0)
			return this.plugins.get(pos);
		else
			return null;
	}
	
	public MuninPlugin getPlugin(String pluginName) {
		for (MuninPlugin plugin : this.plugins) {
			if (plugin.getName().equals(pluginName))
				return plugin;
		}
		return null;
	}
	
	public int getPosition(MuninPlugin p) {
		for (int i=0; i<this.plugins.size(); i++) {
			if (p.equalsApprox(this.plugins.get(i)))
				return i;
		}
		return 0;
	}
	
	
	private void generatePosition() {
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
	
	public int getFlatPosition(MuninFoo muninFooInstance) {
		// si pos -> 0 1 4 8 9 11
		// gFP(2) -> 4 (!= null)
		for (int i=0; i<muninFooInstance.getOrderedServers().size(); i++) {
			if (muninFooInstance.getOrderedServers().get(i).equalsApprox(this))
				return i;
		}
		return 0;
	}
	
	
	private List<MuninPlugin> getPluginsByCategory(String c) {
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
			if (address1.endsWith("index.html"))
				address1 = address1.substring(0, address1.length()-11);
			if (address1.substring(address1.length()-1).equals("/"))
				address1 = address1.substring(0, address1.length()-1);
		}
		if (address2.length() > 11) {
			if (address2.endsWith("index.html"))
				address2 = address2.substring(0, address2.length()-11);
			if (address2.substring(address2.length()-1).equals("/"))
				address2 = address2.substring(0, address2.length()-1);
		}
		return address1.equals(address2);
	}
	
	public boolean equalsApprox (String server2) {
		String address1 = this.getServerUrl();
		String address2 = server2;
		
		// transformations
		if (address1.length() > 11) {
			if (address1.endsWith("index.html"))
				address1 = address1.substring(0, address1.length()-11);
			if (address1.substring(address1.length()-1).equals("/"))
				address1 = address1.substring(0, address1.length()-1);
		}
		if (address2.length() > 11) {
			if (address2.endsWith("index.html"))
				address2 = address2.substring(0, address2.length()-11);
			if (address2.substring(address2.length()-1).equals("/"))
				address2 = address2.substring(0, address2.length()-1);
		}
		return address1.equals(address2);
	}
}