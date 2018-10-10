package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_SeparatedList;
import com.chteuchteu.munin.adptr.NodesListAlertDialog;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Activity_Plugins extends MuninActivity {
	private SimpleAdapter 		adapter;
	private List<MuninPlugin>	pluginsList;
	private List<HashMap<String,String>> adapterList;
	private MuninPlugin[] 		filteredPluginsList;

	private TextView			customActionBarView_textView;
    private NodesListAlertDialog nodesListAlertDialog;

	private View 			customActionBarView;

	private EditText		filter;
	private ListView       listview;

	private Mode mode;
	private enum Mode {
		GROUPED, FLAT
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_plugins);
		super.onContentViewSet();

		listview = (ListView) findViewById(R.id.listview);
		adapterList = new ArrayList<>();

		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);

		// ActionBar custom view
		actionBar.setDisplayShowTitleEnabled(false);
		customActionBarView = findViewById(R.id.actionbar_dropdown);
		customActionBarView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (nodesListAlertDialog == null)
				    nodesListAlertDialog = new NodesListAlertDialog(context, customActionBarView,
                            new NodesListAlertDialog.NodesListAlertDialogClick() {
                        @Override
                        public void onItemClick(MuninNode node) {
							if (muninFoo.getCurrentNode() != node) {
								muninFoo.setCurrentNode(node);
								customActionBarView_textView.setText(node.getName());
								updateListView(mode);
							}
                        }
                    });

                nodesListAlertDialog.show();
			}
		});
        customActionBarView_textView = (TextView) customActionBarView.findViewById(R.id.text);
        customActionBarView_textView.setText(muninFoo.getCurrentNode().getName());

        // By default, mode is grouped (FLAT is for search)
		mode = Mode.GROUPED;

		// Fetch plugins list
		pluginsList = muninFoo.getCurrentNode().getPlugins();

		updateListView(mode);
	}

	private void updateListView(final Mode mode) {
		this.mode = mode;

		if (mode == Mode.FLAT) {
			adapterList.clear();

			HashMap<String,String> item;
			for (MuninPlugin plugin : pluginsList) {
				item = new HashMap<>();
				item.put("line1", plugin.getFancyName());
				item.put("line2", plugin.getName());
				adapterList.add(item);
			}

			adapter = new SimpleAdapter(this, adapterList, R.layout.plugins_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
			listview.setAdapter(adapter);
		} else {
		    // Grouped mode
			// Create plugins list (grouped by category)
			List<List<MuninPlugin>> categories = muninFoo.getCurrentNode().getPluginsListWithCategory();

			Adapter_SeparatedList adapter = new Adapter_SeparatedList(this, false);
			for (List<MuninPlugin> category : categories) {
				List<Map<String,?>> elements = new LinkedList<>();
				String categoryName = "";
				for (MuninPlugin plugin : category) {
					Map<String,String> item = new HashMap<>();
					item.put("title", plugin.getFancyName());
					item.put("caption", plugin.getName());

					elements.add(item);
					categoryName = Util.capitalize(plugin.getCategory());
				}

				adapter.addSection(categoryName, new SimpleAdapter(this, elements, R.layout.plugins_list,
						new String[] { "title", "caption" }, new int[] { R.id.line_a, R.id.line_b }));
			}
			listview.setAdapter(adapter);
		}

		// On click name, open the GraphView
		listview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				TextView pluginName = (TextView) view.findViewById(R.id.line_b);
				Intent intent = new Intent(Activity_Plugins.this, Activity_GraphView.class);

				MuninPlugin plugin = muninFoo.getCurrentNode().getPlugin(pluginName.getText().toString());
				intent.putExtra("position", muninFoo.getCurrentNode().getPlugins().indexOf(plugin));
				intent.putExtra("from", "plugins");
				startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
			}
		});

        // On long click, display options list
		listview.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapter, final View view, final int position, long arg) {
				AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						context, android.R.layout.simple_list_item_1);
				arrayAdapter.add(context.getString(R.string.delete_plugin));

				builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								String pluginName = ((TextView) view.findViewById(R.id.line_b)).getText().toString();
								MuninPlugin plugin = muninFoo.getCurrentNode().getPlugin(pluginName);

								if (plugin == null)
									return;

								muninFoo.getCurrentNode().getPlugins().remove(plugin);
								muninFoo.sqlite.dbHlpr.deleteMuninPlugin(plugin, true);
								// Remove from labels if necessary
								muninFoo.removeLabelRelation(plugin);

								// Save scroll state
								int index = listview.getFirstVisiblePosition();
								View v = listview.getChildAt(0);
								int top = (v == null) ? 0 : v.getTop();

								updateListView(mode);

								listview.setSelectionFromTop(index, top);
								break;
						}
					}
				});
				builderSingle.show();

				return true;
			}
		});
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.plugins, menu);

		// Create filter field
		filter = new EditText(this);
		// This EditText must not have the default accent color or it won't be visible
		filter.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
		filter.setTextColor(Color.WHITE);
		filter.setTag("hidden");

		filter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (pluginsList != null && !pluginsList.isEmpty() && s != null) {
					adapterList.clear();
					String search = s.toString();

					filteredPluginsList = new MuninPlugin[pluginsList.size()];
					for (int i = 0; i < pluginsList.size(); i++) {
						if (pluginsList.get(i).getFancyName().toLowerCase(Locale.ENGLISH).contains(search.toLowerCase(Locale.ENGLISH))
								|| pluginsList.get(i).getName().toLowerCase(Locale.ENGLISH).contains(search.toLowerCase(Locale.ENGLISH))) {
                            filteredPluginsList[i] = pluginsList.get(i);
                        }
					}

					HashMap<String, String> item;
					for (MuninPlugin p : filteredPluginsList) {
						if (p != null) {
							item = new HashMap<>();
							item.put("line1", p.getFancyName());
							item.put("line2", p.getName());
							adapterList.add(item);
						}
					}
					adapter = new SimpleAdapter(Activity_Plugins.this, adapterList, R.layout.plugins_list, new String[]{"line1", "line2"}, new int[]{R.id.line_a, R.id.line_b});
					listview.setAdapter(adapter);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Graphs; }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_filter:
				toggleFilterField(filter.getTag().equals("hidden"));
				return true;
		}

		return true;
	}

	private void toggleFilterField(boolean show) {
		if (show) {
			filter.setTag("shown");
			filter.setFocusable(true);
			filter.setFocusableInTouchMode(true);

			customActionBarView.setVisibility(View.GONE);
			actionBar.setCustomView(filter);

			filter.setLayoutParams(new Toolbar.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT,
					Gravity.TOP | Gravity.RIGHT));
		} else {
			filter.setTag("hidden");
			filter.setFocusable(false);
			filter.setFocusableInTouchMode(false);
			filter.setText("");

			// Hide keyboard
			filter.clearFocus();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(filter.getWindowToken(), 0);

			customActionBarView.setVisibility(View.VISIBLE);
			actionBar.setCustomView(null);
		}
	}

	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        if (filter.getTag().equals("shown")) {
			toggleFilterField(false);
			updateListView(Mode.GROUPED);
		} else {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(this, TransitionStyle.SHALLOWER);
		}
	}
}
