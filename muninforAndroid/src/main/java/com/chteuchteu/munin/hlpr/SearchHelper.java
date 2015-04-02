package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_Search;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchHelper {
    private Activity activity;
    private Context context;

    private EditText search;
    private ListView search_results;
    private Adapter_Search search_results_adapter;
    private List<SearchResult> search_results_array;
    private List<Grid> search_cachedGridsList;

    //final int DRAWABLE_LEFT = 0;
    //final int DRAWABLE_TOP = 1;
    final int DRAWABLE_RIGHT = 2;
    //final int DRAWABLE_BOTTOM = 3;

    public SearchHelper(Activity activity) {
        this.activity = activity;
        this.context = activity;
    }

    public void initSearch() {
        search = (EditText) activity.findViewById(R.id.drawer_search);
        search_results = (ListView) activity.findViewById(R.id.drawer_search_results);
        search_results.setVisibility(View.VISIBLE);

        // Cancel button
        search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);

        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return actionId == EditorInfo.IME_ACTION_SEARCH;
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @SuppressLint("DefaultLocale")
            @Override
            public void afterTextChanged(Editable s) {
                onSearchExprChanged(s.toString().toLowerCase());
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        search_results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                SearchResult searchResult = search_results_array.get(position);
                searchResult.onClick(activity);
            }
        });

        // Cancel button listener
        search.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility") @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    MuninFoo.log("X: " + event.getX());
                    MuninFoo.log("CompoundLeft = " + (search.getRight() - search.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()));
                    if (event.getX()+100 >= (search.getRight() - search.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        search.setText("");
                        Util.hideKeyboard(activity, search);
                    }
                }

                return false;
            }
        });
    }

    public void onSearchExprChanged(String expr) {
        if (expr.length() == 0) {
            activity.findViewById(R.id.drawer_scrollview).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.drawer_search_results).setVisibility(View.GONE);
            search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);
            return;
        } else {
            activity.findViewById(R.id.drawer_scrollview).setVisibility(View.GONE);
            activity.findViewById(R.id.drawer_search_results).setVisibility(View.VISIBLE);
            search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(255);
        }

        if (search_results_adapter != null) {
            search_results_array.clear();
            search_results_adapter.notifyDataSetChanged();
        } else {
            search_results_array = new ArrayList<>();
            search_results_adapter = new Adapter_Search(activity, search_results_array);
            search_results.setAdapter(search_results_adapter);
        }

        search_results_array.addAll(getSearchResults(expr));

        search_results_adapter.notifyDataSetChanged();
    }

    private List<SearchResult> getSearchResults(String expr) {
        List<SearchResult> searchResults = new ArrayList<>();

        // Search in plugins and nodes
        for (MuninNode node : MuninFoo.getInstance(context).getNodes()) {
            if (node.matches(expr))
                searchResults.add(new SearchResult(node));

            for (MuninPlugin plugin : node.getPlugins()) {
                if (plugin.matches(expr))
                    searchResults.add(new SearchResult(plugin));
            }
        }

        // Search in grids
        if (search_cachedGridsList == null)
            search_cachedGridsList = MuninFoo.getInstance(context).sqlite.dbHlpr.getGrids(null);

        for (Grid grid : search_cachedGridsList) {
            if (grid.matches(expr))
                searchResults.add(new SearchResult(grid));
        }

        // Search in labels
        for (Label label : MuninFoo.getInstance(context).labels) {
            if (label.matches(expr))
                searchResults.add(new SearchResult(label));
        }

        return searchResults;
    }
}
