package com.chteuchteu.munin.obj;

import android.content.Context;
import android.graphics.Bitmap;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.NetHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MuninMaster {
	private long id;
	private String name;
	private String url;
	private List<MuninServer> children;
	private HDGraphs hdGraphs;
	
	private Boolean ssl;
	private AuthType authType;
	private String authLogin;
	private String authPassword;
	private String authString;
	
	public boolean defaultMaster;
	
	public boolean isPersistant;
	
	public MuninMaster () {
		this.name = "";
		this.id = -1;
		this.url = "";
		this.defaultMaster = false;
		this.children = new ArrayList<MuninServer>();
		this.hdGraphs = HDGraphs.AUTO_DETECT;
		
		this.authType = AuthType.UNKNOWN;
		this.authLogin = "";
		this.authPassword = "";
		this.ssl = false;
		this.authString = "";
		
		this.isPersistant = false;
	}
	
	public enum HDGraphs {
		AUTO_DETECT(""), FALSE("false"), TRUE("true");
		private String val = "";
		HDGraphs(String val) { this.val = val; }
		public String getVal() { return this.val; }
		public String toString() { return this.val; }
		public static HDGraphs get(String val) {
			for (HDGraphs g : HDGraphs.values())
				if (g.val.equals(val))
					return g;
			return AUTO_DETECT;
		}
		public static HDGraphs get(boolean val) {
			if (val)	return TRUE;
			else		return FALSE;
		}
	}
	
	public void setAuthType(AuthType t) { this.authType = t; }
	public AuthType getAuthType() { return this.authType; }
	public boolean isAuthNeeded() {
		return this.authType == AuthType.BASIC || this.authType == AuthType.DIGEST;
	}
	public void setAuthIds(String login, String password) {
		this.authLogin = login;
		this.authPassword = password;
		
		if ((login == null || login.equals("")) && (password == null || password.equals("")))
			this.authType = AuthType.NONE;
	}
	public void setAuthIds(String login, String password, AuthType authType) {
		setAuthIds(login, password);
		this.authType = authType;
	}
	public String getAuthLogin() { return this.authLogin; }
	public String getAuthPassword() { return this.authPassword; }
	
	public void setAuthString(String s) { this.authString = s; }
	public String getAuthString() { return this.authString; }
	
	public void setSSL(boolean value) { this.ssl = value; }
	public boolean getSSL() { return this.ssl; }
	
	/**
	 * Generates a custom name, to avoid "localdomain"
	 */
	private void generateName() {
		if (this.url.equals(""))
			return;
		
		// Everything else than localdomain is OK
		if (!this.name.equals("localdomain"))
			return;
		
		// http(s)://myurl.com/munin/
		//           ---------
		
		this.name = Util.URLManipulation.getHostFromUrl(this.url, this.name);
	}
	
	/**
	 * Checks if dynazoom is available.
	 * Warning : this has to be done on a thread
	 * @return boolean
	 */
	public boolean isDynazoomAvailable() {
		if (this.defaultMaster || this.isEmpty())
			return false;
		
		MuninServer server = getChildAt(0);
		if (server == null)
			return false;
		MuninPlugin plugin = server.getPlugin(0);
		if (plugin == null)
			return false;
		
		String hdGraphUrl = plugin.getHDImgUrl(Period.DAY);
		HTTPResponse res = grabUrl(hdGraphUrl);
		
		boolean seemsAvailable = !res.timeout && res.responseCode == 200;
		
		if (!seemsAvailable)
			return false;
		
		// At this point, the dynazoom seems available. Let's try to download a bitmap to
		// see if we get a bitmap (instead of a custom 404 error)
		Bitmap bitmap = grabBitmap(hdGraphUrl);
		return bitmap != null;
	}
	
	public void rebuildChildren(MuninFoo f) {
		this.children = new ArrayList<MuninServer>();
		for (MuninServer s : f.getServers()) {
			if (s.getParent().getId() == this.id)
				this.children.add(s);
		}
		if (this.children.size() == 0)
			deleteSelf(f);
	}
	
	private void deleteSelf(MuninFoo f) {
		// If there's no more server under this, delete self.
		f.deleteMuninMaster(this);
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
	
	public void setUrl(String url) { this.url = url; }
	public String getUrl() { return this.url; }
	
	public void setHDGraphs(HDGraphs val) { this.hdGraphs = val; }
	public HDGraphs getHDGraphs() { return this.hdGraphs; }

	public List<MuninServer> getChildren() { return this.children; }
	public void addChild(MuninServer s) {
		if (!this.children.contains(s)) {
			this.children.add(s);
			s.setParent(this);
		}
	}
	public MuninServer getChildAt(int i) {
		if (i >= 0 && i < this.children.size())
			return this.children.get(i);
		return null;
	}
	
	public boolean isEmpty() { return this.children.isEmpty(); }
	
	public boolean equalsApprox(MuninMaster p) {
		return p != null && this.url.equals(p.url);
	}
	
	public MuninServer getServerFromFlatPosition(int position) {
		// si pos -> 0 1 4 8 9 11
		// gSFFP(2) -> 4 (!= null)
		if (position >= 0 && position < getOrderedChildren().size())
			return getOrderedChildren().get(position);
		return null;
	}
	
	private MuninServer getServer(String serverUrl) {
		for (MuninServer server : this.children) {
			if (server.getServerUrl().equals(serverUrl))
				return server;
		}
		return null;
	}
	
	public List<MuninServer> getOrderedChildren() {
		// Let's first sort the list.
		// We'll then clean the positions
		List<MuninServer> source = new ArrayList<MuninServer>(this.children);
		List<MuninServer> newList = new ArrayList<MuninServer>();
		
		int curPos = 0;
		while (source.size() > 0) {
			while (Util.serversListContainsPos(source, curPos)) {
				List<MuninServer> toBeDeleted = new ArrayList<MuninServer>();
				
				for (MuninServer s : source) {
					if (s.getPosition() == curPos) {
						newList.add(s);
						toBeDeleted.add(s);
					}
				}
				
				for (MuninServer s : toBeDeleted)
					source.remove(s);
			}
			curPos++;
		}
		
		// We now have a sorted list in newList. Let's restablish the pos
		for (int i=0; i<newList.size(); i++)
			newList.get(i).setPosition(i);
		
		return newList;
	}
	
	public Bitmap grabBitmap(String url) {
		return NetHelper.grabBitmap(this, url);
	}
	
	public HTTPResponse grabUrl(String url) {
		return NetHelper.grabUrl(this, url);
	}
	
	/**
	 * Get the type of the given page:
	 * 	- munin/		: list of servers
	 * 	- munin/x/		: list of plugins (not used)
	 * 	- err_code		: if error -> error code
	 * @return String : pageType
	 */
	public String detectPageType() {
		HTTPResponse res = grabUrl(this.url);
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
			Document doc = Jsoup.parse(page, this.url);
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
	
	/**
	 * Fetches the children of this MuninMaster
	 * @return How many servers have been found
	 */
	public int fetchChildren() {
		int nbServers = 0;
		
		// Grab HTML content
		String html = grabUrl(this.url).html;
		
		if (!html.equals("")) {
			Document doc = Jsoup.parse(html, this.url);
			/*
			 * Between MfA 2.8 and 3.0, we saved Master url as http://demo.munin-monitoring.org/munin-monitoring.org/
			 * So let's update it if needed.													~~~~~~~~~~~~~~~~~~~~~
			 * So let's just check if we are on the right page, if not : update the master url.
			 */
			if (!doc.select("span.comparison").isEmpty()) {
				// Replace the current master URL
				String parentUrl = "test";
				Elements parentLinks = doc.select("a[href=..]");
				if (parentLinks.size() == 1)
					parentUrl = parentLinks.get(0).absUrl("href");
				else {
					Elements parentLinks2 = doc.select("a[href=../index.html]");
					if (parentLinks2.size() == 1)
						parentUrl = parentLinks.get(0).absUrl("href");
				}
				
				if (!parentUrl.equals(this.url)) {
					this.url = parentUrl;
					String htmlBis = grabUrl(parentUrl).html;
					if (!html.equals("")) {
						html = htmlBis;
						doc = Jsoup.parse(html, this.url);
					}
				}
			}
			// URL catchup ends here
			
			// Check if Munin or MunStrap
			if (html.contains("MunStrap")) { // Munstrap
				Elements domains = doc.select("ul.groupview > li > a");
				
				if (domains.size() > 0) {
					// There's only one domain
					Element domain = domains.get(0);
					
					// Get the domain name
					this.name = domain.text();
					//this.url = domain.attr("abs:href");
					// Generate a pretty name if needed
					generateName();
					
					int pos = 0;
					// Get every host for that domain
					Elements hosts = domain.parent().select("ul>li");
					for (Element host : hosts) {
						MuninServer serv = new MuninServer(host.child(0).text(), host.child(0).attr("abs:href"));
						serv.setParent(this);
						serv.setPosition(pos);
						pos++;
						nbServers++;
					}
				}
			} else { // Munin
				Elements domains = doc.select("span.domain");
				
				if (domains.size() > 0) {
					// There's only one domain
					Element domain = domains.get(0);
					
					// Get the domain name
					Element a = domain.child(0);
					this.name = a.text();
					//this.url = a.attr("abs:href");
					// Generate a pretty name if needed
					generateName();
					
					int pos = 0;
					// Get every host for that domain
					Elements hosts = domain.parent().select("span.host");
					for (Element host : hosts) {
						MuninServer serv = new MuninServer(host.child(0).text(), host.child(0).attr("abs:href"));
						serv.setParent(this);
						serv.setPosition(pos);
						pos++;
						nbServers++;
					}
				}
			}
		}
		
		return nbServers;
	}
	
	/**
	 * When temporary deleting an old Master, we should
	 * care about labels, widgets and grids.
	 * Here, we reattach and save the widgets.
	 * @return ArrayList<GraphWidget> : widgets who should be updated afterwards
	 */
	public ArrayList<GraphWidget> reattachWidgets(MuninFoo muninFoo, MuninMaster oldMaster) {
		ArrayList<GraphWidget> toBeUpdated_graphWidgets = new ArrayList<GraphWidget>();
		List<GraphWidget> graphWidgets = muninFoo.sqlite.dbHlpr.getGraphWidgets();
		
		if (graphWidgets.isEmpty())
			return toBeUpdated_graphWidgets;
		
		for (MuninServer server : oldMaster.getChildren()) {
			for (MuninPlugin plugin : server.getPlugins()) {
				// Check graphWidgets
				for (GraphWidget graphWidget : graphWidgets) {
					if (graphWidget.getPlugin().equals(plugin)) {
						// Reattach
						MuninPlugin newPlugin = this.getServer(server.getServerUrl()).getPlugin(graphWidget.getPlugin().getName());
						graphWidget.setPlugin(newPlugin);
						toBeUpdated_graphWidgets.add(graphWidget);
					}
				}
			}
		}
		
		return toBeUpdated_graphWidgets;
	}
	
	/**
	 * When replacing an old Master, we should care about labels, widgets and grids.
	 * Here, we reattach and save the labels.
	 * @return ArrayList<Label> : labels who should be updated afterwards
	 */
	public ArrayList<Label> reattachLabels(MuninFoo muninFoo, MuninMaster oldMaster) {
		ArrayList<Label> toBeUpdated_labels = new ArrayList<Label>();
		List<Label> labels = muninFoo.labels;
		
		if (labels.isEmpty())
			return toBeUpdated_labels;
		
		for (MuninServer server : oldMaster.getChildren()) {
			for (MuninPlugin plugin : server.getPlugins()) {
				// Check labels
				for (Label label : labels) {
					ArrayList<MuninPlugin> toBeRemoved = new ArrayList<MuninPlugin>();
					ArrayList<MuninPlugin> toBeAdded = new ArrayList<MuninPlugin>();
					
					for (MuninPlugin labelPlugin : label.plugins) {
						if (labelPlugin.equals(plugin)) {
							// Reattach
							MuninPlugin newPlugin = this.getServer(server.getServerUrl()).getPlugin(labelPlugin.getName());
							toBeRemoved.add(labelPlugin);
							toBeAdded.add(newPlugin);
							if (!toBeUpdated_labels.contains(label))
								toBeUpdated_labels.add(label);
						}
					}
					
					label.plugins.removeAll(toBeRemoved);
					label.plugins.addAll(toBeAdded);
				}
			}
		}
		
		return toBeUpdated_labels;
	}
	
	/**
	 * When replacing an old Master, we should care about labels, widgets and grids.
	 * Here, we reattach and save the grids.
	 * @return ArrayList<GridItem> : grids who should be updated afterwards
	 */
	public ArrayList<GridItem> reattachGrids(MuninFoo muninFoo, Context context, MuninMaster oldMaster) {
		ArrayList<GridItem> toBeUpdated_grids = new ArrayList<GridItem>();
		List<Grid> grids = muninFoo.sqlite.dbHlpr.getGrids(context, muninFoo);

		if (grids.isEmpty())
			return toBeUpdated_grids;
		
		for (MuninServer server : oldMaster.getChildren()) {
			for (MuninPlugin plugin : server.getPlugins()) {
				// Check grids
				for (Grid grid : grids) {
					for (GridItem item : grid.items) {
						if (item.plugin.equals(plugin)) {
							// Reattach
							item.plugin = this.getServer(server.getServerUrl()).getPlugin(item.plugin.getName());
							toBeUpdated_grids.add(item);
						}
					}
				}
			}
		}
		
		return toBeUpdated_grids;
	}
	
	/**
	 * Contacts the URL to check if there are some other servers / plugins for each server
	 */
	public String rescan(Context context, MuninFoo muninFoo) {
		// Take first server since it contains connection information
		if (isEmpty())
			return null;
		
		String report = "";
		int nbAddedServers = 0;
		int nbUpdatedServers = 0;
		int nbDeletedServers = 0;
		int nbAddedPlugins = 0;
		int nbUpdatedPlugins = 0;
		int nbDeletedPlugins = 0;
		
		if (!this.defaultMaster) { // Regular master
			// Check online
			MuninMaster onlineMaster = new MuninMaster();
			onlineMaster.setUrl(this.url);
			onlineMaster.setSSL(this.ssl);
			onlineMaster.setAuthIds(this.authLogin, this.authPassword, this.authType);
			
			onlineMaster.fetchChildren();
			
			if (!onlineMaster.isEmpty()) {
				// SERVERS DIFF
				// Add new servers if needed
				ArrayList<MuninServer> toBeAdded = new ArrayList<MuninServer>();
				ArrayList<MuninServer> toBeUpdated = new ArrayList<MuninServer>();
				// Add / update servers
				for (MuninServer onlineServer : onlineMaster.getChildren()) {
					// Check if it is in original
					boolean alreadyThere = false;
					for (MuninServer server : this.children) {
						if (server.equalsApprox(onlineServer)) {
							alreadyThere = true;
							
							// Check if we can grab some attributes
							if (!server.getGraphURL().equals(onlineServer.getGraphURL()) && !onlineServer.getGraphURL().equals("")) {
								server.setGraphURL(onlineServer.getGraphURL());
								toBeUpdated.add(server);
							}
							
							break;
						}
					}
					
					// There server isn't already there
					if (!alreadyThere)
						toBeAdded.add(onlineServer);
				}
				
				for (MuninServer server : toBeAdded) {
					addChild(server);
					muninFoo.addServer(server);
					muninFoo.sqlite.dbHlpr.insertMuninServer(server);
					nbAddedServers++;
				}
				for (MuninServer server : toBeUpdated) {
					muninFoo.sqlite.dbHlpr.updateMuninServer(server);
					nbUpdatedServers++;
				}
				
				// Remove offline servers if needed
				ArrayList<MuninServer> toBeRemoved = new ArrayList<MuninServer>();
				for (MuninServer oldServer : this.children) {
					// Check if it is still there
					boolean stillThere = false;
					for (MuninServer server : onlineMaster.getChildren()) {
						if (server.equalsApprox(oldServer)) {
							stillThere = true;
							break;
						}
					}
					
					// The server has been deleted in meantime
					if (!stillThere)
						toBeRemoved.add(oldServer);
				}
				
				for (MuninServer server : toBeRemoved) {
					this.children.remove(server);
					muninFoo.getServers().remove(server);
					muninFoo.sqlite.dbHlpr.deleteServer(server);
					nbDeletedServers++;
				}
				
				// The servers are now synced.
				// PLUGINS DIFF
				for (MuninServer server : this.children) {
					List<MuninPlugin> onlinePlugins = server.getPluginsList();
					
					// If the download hasn't failed
					if (onlinePlugins != null && onlinePlugins.size() > 0) {
						// Add new plugins
						ArrayList<MuninPlugin> pluginsToBeAdded = new ArrayList<MuninPlugin>();
						ArrayList<MuninPlugin> pluginsToBeUpdated = new ArrayList<MuninPlugin>();
						for (MuninPlugin onlinePlugin : onlinePlugins) {
							boolean alreadyThere = false;
							for (MuninPlugin oldPlugin : server.getPlugins()) {
								if (oldPlugin.equalsApprox(onlinePlugin)) {
									alreadyThere = true;
									
									// Get other values
									if (!oldPlugin.getCategory().equals(onlinePlugin.getCategory())
											&& !onlinePlugin.getCategory().equals("")) {
										oldPlugin.setCategory(onlinePlugin.getCategory());
										pluginsToBeUpdated.add(oldPlugin);
									}
									
									break;
								}
							}
							
							if (!alreadyThere)
								pluginsToBeAdded.add(onlinePlugin);
						}
						for (MuninPlugin plugin : pluginsToBeAdded) {
							// Update "installedOn" and insert:
							server.addPlugin(plugin);
							muninFoo.sqlite.dbHlpr.insertMuninPlugin(plugin);
							nbAddedPlugins++;
						}
						for (MuninPlugin plugin : pluginsToBeUpdated) {
							muninFoo.sqlite.dbHlpr.updateMuninPlugin(plugin);
							nbUpdatedPlugins++;
						}
						
						
						// Remove deleted plugins
						ArrayList<MuninPlugin> pluginsToBeRemoved = new ArrayList<MuninPlugin>();
						for (MuninPlugin oldPlugin : server.getPlugins()) {
							boolean stillThere = false;
							for (MuninPlugin onlinePlugin : onlinePlugins) {
								if (oldPlugin.equalsApprox(onlinePlugin)) {
									stillThere = true;
									break;
								}
							}
							
							if (!stillThere) {
								pluginsToBeRemoved.add(oldPlugin);
								muninFoo.sqlite.dbHlpr.deleteMuninPlugin(oldPlugin, true);
							}
						}
						for (MuninPlugin plugin : pluginsToBeRemoved) {
							server.getPlugins().remove(plugin);
							muninFoo.sqlite.dbHlpr.deletePlugin(plugin);
							nbDeletedPlugins++;
						}
					}
				}
			}
		}
		
		// Generate report
		if (nbAddedServers + nbUpdatedServers + nbDeletedServers
				+ nbAddedPlugins + nbUpdatedPlugins + nbDeletedPlugins == 0)
			report = context.getString(R.string.sync_nochange);
		else {
			// Servers
			if (nbAddedServers > 0)
				report += context.getString(R.string.sync_serversadded).replace("XXX", String.valueOf(nbAddedServers));
			if (nbUpdatedServers > 0)
				report += context.getString(R.string.sync_serversupdated).replace("XXX", String.valueOf(nbUpdatedServers));
			if (nbDeletedServers > 0)
				report += context.getString(R.string.sync_serversdeleted).replace("XXX", String.valueOf(nbDeletedServers));
			
			// Plugins
			if (nbAddedPlugins > 0)
				report += context.getString(R.string.sync_pluginsadded).replace("XXX", String.valueOf(nbAddedPlugins));
			if (nbUpdatedPlugins > 0)
				report += context.getString(R.string.sync_pluginsupdated).replace("XXX", String.valueOf(nbUpdatedPlugins));
			if (nbDeletedPlugins > 0)
				report += context.getString(R.string.sync_pluginsdeleted).replace("XXX", String.valueOf(nbDeletedPlugins));
		}
		
		return report;
	}
}