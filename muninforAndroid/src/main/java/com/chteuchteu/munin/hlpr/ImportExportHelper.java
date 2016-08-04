package com.chteuchteu.munin.hlpr;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.exc.ImportExportWebserviceException;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.HTTPResponse.HTMLResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.ui.IImportExportActivity;
import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImportExportHelper {
	public static final String ENCRYPTION_SEED = "786547E9431EE";

    /**
     * Default import/export target URI. Can be overridden by user.
     */
    public static final String IMPORT_EXPORT_URI = "https://ws.munin-for-android.com/importExport.php";
    public static final int IMPORT_EXPORT_VERSION = 1;

	public enum ImportExportType { MASTERS, GRIDS }
	
	public static class Export {
		private static String sendExportRequest(Context context, String jsonString, ImportExportType type) {
			List<Pair<String, String>> params = new ArrayList<>();
			params.add(new Pair<>("dataString", jsonString));
			params.add(new Pair<>("version", String.valueOf(IMPORT_EXPORT_VERSION)));
			params.add(new Pair<>("dataType", type.name().toLowerCase()));

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
			private ImportExportType type;
			private boolean result;
			private String pswd;
			private ProgressDialog progressDialog;
			private IImportExportActivity activity;
			private Context context;
			
			public ExportRequestMaker (String jsonString, ImportExportType type,
			                           Context context, IImportExportActivity activity) {
				super();
				this.jsonString = jsonString;
				this.type = type;
				this.result = false;
				this.activity = activity;
				this.context = context;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    pswd = sendExportRequest(context, jsonString, type);
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
					activity.onExportSuccess(pswd);
				else
					activity.onExportError();
			}
		}
	}
	
	public static class Import {
		public static void applyMastersImport(Context context, JSONObject jsonObject, String code) {
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

		public static void applyGridsImport(Context context, JSONObject jsonObject, List<String> currentGridsNames) {
			MuninFoo muninFoo = MuninFoo.getInstance(context);
			List<Grid> newGrids = JSONHelper.getGridsFromJSON(jsonObject, muninFoo);
			removeIdsFromGrids(currentGridsNames, newGrids);

			for (Grid grid : newGrids) {
				grid.setId(muninFoo.sqlite.dbHlpr.insertGrid(grid.getName()));
				muninFoo.sqlite.dbHlpr.insertGridItemRelations(grid.getItems());
			}
		}
		
		private static JSONObject sendImportRequest(Context context, String code, ImportExportType type) {
			List<Pair<String, String>> params = new ArrayList<>();
			params.add(new Pair<>("pswd", code));
			params.add(new Pair<>("version", String.valueOf(IMPORT_EXPORT_VERSION)));
			params.add(new Pair<>("dataType", type.name().toLowerCase()));

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
			private ImportExportType type;
			private String code;
			private boolean result;
			private IImportExportActivity activity;
			private Context context;
			private ProgressDialog progressDialog;
			
			public ImportRequestMaker (String code, Context context,
			                           ImportExportType type, IImportExportActivity activity) {
				super();
				this.code = code;
				this.type = type;
				this.result = false;
				this.activity = activity;
				this.context = context;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    jsonObject = sendImportRequest(context, code, type);
                    result = jsonObject != null;
                } catch (IllegalStateException ex) {
                    // Thrown when the URL isn't valid
                    result = false;
                }
				
				if (result) {
					switch (type) {
						case MASTERS:
							applyMastersImport(context, jsonObject, ENCRYPTION_SEED);
							break;
						case GRIDS:
							List<String> currentGridsNames = MuninFoo.getInstance().sqlite.dbHlpr.getGridsNames();
							applyGridsImport(context, jsonObject, currentGridsNames);
					}
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					activity.onImportSuccess();
				else
					activity.onImportError();
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

	/**
	 * Removes ids from grids and resolve possible conflicted names
	 */
	private static void removeIdsFromGrids(List<String> currentGridNames, List<Grid> grids) {
		// Remove ids
		for (Grid grid : grids) {
			grid.setId(-1);
			for (GridItem item : grid.getItems())
				item.setId(-1);
		}

		// Fix possible conflicts in names
		for (Grid grid : grids) {
			// Find a grid with this name

			if (currentGridNames.contains(grid.getName()))
				grid.setName(grid.getName() + " (2)");
		}
	}

    public static String getImportExportServerUrl(Context context) {
		String oldUrl = "http://www.munin-for-android.com/ws/importExport.php";
        String url = Settings.getInstance(context).getString(Settings.PrefKeys.ImportExportServer, IMPORT_EXPORT_URI);

		return url.equals(oldUrl) ? IMPORT_EXPORT_URI : url;
    }

	public static void showExportDialog(MuninFoo muninFoo, final Context context,
	                                    final ImportExportType type, final IImportExportActivity activity) {
		if (!muninFoo.premium) {
			Toast.makeText(context, R.string.featuresPackNeeded, Toast.LENGTH_SHORT).show();
			return;
		}

		new AlertDialog.Builder(context)
				.setTitle(R.string.action_export)
				.setMessage(R.string.export_explanation)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MuninFoo muninFoo = MuninFoo.getInstance(context);
						String json = type == ImportExportType.MASTERS
							? JSONHelper.getMastersJSONString(muninFoo.getMasters(), ImportExportHelper.ENCRYPTION_SEED)
							: JSONHelper.getGridsJSONString(muninFoo.sqlite.dbHlpr.getGrids(muninFoo));
						if (json.equals(""))
							Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show();
						else
							new Export.ExportRequestMaker(json, type, context, activity).execute();
					}
				})
				.setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
	}

	public static void showImportDialog(MuninFoo muninFoo, final Context context,
	                                    final ImportExportType type, final IImportExportActivity activity) {
		if (!muninFoo.premium) {
			Toast.makeText(context, R.string.featuresPackNeeded, Toast.LENGTH_SHORT).show();
			return;
		}

		final View dialogView = View.inflate(context, R.layout.dialog_import, null);
		new AlertDialog.Builder(context)
				.setTitle(R.string.import_title)
				.setView(dialogView)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String code = ((EditText) dialogView.findViewById(R.id.import_code)).getText().toString();
						code = code.toLowerCase();
						new Import.ImportRequestMaker(code, context, type, activity).execute();
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.text64, null)
				.show();
	}
}
