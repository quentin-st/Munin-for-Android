package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninNode;

import java.util.List;

public class Adapter_NodesList extends ArrayAdapter<MuninNode> {
	private Context context;
	private List<MuninNode> nodes;

	public Adapter_NodesList(Context context, List<MuninNode> nodes) {
		super(context, R.layout.nodes_list, nodes);
		this.context = context;
		this.nodes = nodes;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView != null)
			view = convertView;
		else
			view = LayoutInflater.from(context).inflate(R.layout.nodes_list, parent, false);

		TextView textView1 = (TextView) view.findViewById(R.id.line_a);
		TextView textView2 = (TextView) view.findViewById(R.id.line_b);

		MuninNode node = nodes.get(position);
		textView1.setText(node.getName());
		textView2.setText(node.getParent().getName());

		return view;
	}

	public MuninNode getItem(int position) {
		return this.nodes.get(position);
	}
}
