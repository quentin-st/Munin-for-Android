package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.List;

public class Adapter_ServersList extends ArrayAdapter<MuninServer> {
	private Context context;
	private List<MuninServer> servers;

	public Adapter_ServersList(Context context, List<MuninServer> servers) {
		super(context, R.layout.servers_list, servers);
		this.context = context;
		this.servers = servers;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView != null)
			view = convertView;
		else
			view = LayoutInflater.from(context).inflate(R.layout.servers_list, parent, false);

		TextView textView1 = (TextView) view.findViewById(R.id.line_a);
		TextView textView2 = (TextView) view.findViewById(R.id.line_b);

		MuninServer server = servers.get(position);
		textView1.setText(server.getName());
		textView2.setText(server.getParent().getName());

		return view;
	}

	public MuninServer getItem(int position) {
		return this.servers.get(position);
	}
}
