package com.chteuchteu.munin.obj;

import com.chteuchteu.munin.hlpr.Dynazoom.DynazoomAvailability;
import com.chteuchteu.munin.hlpr.Exception.Http.HttpException;
import com.chteuchteu.munin.hlpr.Parser.DynazoomDiscoveryHelper;
import com.chteuchteu.munin.hlpr.Parser.PageParser;
import com.chteuchteu.munin.hlpr.Util.SpecialBool;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MuninNode {
	private long id;
	private String name;
	private String url;
	private List<MuninPlugin> plugins;
	private String graphURL;
	private String hdGraphURL;
	public MuninMaster master;
	private int position;
	public boolean isPersistant = false;
	/**
	 * Used for Alerts (display if node is unreachable)
	 */
	public SpecialBool reachable;

	private List<MuninPlugin> erroredPlugins;
	private List<MuninPlugin> warnedPlugins;

	public MuninNode() {
		this.name = "";
		this.url = "";
		this.plugins = new ArrayList<>();
		this.graphURL = "";
		this.hdGraphURL = "";
		this.erroredPlugins = new ArrayList<>();
		this.warnedPlugins = new ArrayList<>();
		this.reachable = SpecialBool.UNKNOWN;
		this.position = -1;
	}
	public MuninNode(String name, String url) {
		this.name = name;
		this.url = url;
		this.plugins = new ArrayList<>();
		this.graphURL = "";
		this.hdGraphURL = "";
		this.erroredPlugins = new ArrayList<>();
		this.warnedPlugins = new ArrayList<>();
		this.reachable = SpecialBool.UNKNOWN;
		this.position = -1;
	}

	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }

	public void setUrl(String u) { this.url = u; }
	public String getUrl() { return this.url; }

	public void setName(String n) { this.name = n; }
	public String getName() { return this.name; }

	public void setGraphURL(String url) { this.graphURL = url; }
	public String getGraphURL() { return this.graphURL; }

	public void setHdGraphURL(String url) { this.hdGraphURL = url; }
	public String getHdGraphURL() { return this.hdGraphURL == null ? "" : this.hdGraphURL; }

	public void setPluginsList(List<MuninPlugin> pL) { this.plugins = pL; }
	public List<MuninPlugin> getPlugins() { return this.plugins; }

	public void setPosition(int val) { this.position = val; }
	public int getPosition() { return this.position; }

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


    /**
     * @deprecated TODO - move this out of this class
     */
	public List<MuninPlugin> getPluginsList(String userAgent) throws HttpException {
		HTMLResponse response = this.master.downloadUrl(this.getUrl(), userAgent);

        // Throw if request went wrong
        response.throwOnFailure();

		ArrayList<MuninPlugin> plugins = PageParser.parsePlugins(this, response.getHtml());

        // Find HDGraphURL (if not already done in a previous loop iteration) and DynazoomAvailability
        if ((this.hdGraphURL == null || this.hdGraphURL.equals(""))
            && this.master.isDynazoomAvailable() != DynazoomAvailability.FALSE) {
            DynazoomDiscoveryHelper.checkDynazoomAvailability(this, userAgent);
        }

		return plugins;
	}

    /**
     * @deprecated TODO - move this out of this class
     */
	public boolean fetchPluginsList(String userAgent) throws HttpException {
		List<MuninPlugin> plugins = getPluginsList(userAgent);

		if (plugins != null) {
			this.plugins = plugins;
			return true;
		}
		return false;
	}

	/**
	 * Get plugin state (OK / WARNING / CRITICAL) for each plugin
	 * in this senodeUsed on Activity_Alerts
	 * @param userAgent String
     *
     * @deprecated TODO - move this out of this class
	 */
	public void fetchPluginsStates(String userAgent) {
		erroredPlugins.clear();
		warnedPlugins.clear();

		// Set all to undefined
		for (MuninPlugin plugin : this.plugins)
			plugin.setState(AlertState.UNDEFINED);

		HTMLResponse response = master.downloadUrl(this.getUrl(), userAgent);

		if (!response.hasSucceeded())
			this.reachable = SpecialBool.FALSE;
		else {
			this.reachable = SpecialBool.TRUE;

			Document doc = Jsoup.parse(response.getHtml(), this.getUrl());
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

    public boolean hasPlugin(MuninPlugin plugin) {
        for (MuninPlugin p : this.plugins) {
            if (p.equalsApprox(plugin))
                return true;
        }
        return false;
    }

    public MuninPlugin getFirstPluginFromCategory(String categoryName) {
        for (MuninPlugin p : this.plugins) {
            if (p.getCategory().equals(categoryName))
                return p;
        }
        return null;
    }

    public boolean hasCategory(String categoryName) {
        for (MuninPlugin p : this.plugins) {
            if (p.getCategory().equals(categoryName))
                return true;
        }
        return false;
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

	public boolean equalsApprox (MuninNode node2) {
	    return equalsApprox(node2.getUrl());
	}

	public boolean equalsApprox (String node2) {
		String address1 = this.getUrl();
		String address2 = node2;

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
