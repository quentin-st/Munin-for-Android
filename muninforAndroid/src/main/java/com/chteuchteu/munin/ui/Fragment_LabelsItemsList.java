package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_SeparatedList;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Fragment_LabelsItemsList extends Fragment {
	private MuninFoo muninFoo;
	private Context context;
	private ILabelsActivity activity;
	private View view;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (ILabelsActivity) activity;
		this.context = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		muninFoo = MuninFoo.getInstance();

		this.view = inflater.inflate(R.layout.fragment_label, container, false);

		Bundle args = getArguments();
		if (args != null && args.containsKey("labelId"))
			setLabel(muninFoo.getLabel(args.getLong("labelId")));

		activity.onLabelsItemsListFragmentLoaded();

		return this.view;
	}

	public void setLabel(final Label label) {
		if (label == null)
			return;

		List<List<MuninPlugin>> labelsListCat = label.getPluginsSortedByServer(muninFoo);
		final List<MuninPlugin> correspondance = new ArrayList<>();
		final List<String> correspondanceServers = new ArrayList<>();
		Adapter_SeparatedList adapter = new Adapter_SeparatedList(context, false);
		for (List<MuninPlugin> l : labelsListCat) {
			correspondanceServers.add("");
			correspondance.add(new MuninPlugin());
			List<Map<String,?>> elements = new LinkedList<>();
			String serverName = "";
			for (MuninPlugin p : l) {
				elements.add(createItem(p.getFancyName(), p.getName()));
				if (serverName.equals(""))
					serverName = p.getInstalledOn().getName();
				correspondance.add(p);
				correspondanceServers.add(p.getInstalledOn().getServerUrl());
			}

			adapter.addSection(serverName, new SimpleAdapter(context, elements, R.layout.plugins_list,
					new String[] { "title", "caption" }, new int[] { R.id.line_a, R.id.line_b }));
		}

		ListView labels_listView = (ListView) view.findViewById(R.id.labels_listview);

		labels_listView.setAdapter(adapter);
		labels_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				MuninPlugin plugin = correspondance.get(position);
				String serverUrl = correspondanceServers.get(position);
				muninFoo.setCurrentServer(muninFoo.getServer(serverUrl));
				int pos = label.getPlugins().indexOf(plugin);
				activity.onLabelItemClick(pos, label.getName(), label.getId());
			}
		});
	}

	private Map<String,?> createItem(String title, String caption) {
		Map<String,String> item = new HashMap<>();
		item.put("title", title);
		item.put("caption", caption);
		return item;
	}
}
