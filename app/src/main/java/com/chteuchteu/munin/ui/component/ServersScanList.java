package com.chteuchteu.munin.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninNode;

import java.util.HashMap;
import java.util.Map;

public class ServersScanList extends LinearLayout {
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOADED = 2;

    private LinearLayout list;
    private Map<MuninNode, LinearLayout> views;

    public ServersScanList(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.servers_scan_list, this);
        views = new HashMap<>();
    }

    public void addNode(MuninNode node) {
        LinearLayout view = (LinearLayout) inflate(getContext(), R.layout.servers_scan_list_item, list);

        TextView textView = view.findViewById(R.id.node_name);
        textView.setText(node.getName());

        // TODO attach to list?

        this.views.put(node, view);

        invalidate();
        requestLayout();
    }

    public void updateNode(MuninNode node, int state) {
        LinearLayout view = this.views.get(node);

        switch (state) {
            case STATE_LOADING:
                // TODO
                break;
            case STATE_LOADED:
                // TODO
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
