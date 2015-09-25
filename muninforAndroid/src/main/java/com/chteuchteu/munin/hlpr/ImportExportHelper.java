package com.chteuchteu.munin.hlpr;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.exc.ImportExportWebserviceException;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImportExportHelper {
	public static final String ENCRYPTION_SEED = "786547E9431EE";

    /**
     * Default import/export target URI. Can be overriden by user.
     */
    public static final String IMPORT_EXPORT_URI = "http://ws.munin-for-android.com/importExport.php";
    public static final int IMPORT_EXPORT_VERSION = 1;
	
	public static class Export {
		private static String sendExportRequest(Context context, String jsonString) {
			List<Pair<String, String>> params = new ArrayList<>();
			params.add(new Pair<>("dataString", jsonString));
			params.add(new Pair<>("version", String.valueOf(IMPORT_EXPORT_VERSION)));

			String url = getImportExportServerUrl(context) + "?export";
			String userAgent = MuninFoo.getInstance().getUserAgent();
			
			try {
				HTMLResponse response = NetHelper.simplePost(url, params, userAgent);
				JSONObject jsonResult = new JSONObject(response.getHtml());

				boolean success = jsonResult.getBoolean("success");

				if (success) {
					return jsonResult.getString("password");
				} else {
					String error = jsonResult.getString("error");
					Crashlytics.logException(new ImportExportWebserviceException("Error is " + error));
				}
				
				return null;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public static class ExportRequestMaker extends AsyncTask<Void, Integer, Void> {
			private String jsonString;
			private boolean result;
			private String pswd;
			private ProgressDialog progressDialog;
			private Activity_Servers activity_servers;
			private Context context;
			
			public ExportRequestMaker (String jsonString, Activity_Servers activity) {
				super();
				this.jsonString = jsonString;
				this.result = false;
				this.activity_servers = activity;
				this.context = activity;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    pswd = sendExportRequest(context, jsonString);
                    result = pswd != null && !pswd.equals("");
                } catch (IllegalStateException ex) {
                    // Thrown when the URL isn't valid
                    result = false;
                }
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					activity_servers.onExportSuccess(pswd);
				else
					activity_servers.onExportError();
			}
		}
	}
	
	public static class Import {
		public static void applyImportation(Context context, JSONObject jsonObject, String code) {
			List<MuninMaster> newMasters = JSONHelper.getMastersFromJSON(jsonObject, code);
			removeIds(newMasters);

			MuninFoo muninFooRef = MuninFoo.getInstance(context);
			
			// Add masters
			for (MuninMaster newMaster : newMasters) {
				// Check if master already added
				if (!muninFooRef.contains(newMaster)) {
					muninFooRef.getMasters().add(newMaster);
					for (MuninNode node : newMaster.getChildren())
						muninFooRef.addNode(node);

					muninFooRef.sqlite.insertMuninMaster(newMaster);
				}
			}
		}
		
		private static JSONObject sendImportRequest(Context context, String code) {
			List<Pair<String, String>> params = new ArrayList<>();
			params.add(new Pair<>("pswd", code));
			params.add(new Pair<>("version", String.valueOf(IMPORT_EXPORT_VERSION)));

			String url = getImportExportServerUrl(context) + "?import";
			String userAgent = MuninFoo.getInstance().getUserAgent();

			try {
				HTMLResponse response = NetHelper.simplePost(url, params, userAgent);
				JSONObject jsonResult = new JSONObject(response.getHtml());
				
				boolean success = jsonResult.getBoolean("success");
				
				if (success) {
					return jsonResult.getJSONArray("data").getJSONObject(0);
				} else {
					String error = jsonResult.getString("error");
					if (!error.equals("006")) // Wrong password
						Crashlytics.logException(new ImportExportWebserviceException("Error is " + error));
				}
				
				return null;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public static class ImportRequestMaker extends AsyncTask<Void, Integer, Void> {
			private JSONObject jsonObject;
			private String code;
			private boolean result;
			private Activity_Servers activity_servers;
			private Context context;
			private ProgressDialog progressDialog;
			
			public ImportRequestMaker (String code, Activity_Servers activity) {
				super();
				this.code = code;
				this.result = false;
				this.activity_servers = activity;
				this.context = activity;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    jsonObject = sendImportRequest(context, code);
                    result = jsonObject != null;
                } catch (IllegalStateException ex) {
                    // Thrown when the URL isn't valid
                    result = false;
                }
				
				if (result)
					applyImportation(context, jsonObject, ENCRYPTION_SEED);
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					activity_servers.onImportSuccess();
				else
					activity_servers.onImportError();
			}
		}
	}
	
	/**
	 * Removes the ids contained in the structure given as parameter
	 */
	private static void removeIds(List<MuninMaster> masters) {
		for (MuninMaster master : masters) {
			master.setId(-1);
			for (MuninNode node : master.getChildren()) {
				node.setId(-1);
				node.isPersistant = false;
				for (MuninPlugin plugin : node.getPlugins())
					plugin.setId(-1);
			}
		}
	}

    public static String getImportExportServerUrl(Context context) {
		String oldUrl = "http://www.munin-for-android.com/ws/importExport.php";
        String url = Settings.getInstance(context).getString(Settings.PrefKeys.ImportExportServer, IMPORT_EXPORT_URI);

		return url.equals(oldUrl) ? IMPORT_EXPORT_URI : url;
    }
}
