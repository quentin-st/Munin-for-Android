package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin;

public class DialogFragment_AddGridItem extends DialogFragment {
	public static final String KEY_X = "x";
	public static final String KEY_Y = "y";

	private Dialog dialog;
	private MuninFoo muninFoo;
	private Context context;

	private Grid grid;
	private MultiLevelCheckboxList multiLevelCheckboxList;

	public static DialogFragment_AddGridItem init(Grid grid, int X, int Y) {
		DialogFragment_AddGridItem newFragment = new DialogFragment_AddGridItem();
		Bundle args = new Bundle();
		args.putInt(KEY_X, X);
		args.putInt(KEY_Y, Y);
		newFragment.setArguments(args);
		return newFragment;
	}

	public DialogFragment_AddGridItem() {
		this.muninFoo = MuninFoo.getInstance();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.context = getActivity();
		this.dialog = getDialog();
		Activity_Grid activity = (Activity_Grid) getActivity();
		Grid grid = activity.getGrid();
		Bundle args = getArguments();
		int gridItemX = args.getInt(KEY_X);
		int gridItemY = args.getInt(KEY_Y);

		// Servers list
		dialog.setTitle(R.string.text71);

		multiLevelCheckboxList = new MultiLevelCheckboxList(context);
		multiLevelCheckboxList.initList(muninFoo.getMasters());

		return multiLevelCheckboxList;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(context)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						/*List<MuninPlugin> selectedItems = multiLevelCheckboxList.getCheckedPlugins();

						if (grid.getNbColumns() < 3 && selectedItems.size() > 3)
							while (grid.getNbColumns() < 3)
								grid.addColumn(context, true);

						int maxWidth = grid.getNbColumns();
						List<GridItem> addedItems = new ArrayList<>();
						for (MuninPlugin plugin : selectedItems) {
							if (!alreadyAdded(grid, plugin)) {
								GridItem item = new GridItem(grid, plugin);
								item.setActivityReferences(context, activity, fragment);
								int[] pos = grid.getNextAvailable(X, Y, maxWidth, context);
								item.X = pos[0];
								item.Y = pos[1];
								grid.add(item, context, f, true, false);
								grid.swapViews(grid.getViewAt(item.X, item.Y), item.getView(null));
								addedItems.add(item);
							}
						}

						f.sqlite.dbHlpr.insertGridItemRelations(addedItems);
						// Load graph for those items
						grid.dHelper.startForItems(addedItems);*/
					}
				})
				.setNegativeButton(R.string.text64, null)
				.create();
	}

	private static boolean alreadyAdded(Grid g, MuninPlugin p) {
		for (GridItem item : g.getItems()) {
			if (item != null && item.getPlugin() != null && item.getPlugin().equals(p)) // hotfix
				return true;
		}
		return false;
	}
}
