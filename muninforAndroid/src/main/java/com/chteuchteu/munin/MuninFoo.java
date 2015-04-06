package com.chteuchteu.munin;

import android.content.Context;
import android.util.Log;

import com.chteuchteu.munin.exc.NullMuninFooException;
import com.chteuchteu.munin.exc.TrialExpirationDateReached;
import com.chteuchteu.munin.hlpr.ChromecastHelper;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Main class of the app. This singleton is created on app launch
 * Its instance is retrieved using MuninFoo.getInstance().
 *  (we supply a Context reference on first getInstance call)
 */
public class MuninFoo {
	private static MuninFoo instance;
	public boolean languageLoaded = false;
	
	private List<MuninNode> nodes;
	public List<Label> labels;
	public List<MuninMaster> masters;
	
	public SQLite sqlite;
	private MuninNode currentNode;

	private String userAgent;

	public static final double VERSION = 5.7;
	private static final boolean FORCE_NOT_PREMIUM = false;

    // Allows an user to test the app until the TRIAL_EXPIRATION date is reached
    private static final boolean TRIAL = false;
    private static final Calendar TRIAL_EXPIRATION = new GregorianCalendar(2015, 1, 10);

    public boolean premium;

	public Calendar alerts_lastUpdated;

	public ChromecastHelper chromecastHelper;
	
	private MuninFoo(Context context) {
		premium = false;
		nodes = new ArrayList<>();
		labels = new ArrayList<>();
		masters = new ArrayList<>();
		sqlite = new SQLite(context, this);
		instance = null;

		// User agent
		String userAgentPref = Util.getPref(context, Util.PrefKeys.UserAgent);
		this.userAgent = userAgentPref.equals("") ? generateUserAgent(context) : userAgentPref;

		loadInstance(context);

        if (TRIAL && isTrialExpired())
            throw new RuntimeException(new TrialExpirationDateReached("Trial has expired"));
	}
	
	public static boolean isLoaded() { return instance != null; }
	
	private void loadInstance(Context context) {
		this.masters = sqlite.dbHlpr.getMasters();
		this.nodes = sqlite.dbHlpr.getNodes(this.masters);
		this.labels = sqlite.dbHlpr.getLabels(this.masters);

		attachOrphanNodes();

		if (BuildConfig.DEBUG)
			this.sqlite.logMasters();

		if (context != null) {
			// Set default node
			this.currentNode = getCurrentNode(context);
			
			this.premium = isPremium(context);
		}
	}

	public MuninNode getCurrentNode() { return getCurrentNode(null); }
	public MuninNode getCurrentNode(Context context) {
		updateCurrentNode(context);
		return this.currentNode;
	}
	public void updateCurrentNode(Context context) {
		if (this.currentNode != null)
			return;

		if (this.nodes.isEmpty())
			return;

		if (context != null) {
			String defaultNodeUrl = Util.getPref(context, Util.PrefKeys.DefaultNode);

			if (!defaultNodeUrl.equals("")) {
				MuninNode defaultNode = getNode(defaultNodeUrl);
				if (defaultNode != null) {
					this.currentNode = defaultNode;
					return;
				}
			}
		}

		// Failed to find the defaultNode
		if (this.nodes.isEmpty()) {
			this.currentNode = null;
			return;
		}

		this.currentNode = this.nodes.get(0);
	}
	public void setCurrentNode(MuninNode node) { this.currentNode = node; }
	
	public void resetInstance(Context context) {
		nodes = new ArrayList<>();
		labels = new ArrayList<>();
		sqlite = new SQLite(context, this);
		loadInstance(context);
	}
	
	public static synchronized MuninFoo getInstance() {
		if (instance == null)
			Crashlytics.logException(
					new NullMuninFooException("getInstance() called without Context for the first time"));
		return instance;
	}
	
	public static synchronized MuninFoo getInstance(Context context) {
		if (instance == null)
			instance = new MuninFoo(context);
		return instance;
	}
	
	/**
	 * Set a common parent to the nodes which does not
	 * have one after getting them
	 */
	private void attachOrphanNodes() {
		int n = 0;
		MuninMaster defMaster = new MuninMaster();
		defMaster.setName("Default");
		defMaster.defaultMaster = true;
		
		for (MuninNode s : this.nodes) {
			if (s.getParent() == null) {
				s.setParent(defMaster);
				n++;
			}
		}
		
		if (n > 0)
			this.masters.add(defMaster);
	}
	
	public void addNode(MuninNode node) {
		boolean contains = false;
		int pos = -1;
		for (int i=0; i<nodes.size(); i++) {
			if (nodes.get(i) != null && nodes.get(i).equalsApprox(node)) {
				contains = true; pos = i; break;
			}
		}
		if (contains) // Replacement
			nodes.set(pos, node);
		else
			nodes.add(node);
	}
	public void deleteNode(MuninNode s) {
		s.getParent().rebuildChildren(this);
		
		// Delete from nodes list
		this.nodes.remove(s);
		
		s.getParent().rebuildChildren(this);
		
		// Update current node
		if (this.currentNode.equals(s) && this.nodes.size() > 0)
			this.currentNode = this.nodes.get(0);
	}
	public void deleteMuninMaster(MuninMaster master) { deleteMuninMaster(master, null); }
	public void deleteMuninMaster(MuninMaster master, Util.ProgressNotifier progressNotifier) {
		if (this.masters.remove(master)) {
			sqlite.dbHlpr.deleteMaster(master, true, progressNotifier);
			
			// Remove labels relations for the current session
			for (MuninNode node : master.getChildren()) {
				for (MuninPlugin plugin : node.getPlugins())
					removeLabelRelation(plugin);
			}
			
			this.nodes.removeAll(master.getChildren());
		}
	}
	public boolean addLabel(Label l) {
		boolean contains = false;
		for (Label ml : labels) {
			if (ml.getName().equals(l.getName())) {
				contains = true; break;
			}
		}
		if (!contains) {
			labels.add(l);
			sqlite.saveLabels();
		}
		return !contains;
	}
	public boolean removeLabel(Label label) {
		List<Label> list = new ArrayList<>();
		boolean somthingDeleted = false;
		for (Label l : labels) {
			if (!l.equals(label))
				list.add(l);
			else
				somthingDeleted = true;
		}
		labels = list;
		if (somthingDeleted)
			sqlite.saveLabels();
		return somthingDeleted;
	}
	/**
	 * When removing a plugin, delete its label relations.
	 * Warning : this does not deletes it from the local db.
	 * @param plugin MuninPlugin
	 */
	public void removeLabelRelation(MuninPlugin plugin) {
		for (Label label : this.labels) {
			for (MuninPlugin labelPlugin : label.plugins) {
				if (labelPlugin.equals(plugin)) {
					label.plugins.remove(labelPlugin);
					return;
				}
			}
		}
	}

	public List<MuninNode> getNodes() {
		return this.nodes;
	}
	public MuninNode getNode(int pos) {
		if (pos >= 0 && pos < nodes.size())
			return nodes.get(pos);
		else
			return null;
	}
	public MuninNode getNode(String url) {
		for (MuninNode s : nodes) {
			if (s.getUrl().equals(url))
				return s;
		}
		return null;
	}
	public MuninPlugin getPlugin(int id) {
		for (MuninNode node : nodes) {
			for (MuninPlugin plugin : node.getPlugins()) {
				if (plugin.getId() == id)
					return plugin;
			}
		}
		return null;
	}
	
	public List<MuninMaster> getMasters() { return this.masters; }

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
	
	public Label getLabel(String lname) {
		for (Label l : labels) {
			if (l.getName().equals(lname))
				return l;
		}
		return null;
	}

	public Label getLabel(long labelId) {
		for (Label label : labels) {
			if (label.getId() == labelId)
				return label;
		}
		return null;
	}
	
	public boolean contains(MuninMaster master) {
		for (MuninMaster m : masters) {
			if (m.equalsApprox(master))
				return true;
		}
		return false;
	}
	
	public List<List<MuninNode>> getGroupedNodesList() {
		List<List<MuninNode>> l = new ArrayList<>();
		for (MuninMaster master : masters) {
			List<MuninNode> nodesList = new ArrayList<>();
			nodesList.addAll(master.getChildren());
			l.add(nodesList);
		}
		return l;
	}
	
	/**
	 * Returns true if we should retrieve nodes information
	 *  We consider alerts information as outdated after 10 minutes.
	 *  Note : hitting refresh on alerts screen forces data refresh.
	 * @return bool
	 */
	public boolean shouldUpdateAlerts() {
		if (alerts_lastUpdated == null) {
			alerts_lastUpdated = Calendar.getInstance();
			return true;
		}
		
		Calendar updateTreshold = Calendar.getInstance();
		updateTreshold.add(Calendar.MINUTE, -10);
		
		// If the last time the information was retrieved is before
		// now -10 minutes, we should update it again.
		
		return alerts_lastUpdated.before(updateTreshold);
	}

	public static void log(String msg) { log("Munin for Android", msg); }
	public static void log(String tag, String msg) { if (BuildConfig.DEBUG) Log.i(tag, msg); }
	public static void logV(String msg) { logV("Munin for Android", msg); }
	public static void logV(String tag, String msg) { if (BuildConfig.DEBUG) Log.v(tag, msg); }
	public static void logE(String msg) { logE("Munin for Android", msg); }
	public static void logE(String tag, String msg) { if (BuildConfig.DEBUG) Log.e(tag, msg); }
	public static void logW(String msg) { logW("Munin for Android", msg); }
	public static void logW(String tag, String msg) { if (BuildConfig.DEBUG) Log.w(tag, msg); }

	public static boolean isPremium(Context c) {
        if (TRIAL)
            return true;

		if (Util.isPackageInstalled("com.chteuchteu.muninforandroidfeaturespack", c)) {
			if (BuildConfig.DEBUG && FORCE_NOT_PREMIUM)
				return false;
			if (BuildConfig.DEBUG)
				return true;
			//PackageManager manager = c.getPackageManager();
			//return (manager.checkSignatures("com.chteuchteu.munin", "com.chteuchteu.muninforandroidfeaturespack")
			//		== PackageManager.SIGNATURE_MATCH);
			return true;
		}
		return false;
	}

    public static boolean isTrialExpired() {
        Calendar today = Calendar.getInstance();
        return today.after(TRIAL_EXPIRATION);
    }

	/**
	 * Generates "MuninForAndroid/3.0 (Android 4.4.4 KITKAT)" from context
	 * @param context Application/activity context
	 */
	public static String generateUserAgent(Context context) {
		if (context == null)
			return generateUserAgent();

		String appVersion = Util.getAppVersion(context);
		String androidVersion = Util.getAndroidVersion();
		String userAgent = "MuninForAndroid/" + appVersion + " (" + androidVersion + ")";
		log("User agent : " + userAgent);
		return userAgent;
	}

	/**
	 * Context-less version of generateUserAgent(Context context)
	 * Generates "MuninForAndroid (Android 4.4.4 KITKAT)"
	 */
	private static String generateUserAgent() {
		String androidVersion = Util.getAndroidVersion();
		String userAgent = "MuninForAndroid (" + androidVersion + ")";
		log("User agent : " + userAgent);
		return userAgent;
	}

	public String getUserAgent() { return this.userAgent; }
	/**
	 * Get user agent in a context where MuninFoo isn't probably loaded
	 * @param context Valid context (not nullable)
	 * @return User Agent
	 */
	public static String getUserAgent(Context context) {
		String userAgentPref = Util.getPref(context, Util.PrefKeys.UserAgent);
		return userAgentPref.equals("") ? generateUserAgent(context) : userAgentPref;
	}
	public void setUserAgent(String val) { this.userAgent = val; }
}
