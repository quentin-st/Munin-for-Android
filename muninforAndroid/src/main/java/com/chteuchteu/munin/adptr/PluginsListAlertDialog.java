package com.chteuchteu.munin.adptr;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PluginsListAlertDialog {
    private AlertDialog dialog;

    public PluginsListAlertDialog(Context context, View attachTo, MuninNode node,
                                  final PluginsListAlertDialogClick onItemClick) {
        // Init
        LinearLayout view = new LinearLayout(context);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setOrientation(LinearLayout.VERTICAL);

        ListView listView = new ListView(context);
        // Create plugins list
        final List<List<MuninPlugin>> list = node.getPluginsListWithCategory();

        Adapter_SeparatedList adapter = new Adapter_SeparatedList(context, true);
        for (List<MuninPlugin> l : list) {
            List<Map<String,?>> elements = new LinkedList<>();
            String categoryName = "";
            for (MuninPlugin p : l) {
                elements.add(createItem(p.getFancyName()));
                categoryName = p.getCategory();
            }

            adapter.addSection(categoryName, new SimpleAdapter(context, elements, R.layout.plugins_serverlist_server,
                    new String[] { "title" }, new int[] { R.id.server }));
        }
        listView.setAdapter(adapter);
        listView.setDivider(null);

        view.addView(listView);

        AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(view);
        dialog = builder.create();
        // Set AlertDialog position and width
        Rect spinnerPos = Util.locateView(attachTo);
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.START;
        wmlp.x = spinnerPos.left;
        wmlp.y = spinnerPos.top;
        wmlp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        wmlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                // Category name lines are taken in account in the positions list.
                // Let's find the plugin
                int i = 0;
                for (List<MuninPlugin> l : list) {
                    i++;
                    for (MuninPlugin plugin : l) {
                        if (i == position) {
                            dialog.dismiss();
                            onItemClick.onItemClick(plugin);
                        }
                        i++;
                    }
                }
            }
        });
    }

    public void show() {
        dialog.show();
    }

    private static Map<String,?> createItem(String title) {
        Map<String,String> item = new HashMap<>();
        item.put("title", title);
        return item;
    }

    public interface PluginsListAlertDialogClick {
        public void onItemClick(MuninPlugin plugin);
    }
}
