package com.chteuchteu.munin;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;

public class Adapter_SeparatedList extends BaseAdapter {
	private Context context;
	private final Map<String,Adapter> sections = new LinkedHashMap<String,Adapter>();
	private final ArrayAdapter<String> headers;
	private final static int TYPE_SECTION_HEADER = 0;
	
	public Adapter_SeparatedList(Context context) {
		this.headers = new ArrayAdapter<String>(context, R.layout.list_header);
		this.context = context;
	}
	
	public void addSection(String section, Adapter adapter) {
		this.headers.add(section);
		this.sections.put(section, adapter);
	}
	
	public Object getItem(int position) {
		for(Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section 
			if(position == 0) return section;
			if(position < size) return adapter.getItem(position - 1);
			
			// otherwise jump into next section
			position -= size;
		}
		return null;
	}
	
	public int getCount() {
		// total together all sections, plus one for each section header
		int total = 0;
		for(Adapter adapter : this.sections.values())
			total += adapter.getCount() + 1;
		return total;
	}
	
	public int getViewTypeCount() {
		// assume that headers count as one, then total all sections
		int total = 1;
		for(Adapter adapter : this.sections.values())
			total += adapter.getViewTypeCount();
		return total;
	}
	
	public int getItemViewType(int position) {
		int type = 1;
		for(Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section 
			if(position == 0) return TYPE_SECTION_HEADER;
			if(position < size) return type + adapter.getItemViewType(position - 1);
			
			// otherwise jump into next section
			position -= size;
			type += adapter.getViewTypeCount();
		}
		return -1;
	}
	
	public boolean areAllItemsSelectable() {
		return false;
	}
	
	public boolean isEnabled(int position) {
		return (getItemViewType(position) != TYPE_SECTION_HEADER);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int sectionnum = 0;
		for (Object section : this.sections.keySet()) {
			Adapter adapter = sections.get(section);
			int size = adapter.getCount() + 1;
			
			// check if position inside this section 
			if (position == 0) {
				View view = headers.getView(sectionnum, convertView, parent);
				TextView textView = (TextView) view.findViewById(R.id.list_header_title);
				Util.Fonts.setFont(context, textView, CustomFont.Roboto_Medium);
				return view;
			}
			if (position < size) {
				View view = adapter.getView(position - 1, convertView, parent);
				return view;
			}
			
			// otherwise jump into next section
			position -= size;
			sectionnum++;
		}
		return null;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
}
