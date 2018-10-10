package com.chteuchteu.munin.adptr;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Grid;

import java.util.List;

/**
 * Open an AlertDialog with transparent background (similar to Spinner),
 *  with a grouped nodes list
 */
public class GridsListAlertDialog {
	private AlertDialog dialog;

	public GridsListAlertDialog(Context context, View attachTo, final List<Grid> grids,
	                            final GridsListAlertDialogClick onItemClick) {
		// Init
		LinearLayout view = new LinearLayout(context);
		view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		view.setOrientation(LinearLayout.VERTICAL);

		ListView listView = new ListView(context);
		listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, grids));

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
				dialog.dismiss();
				onItemClick.onItemClick(grids.get(position));
			}
		});
	}

	public void show() {
		dialog.show();
	}

	public interface GridsListAlertDialogClick {
		void onItemClick(Grid grid);
	}
}
