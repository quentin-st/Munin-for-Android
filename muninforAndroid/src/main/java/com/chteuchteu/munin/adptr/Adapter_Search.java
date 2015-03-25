package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.SearchResult;

import java.util.List;

public class Adapter_Search extends BaseAdapter {
    private List<SearchResult> searchArrayList;
    private Context context;
    private LayoutInflater mInflater;

    public Adapter_Search(Context context, List<SearchResult> results) {
        this.searchArrayList = results;
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public int getCount() { return this.searchArrayList.size(); }
    public Object getItem(int position) { return this.searchArrayList.get(position); }
    public long getItemId(int position) { return position; }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.twolineslist, parent, false);

        TextView ed_line_a = (TextView) convertView.findViewById(R.id.line_a);
        ed_line_a.setText(searchArrayList.get(position).getLine1());
        String line_b = searchArrayList.get(position).getLine2();
        TextView ed_line_b = ((TextView) convertView.findViewById(R.id.line_b));
        if (line_b != null && line_b.equals(""))
            ed_line_b.setVisibility(View.GONE);
        else
            ed_line_b.setText(line_b);

        Util.Fonts.setFont(context, ed_line_a, Util.Fonts.CustomFont.RobotoCondensed_Regular);
        Util.Fonts.setFont(context, ed_line_b, Util.Fonts.CustomFont.RobotoCondensed_Regular);

        return convertView;
    }
}
