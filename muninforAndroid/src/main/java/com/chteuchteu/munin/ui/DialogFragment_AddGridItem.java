package com.chteuchteu.munin.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_NodesList;
import com.chteuchteu.munin.obj.Grid;

public class DialogFragment_AddGridItem extends DialogFragment {
	public static final String KEY_X = "x";
	public static final String KEY_Y = "y";

	private Dialog dialog;
	private MuninFoo muninFoo;
	private Context context;

	private Grid grid;

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

		// Nodes list
		dialog.setTitle(R.string.text71);
		ListView nodesList = new ListView(context);
		Adapter_NodesList nodesAdapter = new Adapter_NodesList(context, muninFoo.getNodes());
		nodesList.setAdapter(nodesAdapter);
		nodesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO
			}
		});

		return nodesList;
	}
}
