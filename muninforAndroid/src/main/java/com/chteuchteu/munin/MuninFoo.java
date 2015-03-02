package com.chteuchteu.munin;

import android.content.Context;
import android.util.Log;

import com.chteuchteu.munin.exc.NullMuninFooException;
import com.chteuchteu.munin.exc.TrialExpirationDateReached;
import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
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
	
	private List<MuninServer> servers;
	public List<Label> labels;
	public List<MuninMaster> masters;
	
	public SQLite sqlite;
	private MuninServer currentServer;

	private String userAgent;
	
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
	//							| 3.1		3.2		3.2.1   3.3		3.4		3.4.5	3.4.6	3.4.7	3.4.8	3.4.9	3.5											|
	//							| 34		35		36		37		38		39		40		41		42		43		44											|
	//							| 4.3		4.4		4.5		4.6		4.7		4.8		4.9		5.0		5.1		5.2		5.3						    				|
	//							| fix				fix								fix		fix		fix															|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	
	public static final double VERSION = 5.3;
	// =============== //
	private static final boolean FORCE_NOT_PREMIUM = false;

    // Allows an user to test the app until the TRIAL_EXPIRATION date is reached
    private static final boolean TRIAL = false;
    private static final Calendar TRIAL_EXPIRATION = new GregorianCalendar(2015, 1, 10);

    public boolean premium;
	
	// Import/Export webservice
	public static final String IMPORT_EXPORT_URI = "http://www.munin-for-android.com/ws/importExport.php";
	public static final int IMPORT_EXPORT_VERSION = 1;

	// Chromecast
	public static final String CHROMECAST_APPLICATION_ID = "31C83628";
	public static final String CHROMECAST_CHANNEL_NAMESPACE = "urn:x-cast:com.chteuchteu.munin";
	
	public Calendar alerts_lastUpdated;
	
	private MuninFoo(Context context) {
		premium = false;
		servers = new ArrayList<>();
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
		this.servers = sqlite.dbHlpr.getServers(this.masters);
		this.labels = sqlite.dbHlpr.getLabels(this.masters);

		attachOrphanServers();

		if (BuildConfig.DEBUG)
			this.sqlite.logMasters();

		if (context != null) {
			// Set default server
			this.currentServer = getCurrentServer(context);
			
			this.premium = isPremium(context);
		}
	}

	public MuninServer getCurrentServer() { return getCurrentServer(null); }
	public MuninServer getCurrentServer(Context context) {
		updateCurrentServer(context);
		return this.currentServer;
	}
	public void updateCurrentServer(Context context) {
		if (this.currentServer != null)
			return;

		if (this.servers.isEmpty())
			return;

		if (context != null) {
			String defaultServerUrl = Util.getPref(context, Util.PrefKeys.DefaultServer);

			if (!defaultServerUrl.equals("")) {
				MuninServer defaultServer = getServer(defaultServerUrl);
				if (defaultServer != null) {
					this.currentServer = defaultServer;
					return;
				}
			}
		}

		// Failed to find the defaultServer
		if (this.servers.isEmpty()) {
			this.currentServer = null;
			return;
		}

		this.currentServer = this.servers.get(0);
	}
	public void setCurrentServer(MuninServer server) { this.currentServer = server; }
	
	public void resetInstance(Context context) {
		servers = new ArrayList<>();
		labels = new ArrayList<>();
		sqlite = new SQLite(context, this);
		loadInstance(context);
	}
	
	public static synchronized MuninFoo getInstance() {
		if (instance == null)
			Crashlytics.logException(
					new NullMuninFooException("getInstante() called without Context for the first time"));
		return instance;
	}
	
	public static synchronized MuninFoo getInstance(Context context) {
		if (instance == null)
			instance = new MuninFoo(context);
		return instance;
	}
	
	/**
	 * Set a common parent to the servers which does not
	 * have one after getting them
	 */
	private void attachOrphanServers() {
		int n = 0;
		MuninMaster defMaster = new MuninMaster();
		defMaster.setName("Default");
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
	
	public void addServer(MuninServer server) {
		boolean contains = false;
		int pos = -1;
		for (int i=0; i<servers.size(); i++) {
			if (servers.get(i) != null && servers.get(i).equalsApprox(server)) {
				contains = true; pos = i; break;
			}
		}
		if (contains) // Replacement
			servers.set(pos, server);
		else
			servers.add(server);
	}
	public void deleteServer(MuninServer s) {
		s.getParent().rebuildChildren(this);
		
		// Delete from servers list
		this.servers.remove(s);
		
		s.getParent().rebuildChildren(this);
		
		// Update current server
		if (this.currentServer.equals(s) && this.servers.size() > 0)
			this.currentServer = this.servers.get(0);
	}
	public void deleteMuninMaster(MuninMaster master) { deleteMuninMaster(master, null); }
	public void deleteMuninMaster(MuninMaster master, Util.ProgressNotifier progressNotifier) {
		if (this.masters.remove(master)) {
			sqlite.dbHlpr.deleteMaster(master, true, progressNotifier);
			
			// Remove labels relations for the current session
			for (MuninServer server : master.getChildren()) {
				for (MuninPlugin plugin : server.getPlugins())
					removeLabelRelation(plugin);
			}
			
			this.servers.removeAll(master.getChildren());
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

	public List<MuninServer> getServers() {
		return this.servers;
	}
	public MuninServer getServer(int pos) {
		if (pos >= 0 && pos < servers.size())
			return servers.get(pos);
		else
			return null;
	}
	public List<MuninServer> getServersFromPlugin(MuninPlugin pl) {
		List<MuninServer> l = new ArrayList<>();
		for (MuninServer s : this.servers) {
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
	public MuninPlugin getPlugin(int id) {
		for (MuninServer server : servers) {
			for (MuninPlugin plugin : server.getPlugins()) {
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
	
	public List<List<MuninServer>> getGroupedServersList() {
		List<List<MuninServer>> l = new ArrayList<>();
		for (MuninMaster master : masters) {
			List<MuninServer> serversList = new ArrayList<>();
			serversList.addAll(master.getChildren());
			l.add(serversList);
		}
		return l;
	}
	
	/**
	 * Returns true if we should retrieve servers information
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
