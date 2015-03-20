package com.chteuchteu.munin.async;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin;

public class FieldsDescriptionFetcher extends AsyncTask<Void, Integer, Void> {
    private MuninPlugin plugin;
    private Context context;
    private Activity activity;
    private String html;
    private ProgressDialog dialog;

    public FieldsDescriptionFetcher (MuninPlugin plugin, Activity activity) {
        this.plugin = plugin;
        this.context = activity;
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        this.dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        this.html = plugin.getFieldsDescriptionHtml(MuninFoo.getInstance().getUserAgent());

        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostExecute(Void result) {
        this.dialog.dismiss();

        if (this.html != null) {
            if (!this.html.equals("")) {
                // Prepare HTML
                String wrappedHtml = "<head><style>" +
                        "td { padding: 5px 10px; margin: 1px;border-bottom: 1px solid #d8d8d8; min-width: 30px; }" +
                        "td.lastrow { border-bottom-width: 0px; } th { border-bottom: 1px solid #999; }" +
                        "</style>" +
                        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                        "</head>" +
                        "<body>" + html + "</body>";

                // Inflate and populate view
                LayoutInflater inflater = LayoutInflater.from(context);
                View customView = inflater.inflate(R.layout.dialog_webview, null);
                WebView webView = (WebView) customView.findViewById(R.id.webview);
                webView.setVerticalScrollBarEnabled(true);
                webView.getSettings().setDefaultTextEncodingName("utf-8");
                webView.setBackgroundColor(0x00000000);
                webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "utf-8", null);
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

                // Create alertdialog
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(customView);
                builder.setTitle(R.string.fieldsDescription);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(context, R.string.text81, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
        }
    }
}
