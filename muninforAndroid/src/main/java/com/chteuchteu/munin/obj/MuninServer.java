package com.chteuchteu.munin.obj;

import android.net.Uri;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.SpecialBool;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MuninServer {
	private long id;
	private String name;
	private String serverUrl;
	private List<MuninPlugin> plugins;
	private String graphURL;
	private String hdGraphURL;
	public MuninMaster master;
	public boolean isPersistant = false;
	/**
	 * Used for Alerts (display if server is unreachable)
	 */
	public SpecialBool reachable;
	
	private List<MuninPlugin> erroredPlugins;
	private List<MuninPlugin> warnedPlugins;
	
	public MuninServer() {
		this.name = "";
		this.serverUrl = "";
		this.plugins = new ArrayList<>();
		this.graphURL = "";
		this.hdGraphURL = "";
		this.erroredPlugins = new ArrayList<>();
		this.warnedPlugins = new ArrayList<>();
		this.reachable = SpecialBool.UNKNOWN;
	}
	public MuninServer (String name, String serverUrl) {
		this.name = name;
		this.serverUrl = serverUrl;
		this.plugins = new ArrayList<>();
		this.graphURL = "";
		this.hdGraphURL = "";
		this.erroredPlugins = new ArrayList<>();
		this.warnedPlugins = new ArrayList<>();
		this.reachable = SpecialBool.UNKNOWN;
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

	public void setHdGraphURL(String url) { this.hdGraphURL = url; }
	public String getHdGraphURL() { return this.hdGraphURL == null ? "" : this.hdGraphURL; }
	
	public void setPluginsList(List<MuninPlugin> pL) { this.plugins = pL; }
	public List<MuninPlugin> getPlugins() { return this.plugins; }

	public List<MuninPlugin> getErroredPlugins() { return this.erroredPlugins; }
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
	
	
	public List<MuninPlugin> getPluginsList(String userAgent) {
		List<MuninPlugin> mp = new ArrayList<>();
		String html = this.master.grabUrl(this.getServerUrl(), userAgent).html;
		
		if (html.equals(""))
			return null;
		
		//						   code  base_uri
		Document doc = Jsoup.parse(html, this.getServerUrl());
		Elements images = doc.select("img[src$=-day.png]");

		if (images.size() == 0)
			images = doc.select("img[src$=-day.svg]");
		
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

			// Find GraphURL
			if (this.graphURL.equals("")) {
				String srcAttr = image.attr("abs:src");
				this.graphURL = srcAttr.substring(0, srcAttr.lastIndexOf('/') + 1);
			}

			// Find HDGraphURL (if not already done)
			if ((this.hdGraphURL == null || this.hdGraphURL.equals("")) && this.master.isDynazoomAvailable() != MuninMaster.DynazoomAvailability.FALSE) {
				// To go to the dynazoom page, we have to "click" on the first graph.
				// Then, on the second page, we have to "click" again on the first graph.
				// Finally, the only image on this third page is the dynazoom graph.
				String srcAttr = image.parent().attr("abs:href");

				String secondPageHtml = this.master.grabUrl(srcAttr, userAgent).html;
				Document secondPage = Jsoup.parse(secondPageHtml, srcAttr);

				Elements images2 = secondPage.select("img[src$=-day.png]");
				if (images2.size() == 0)
					images2 = doc.select("img[src$=-day.svg]");

				if (images2.size() > 0) {
					Element imageParent = images2.get(0).parent();
					if (imageParent.tagName().equals("a")) {
						String srcAttr2 = imageParent.attr("abs:href");
						String thirdPageHtml = this.master.grabUrl(srcAttr2, userAgent).html;
						if (thirdPageHtml.equals(""))
							this.master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
						else {
							// Since the image URL is built in JS on the web page, we have to build it manually
							Uri uri = Uri.parse(srcAttr2);
							String cgiUrl = uri.getQueryParameterNames().contains("cgiurl_graph") ? uri.getQueryParameter("cgiurl_graph")
									: "/munin-cgi/munin-cgi-graph";
							// localdomain/localhost.localdomain/if_eth0
							String pluginNameUrl = uri.getQueryParameterNames().contains("plugin_name") ? uri.getQueryParameter("plugin_name")
									: "localdomain/localhost.localdomain/pluginName";
							// Remove plugin name from pluginNameUrl
							pluginNameUrl = pluginNameUrl.substring(0, pluginNameUrl.lastIndexOf('/') + 1);

							this.hdGraphURL = "http://" + Util.URLManipulation.getHostFromUrl(this.getServerUrl()) + cgiUrl + "/" + pluginNameUrl;

							// Now that we have the HD Graph URL, let's try to reach it to see if it is available
							if (this.master.isDynazoomAvailable(currentPl, userAgent))
								this.master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.TRUE);
							else {
								this.hdGraphURL = "";
								this.master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
							}
						}
					} else
						this.master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
				} else
					this.master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
			}
		}
		return mp;
	}
	
	public boolean fetchPluginsList(String userAgent) {
		List<MuninPlugin> plugins = getPluginsList(userAgent);
		
		if (plugins != null) {
			this.plugins = plugins;
			return true;
		}
		return false;
	}

	/**
	 * Get plugin state (OK / WARNING / CRITICAL) for each plugin
	 * in this server. Used on Activity_Alerts
	 * @param userAgent String
	 */
	public void fetchPluginsStates(String userAgent) {
		erroredPlugins.clear();
		warnedPlugins.clear();
		
		// Set all to undefined
		for (MuninPlugin plugin : this.plugins)
			plugin.setState(AlertState.UNDEFINED);
		
		HTTPResponse response = master.grabUrl(this.getServerUrl(), userAgent);
		
		if (response.timeout || response.responseCode != 200 || response.html.equals(""))
			this.reachable = SpecialBool.FALSE;
		else {
			this.reachable = SpecialBool.TRUE;
			
			Document doc = Jsoup.parse(response.html, this.getServerUrl());
			Elements images = doc.select("img[src$=-day.png]");

			if (images.size() == 0)
				images = doc.select("img[src$=-day.svg]");
			
			for (Element image : images) {
				String pluginName = image.attr("src").substring(image.attr("src").lastIndexOf('/') + 1, image.attr("src").lastIndexOf('-'));
				
				MuninPlugin plugin = null;
				// Plugin lookup
				for (MuninPlugin m : this.plugins) {
					if (m.getName().equals(pluginName)) {
						plugin = m; break;
					}
				}
				if (plugin != null) {
					if (image.hasClass("crit") || image.hasClass("icrit")) {
						plugin.setState(AlertState.CRITICAL);
						erroredPlugins.add(plugin);
					}
					else if (image.hasClass("warn") || image.hasClass("iwarn")) {
						plugin.setState(AlertState.WARNING);
						warnedPlugins.add(plugin);
					}
					else
						plugin.setState(AlertState.OK);
				}
			}
		}
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
	
	private List<MuninPlugin> getPluginsByCategory(String c) {
		List<MuninPlugin> l = new ArrayList<>();
		for (MuninPlugin p : plugins) {
			if (p.getCategory() != null && p.getCategory().equals(c))
				l.add(p);
		}
		return l;
	}
	
	private List<String> getDistinctCategories() {
		List<String> list = new ArrayList<>();
		
		for (MuninPlugin plugin : plugins) {
			if (plugin.getCategory() == null)
				continue;

			// Check if list already contains
			boolean contains = false;
			for (String s : list) {
				if (plugin.getCategory().equals(s))
					contains = true;
			}
			if (!contains)
				list.add(plugin.getCategory());
		}
		return list;
	}
	
	public List<List<MuninPlugin>> getPluginsListWithCategory() {
		List<List<MuninPlugin>> l = new ArrayList<>();
		for (String s : getDistinctCategories()) {
			l.add(getPluginsByCategory(s));
		}
		return l;
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