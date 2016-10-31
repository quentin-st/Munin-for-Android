package com.chteuchteu.munin.obj;

import android.content.Context;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.NetHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse.BitmapResponse;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

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
	private List<MuninNode> children;
	private DynazoomAvailability dynazoomAvailability;

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
		this.children = new ArrayList<>();
		this.dynazoomAvailability = DynazoomAvailability.AUTO_DETECT;

		this.authType = AuthType.UNKNOWN;
		this.authLogin = "";
		this.authPassword = "";
		this.ssl = false;
		this.authString = "";

		this.isPersistant = false;
	}

	public enum DynazoomAvailability {
		AUTO_DETECT(""), FALSE("false"), TRUE("true");
		private String val = "";
		DynazoomAvailability(String val) { this.val = val; }
		public String getVal() { return this.val; }
		public String toString() { return this.val; }
		public static DynazoomAvailability get(String val) {
			for (DynazoomAvailability g : DynazoomAvailability.values())
				if (g.val.equals(val))
					return g;
			return AUTO_DETECT;
		}
		public static DynazoomAvailability get(boolean val) {
            return val ? TRUE : FALSE;
		}
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
		if (!this.name.equals("localdomain") && !this.name.equals(""))
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
	public boolean isDynazoomAvailable(String userAgent) {
		if (this.defaultMaster || this.isEmpty())
			return false;

		MuninNode node = getChildren().get(0);
		if (node == null)
			return false;
		MuninPlugin plugin = node.getPlugin(0);
		return plugin != null && isDynazoomAvailable(plugin, userAgent);

	}

	public boolean isDynazoomAvailable(MuninPlugin plugin, String userAgent) {
		String hdGraphUrl = plugin.getHDImgUrl(Period.DAY);
		BitmapResponse res = downloadBitmap(hdGraphUrl, userAgent);

		return res.hasSucceeded();
	}

	public void rebuildChildren(MuninFoo f) {
		this.children = new ArrayList<>();
		for (MuninNode s : f.getNodes()) {
			if (s.getParent().getId() == this.id)
				this.children.add(s);
		}
		if (this.children.size() == 0)
			deleteSelf(f);
	}

	private void deleteSelf(MuninFoo f) {
		// If there's no more node under this, delete self.
		f.deleteMuninMaster(this);
	}

	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }

	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }

	public void setUrl(String url) { this.url = url; }
	public String getUrl() { return this.url; }

	public void setDynazoomAvailable(DynazoomAvailability val) { this.dynazoomAvailability = val; }
	public DynazoomAvailability isDynazoomAvailable() { return this.dynazoomAvailability; }

	public List<MuninNode> getChildren() { return this.children; }
	public void addChild(MuninNode s) {
		if (!this.children.contains(s)) {
			this.children.add(s);
			s.setParent(this);
		}
	}

	public boolean isEmpty() { return this.children.isEmpty(); }

	public boolean equalsApprox(MuninMaster p) {
		return p != null && this.url.equals(p.url);
	}

	private MuninNode getNode(String nodeUrl) {
		for (MuninNode node : this.children) {
			if (node.getUrl().equals(nodeUrl))
				return node;
		}
		return null;
	}

	public BitmapResponse downloadBitmap(String url, String userAgent) {
		return NetHelper.downloadBitmap(this, url, userAgent);
	}

	public HTMLResponse downloadUrl(String url, String userAgent) {
		return NetHelper.downloadUrl(this, url, userAgent);
	}

	/**
	 * Get the type of the given page:
	 * 	- munin/		: list of nodes
	 * 	- munin/x/		: list of plugins (not used)
	 * 	- err_code		: if error -> error code
	 * @return String : pageType
	 */
	public String detectPageType(String userAgent) {
		HTMLResponse res = downloadUrl(this.url, userAgent);
		String page = res.getHtml();
		if (!res.getAuthenticateHeader().equals("")) {
			// Digest realm="munin", nonce="39r1cMPqBAA=57afd1487ef532bfe119d40278a642533f25964e", algorithm=MD5, qop="auth"
			this.authString = res.getAuthenticateHeader();
			if (res.getAuthenticateHeader().contains("Digest"))
				this.authType = AuthType.DIGEST;
			else if (res.getAuthenticateHeader().contains("Basic"))
				this.authType = AuthType.BASIC;
		}

		if (res.hasSucceeded()) {
			if (res.wasRedirected()) {
				// Redirected from http to https
				if (res.getLastUrl().contains("https") && !this.url.contains("https"))
					this.setSSL(true);
				this.url = res.getLastUrl();
			}

			Document doc = Jsoup.parse(page, this.url);
			Elements images = doc.select("img[src$=-day.png]");

			if (images.size() == 0)
				images = doc.select("img[src$=-day.svg]");

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
					return res.getResponseCode() + " - " + res.getResponseMessage();
			}
		} else if (res.getTimeout())
			return "timeout";
		else
			return res.getResponseCode() + " - " + res.getResponseMessage();
	}

	/**
	 * Fetches the children of this MuninMaster
	 * @return How many nodes have been found
	 */
	public int fetchChildren(String userAgent) {
		int nbNodes = 0;

		// Grab HTML content
		HTMLResponse response = downloadUrl(this.url, userAgent);

		if (!response.hasSucceeded())
			return nbNodes;

		String html = response.getHtml();

		Document doc = Jsoup.parse(html, this.url);
		/*
		 * URL CATCHUP
		 * Between MfA 2.8 and 3.0, we saved Master url as http://demo.munin-monitoring.org/munin-monitoring.org/
		 * So let's update it if needed.													~~~~~~~~~~~~~~~~~~~~~
		 * So let's just check if we are on the right page, if not : update the master url.
		 */
		if (!doc.select("span.comparison").isEmpty()) {
			// Replace the current master URL
			String parentUrl = "";
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
				String htmlBis = downloadUrl(parentUrl, userAgent).getHtml();
				if (!html.equals("")) {
					html = htmlBis;
					doc = Jsoup.parse(html, this.url);
				}
			}
		}
		// URL catchup ends here

		// Check if Munin or MunStrap
		if (html.contains("MunStrap")) { // Munstrap
			Elements domains = doc.select("ul.groupview > li > a.link-domain");

			if (domains.size() > 0) {
				// If there's just one domain : take the domain name as master name.
				// Else : generate the name from host url
				if (domains.size() == 1 && !domains.get(0).text().equals("localdomain"))
					this.name = domains.get(0).text();
				else
					this.generateName();

				int previousPosition = -1;
				for (Element domain : domains) {
					// Get every host for that domain
					Elements hosts = domain.parent().select("ul>li");
					for (Element host : hosts) {
						Elements infosList = host.select("a.link-host");

						if (infosList.size() == 0)
							continue;

						Element infos = infosList.get(0);
						MuninNode serv = new MuninNode(infos.text(), infos.attr("abs:href"));
						serv.setParent(this);
						previousPosition++;
						serv.setPosition(previousPosition);
						nbNodes++;
					}
				}
			}
		} else { // Munin
			Elements domains = doc.select("span.domain");

			if (domains.size() > 0) {
				// If there's just one domain : take the domain name as master name.
				// Else : generate the name from host url
				if (domains.size() == 1 && !domains.get(0).text().equals("localdomain")) {
					Element a = domains.get(0).child(0);
					this.name = a.text();
				}
				else
					this.generateName();

				int previousPosition = -1;
				for (Element domain : domains) {
					// Get every host for that domain
					Elements hosts = domain.parent().select("span.host");
					for (Element host : hosts) {
						String nodeUrl = host.child(0).attr("abs:href");
						// Avoid duplicates for weird DOM analysis: check if it has already been added
						if (!this.has(nodeUrl)) {
							MuninNode node = new MuninNode(host.child(0).text(), nodeUrl);
							node.setParent(this);
							previousPosition++;
							node.setPosition(previousPosition);
							nbNodes++;
						}
					}
				}
			}
		}

		return nbNodes;
	}

	/**
	 * When temporary deleting an old Master, we should
	 * care about labels, widgets and grids.
	 * Here, we reattach and save the widgets.
	 * @return ArrayList<GraphWidget> : widgets who should be updated afterwards
	 */
	public List<GraphWidget> reattachWidgets(MuninFoo muninFoo, MuninMaster oldMaster) {
		List<GraphWidget> toBeUpdated_graphWidgets = new ArrayList<>();
		List<GraphWidget> graphWidgets = muninFoo.sqlite.dbHlpr.getGraphWidgets();

		if (graphWidgets.isEmpty())
			return toBeUpdated_graphWidgets;

		for (MuninNode node : oldMaster.getChildren()) {
			for (MuninPlugin plugin : node.getPlugins()) {
				// Check graphWidgets
				for (GraphWidget graphWidget : graphWidgets) {
					if (graphWidget.getPlugin().equals(plugin)) {
						// Reattach
						MuninPlugin newPlugin = this.getNode(node.getUrl()).getPlugin(graphWidget.getPlugin().getName());
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
	public List<Label> reattachLabels(MuninFoo muninFoo, MuninMaster oldMaster) {
		List<Label> toBeUpdated_labels = new ArrayList<>();
		List<Label> labels = muninFoo.labels;

		if (labels.isEmpty())
			return toBeUpdated_labels;

		for (MuninNode node : oldMaster.getChildren()) {
			for (MuninPlugin plugin : node.getPlugins()) {
				// Check labels
				for (Label label : labels) {
					ArrayList<MuninPlugin> toBeRemoved = new ArrayList<>();
					ArrayList<MuninPlugin> toBeAdded = new ArrayList<>();

					for (MuninPlugin labelPlugin : label.plugins) {
						if (labelPlugin.equals(plugin)) {
							// Reattach
							MuninPlugin newPlugin = this.getNode(node.getUrl()).getPlugin(labelPlugin.getName());
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
	public List<GridItem> reattachGrids(MuninFoo muninFoo, MuninMaster oldMaster) {
		List<GridItem> toBeUpdated_grids = new ArrayList<>();
		List<Grid> grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);

		if (grids.isEmpty())
			return toBeUpdated_grids;

		for (MuninNode node : oldMaster.getChildren()) {
			for (MuninPlugin plugin : node.getPlugins()) {
				// Check grids
				for (Grid grid : grids) {
					for (GridItem item : grid.getItems()) {
						if (item.getPlugin() == null)
							continue;

						if (item.getPlugin().equals(plugin)) {
							// Reattach
							item.setPlugin(this.getNode(node.getUrl()).getPlugin(item.getPlugin().getName()));
							toBeUpdated_grids.add(item);
						}
					}
				}
			}
		}

		return toBeUpdated_grids;
	}

	/**
	 * Contacts the URL to check if there are some other nodes / plugins for each node
	 */
	public String rescan(Context context, MuninFoo muninFoo) {
		if (isEmpty())
			return null;

		String report = "";
		int nbAddedNodes = 0;
		int nbUpdatedNodes = 0;
		int nbDeletedNodes = 0;
		int nbAddedPlugins = 0;
		int nbUpdatedPlugins = 0;
		int nbDeletedPlugins = 0;

		if (!this.defaultMaster) { // Regular master
			// Check online
			MuninMaster onlineMaster = new MuninMaster();
			onlineMaster.setUrl(this.url);
			onlineMaster.setSSL(this.ssl);
			onlineMaster.setAuthIds(this.authLogin, this.authPassword, this.authType);
			// Redetect HTTPS
			String res = onlineMaster.detectPageType(muninFoo.getUserAgent());
			if (res.equals("munin/x/") || res.equals("munin/")) {
				// SSL
				if (onlineMaster.getSSL() != this.getSSL()) {
					MuninFoo.logV("rescan", "master.SSL: " + this.getSSL() + " -> " + onlineMaster.getSSL());
					this.setSSL(onlineMaster.getSSL());
				}
				// Master url
				if (!onlineMaster.getUrl().equals(this.getUrl()) && !onlineMaster.getUrl().equals("")) {
					MuninFoo.logV("rescan", "master.url: " + this.getUrl() + " -> " + onlineMaster.getUrl());
					this.setUrl(onlineMaster.getUrl());
				}
			}

            // Set DynazoomAvailability to AUTO_DETECT to enable a new check
            onlineMaster.setDynazoomAvailable(DynazoomAvailability.AUTO_DETECT);

			onlineMaster.fetchChildren(muninFoo.getUserAgent());

			if (!onlineMaster.isEmpty()) {
				// NODES DIFF
				// Add new nodes if needed
				ArrayList<MuninNode> toBeAdded = new ArrayList<>();
				ArrayList<MuninNode> toBeUpdated = new ArrayList<>();
				// Add / update nodes
				for (MuninNode onlineNode : onlineMaster.getChildren()) {
					// Check if it is in original
					boolean alreadyThere = false;
					for (MuninNode node : this.children) {
						if (node.equalsApprox(onlineNode)) {
							alreadyThere = true;

							// Check if we can grab some attributes
                            // DynazoomAvailability
                            if (node.getParent().isDynazoomAvailable() != onlineNode.getParent().isDynazoomAvailable()) {
								node.getParent().setDynazoomAvailable(onlineNode.getParent().isDynazoomAvailable());
								if (onlineNode.getParent().isDynazoomAvailable() != DynazoomAvailability.AUTO_DETECT) {
									MuninFoo.logV("rescan", "Dynazoom availability has changed");
									if (!toBeUpdated.contains(node))
										toBeUpdated.add(node);
								}
							}

							// HDGraphURL
							if (!node.getHdGraphURL().equals(onlineNode.getHdGraphURL()) && !onlineNode.getHdGraphURL().equals("")) {
								MuninFoo.logV("rescan", "HDGraphUrl has changed");
								node.setHdGraphURL(onlineNode.getHdGraphURL());
								if (!toBeUpdated.contains(node))
									toBeUpdated.add(node);
							}

							// Node URL (http => https)
							if (!node.getUrl().equals(onlineNode.getUrl()) && !onlineNode.getUrl().equals("")) {
								MuninFoo.logV("rescan", "node url has changed");
								node.setUrl(onlineNode.getUrl());
								if (!toBeUpdated.contains(node))
									toBeUpdated.add(node);
							}

							// Node position
							if (node.getPosition() != onlineNode.getPosition() && onlineNode.getPosition() != -1) {
								MuninFoo.logV("rescan", "node position has changed");
								node.setPosition(onlineNode.getPosition());
								if (!toBeUpdated.contains(node))
									toBeUpdated.add(node);
							}

							break;
						}
					}

					// There node isn't already there
					if (!alreadyThere)
						toBeAdded.add(onlineNode);
				}

				// Save MuninMaster
				muninFoo.sqlite.dbHlpr.updateMuninMaster(this);

				// Save nodes changes
				for (MuninNode node : toBeAdded) {
					addChild(node);
					muninFoo.addNode(node);
					muninFoo.sqlite.dbHlpr.insertMuninNode(node);
					nbAddedNodes++;
				}
				toBeAdded.clear();

				for (MuninNode node : toBeUpdated) {
					muninFoo.sqlite.dbHlpr.updateMuninNode(node);
					nbUpdatedNodes++;
				}
				toBeUpdated.clear();

				// Remove offline nodes if needed
				ArrayList<MuninNode> toBeRemoved = new ArrayList<>();
				for (MuninNode oldNode : this.children) {
					// Check if it is still there
					boolean stillThere = false;
					for (MuninNode node : onlineMaster.getChildren()) {
						if (node.equalsApprox(oldNode)) {
							stillThere = true;
							break;
						}
					}

					// The node has been deleted in meantime
					if (!stillThere)
						toBeRemoved.add(oldNode);
				}

				for (MuninNode node : toBeRemoved) {
					this.children.remove(node);
					muninFoo.getNodes().remove(node);
					muninFoo.sqlite.dbHlpr.deleteNode(node);
					nbDeletedNodes++;
				}
				toBeRemoved.clear();

				// Nodes are now synced.
				// PLUGINS DIFF
				for (MuninNode node : this.children) {
                    // Force HD graph URL rescan
                    node.setHdGraphURL(null);

					boolean graphUrlChanged = false;
					String oldGraphUrl = node.getGraphURL();

					List<MuninPlugin> onlinePlugins = node.getPluginsList(muninFoo.getUserAgent());

					// Detect graphUrl change
					String newGraphUrl = node.getGraphURL();
					if (!oldGraphUrl.equals(newGraphUrl))
						graphUrlChanged = true;

					// If the download hasn't failed
					if (onlinePlugins != null && !onlinePlugins.isEmpty()) {
						// Add new plugins
						ArrayList<MuninPlugin> pluginsToBeAdded = new ArrayList<>();
						ArrayList<MuninPlugin> pluginsToBeUpdated = new ArrayList<>();
						for (MuninPlugin onlinePlugin : onlinePlugins) {
							boolean alreadyThere = false;
							for (MuninPlugin oldPlugin : node.getPlugins()) {
								if (oldPlugin.equalsApprox(onlinePlugin)) {
									alreadyThere = true;

									// Get other values
									// Plugin category
									if (!oldPlugin.getCategory().equals(onlinePlugin.getCategory()) && !onlinePlugin.getCategory().equals("")) {
										oldPlugin.setCategory(onlinePlugin.getCategory());
										pluginsToBeUpdated.add(oldPlugin);
									}

                                    // Position
                                    if (oldPlugin.getPosition() != onlinePlugin.getPosition()) {
                                        oldPlugin.setPosition(onlinePlugin.getPosition());

                                        if (!pluginsToBeUpdated.contains(oldPlugin))
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
							node.addPlugin(plugin);
							muninFoo.sqlite.dbHlpr.insertMuninPlugin(plugin);
							nbAddedPlugins++;
						}
						for (MuninPlugin plugin : pluginsToBeUpdated) {
							muninFoo.sqlite.dbHlpr.updateMuninPlugin(plugin);
							nbUpdatedPlugins++;
						}


						// Remove deleted plugins
						ArrayList<MuninPlugin> pluginsToBeRemoved = new ArrayList<>();
						for (MuninPlugin oldPlugin : node.getPlugins()) {
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
							node.getPlugins().remove(plugin);
							muninFoo.sqlite.dbHlpr.deletePlugin(plugin);
							nbDeletedPlugins++;
						}

						if (graphUrlChanged)
							muninFoo.sqlite.dbHlpr.updateMuninNode(node);
					}
				}
			}
		}

		// Generate report
		if (nbAddedNodes + nbUpdatedNodes + nbDeletedNodes
				+ nbAddedPlugins + nbUpdatedPlugins + nbDeletedPlugins == 0)
			report = context.getString(R.string.sync_nochange);
		else {
			// Nodes
			if (nbAddedNodes > 0)
				report += context.getString(R.string.sync_serversadded).replace("XXX", String.valueOf(nbAddedNodes));
			if (nbUpdatedNodes > 0)
				report += context.getString(R.string.sync_serversupdated).replace("XXX", String.valueOf(nbUpdatedNodes));
			if (nbDeletedNodes > 0)
				report += context.getString(R.string.sync_serversdeleted).replace("XXX", String.valueOf(nbDeletedNodes));

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

	/**
	 * Checks if this master contains a node, based on its URL
	 * @param nodeUrl Node URL
	 * @return boolean
	 */
	public boolean has(String nodeUrl) {
		for (MuninNode node : this.children) {
			if (node.getUrl().equals(nodeUrl))
				return true;
		}
		return false;
	}
}
