package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_GraphView;
import com.chteuchteu.munin.adptr.PluginsListAlertDialog;
import com.chteuchteu.munin.async.DynazoomDetector;
import com.chteuchteu.munin.async.FieldsDescriptionFetcher;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.DynazoomHelper;
import com.chteuchteu.munin.hlpr.DynazoomHelper.DynazoomFetcher;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.HTTPResponse.BaseResponse;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.DynazoomAvailability;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.edmodo.rangebar.RangeBar;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.co.senab.photoview.PhotoViewAttacher;

public class Activity_GraphView extends MuninActivity {
	private MuninPlugin     currentPlugin;
	public Period       load_period;
	private ViewPager    viewPager;
	private Adapter_GraphView adapter;
	private View        ic_secure;
	private View        ic_insecure;
    private TextView    toolbarPluginName;
    private PluginsListAlertDialog pluginsListAlertDialog;

	public int          viewFlowMode;
	public static final int VIEWFLOWMODE_GRAPHS = 1;
	public static final int VIEWFLOWMODE_LABELS = 2;
	public Label        label;

    public int[]        imageViewDimensions;

	private static int  position;
	public HashMap<Integer, PhotoViewAttacher> photoViewAttachers;
	private HashMap<Integer, Bitmap> bitmaps;
	public ImageView    iv_documentation;
	/**
	 * How many bitmaps should be kept on left and right
	 * of current list position
	 */
	private static final int BITMAPS_PADDING = 5;
	public FloatingActionButton fab;
	public boolean      isFabShown;

	private MenuItem    item_period;
	private MenuItem    item_documentation;

	private Handler	    mHandler;
	private Runnable    mHandlerTask;

	// If the Adapter_GraphView:getView method should
	// load the graphs
	public boolean	    loadGraphs = false;

	// Dynazoom
	private DynazoomFetcher dynazoomFetcher;
	private long    dynazoom_from;
	private long    dynazoom_to;

	private Drawable toolbar_originalIcon;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_graphview);
		super.onContentViewSet();

		if (settings.getBool(Settings.PrefKeys.ScreenAlwaysOn))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		actionBar.setTitle("");
		ic_secure = findViewById(R.id.connection_secure);
		ic_insecure = findViewById(R.id.connection_insecure);
		ic_insecure.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(context, R.string.certificate_error, Toast.LENGTH_SHORT).show();
			}
		});

		load_period = Period.get(settings.getString(Settings.PrefKeys.DefaultScale));

		// Coming from widget
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null
				&& thisIntent.getExtras().containsKey("node")
				&& thisIntent.getExtras().containsKey("plugin")
				&& thisIntent.getExtras().containsKey("period")) {
			String node = thisIntent.getExtras().getString("node");
			String plugin = thisIntent.getExtras().getString("plugin");
			// Setting currentNode
			muninFoo.setCurrentNode(muninFoo.getNode(node));

			// Giving position of plugin in list to GraphView
			for (int i=0; i<muninFoo.getCurrentNode().getPlugins().size(); i++) {
				if (muninFoo.getCurrentNode().getPlugins().get(i).getName().equals(plugin))
					thisIntent.putExtra("position", i);
			}
		}

		int pos = 0;

		// Coming from Grid
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("plugin")) {
			int i = 0;
			for (MuninPlugin p : muninFoo.getCurrentNode().getPlugins()) {
				if (p.getName().equals(thisIntent.getExtras().getString("plugin"))) {
					pos = i; break;
				}
				i++;
			}
		}

		// Coming from PluginSelection / Label
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("position"))
			pos = thisIntent.getExtras().getInt("position");

		// Orientation changed
		if (savedInstanceState != null)
			pos = savedInstanceState.getInt("position");

		String from = thisIntent != null && thisIntent.getExtras() != null ? thisIntent.getExtras().getString("from") : "";
		if (from != null && (from.equals("labels") || from.equals("main_labels"))) {
			viewFlowMode = VIEWFLOWMODE_LABELS;
			long labelId = thisIntent != null && thisIntent.getExtras() != null ? thisIntent.getExtras().getLong("labelId") : -1;
			this.label = muninFoo.getLabel(labelId);
			this.currentPlugin = this.label.getPlugins().get(pos);
		} else {
			viewFlowMode = VIEWFLOWMODE_GRAPHS;
			this.currentPlugin = muninFoo.getCurrentNode().getPlugin(pos);
		}

		if (this.currentPlugin == null) {
			Toast.makeText(this, R.string.text09, Toast.LENGTH_SHORT).show();
			startActivity(new Intent(this, Activity_Plugins.class));
		}

		// Viewflow
		position = pos;
		int nbPlugins;
		if (viewFlowMode == VIEWFLOWMODE_LABELS)
			nbPlugins = this.label.getPlugins().size();
		else
			nbPlugins = muninFoo.getCurrentNode().getPlugins().size();
		bitmaps = new HashMap<>();
		photoViewAttachers = new HashMap<>();

		// Init ViewPager
		viewPager = (ViewPager) findViewById(R.id.viewPager);
		adapter = new Adapter_GraphView(getSupportFragmentManager(), this, muninFoo, nbPlugins);
		viewPager.setAdapter(adapter);
		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
			}
			@Override public void onTabUnselected(TabLayout.Tab tab) { }
			@Override public void onTabReselected(TabLayout.Tab tab) { }
		});
        viewPager.setCurrentItem(pos);

		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int pos) {
				position = pos;
				cleanBitmaps(position);

				if (viewFlowMode == VIEWFLOWMODE_GRAPHS)
					currentPlugin = muninFoo.getCurrentNode().getPlugin(position);
				else {
					currentPlugin = label.getPlugins().get(position);
					((TextView) findViewById(R.id.serverName)).setText(currentPlugin.getInstalledOn().getName());
					muninFoo.setCurrentNode(currentPlugin.getInstalledOn());
				}

				toolbarPluginName.setText(currentPlugin.getFancyNameOrDefault());

				// Documentation
				if (item_documentation != null) {
					// DocumentationAvailability is cached in order to avoid browsing the JSON array too much
					if (currentPlugin.isDocumentationAvailable() == Util.SpecialBool.UNKNOWN)
						currentPlugin.setDocumentationAvailability(muninFoo.documentationHelper.hasDocumentation(currentPlugin));

					item_documentation.setVisible(currentPlugin.isDocumentationAvailable() == Util.SpecialBool.TRUE);
				}

				// If changed plugin from drawer and documentation is shown => hide it
				if (findViewById(R.id.documentation).getVisibility() == View.VISIBLE)
					hideDocumentation();

				// Dynazoom
				boolean dynazoomAvailable = currentPlugin.getInstalledOn().getParent().isDynazoomAvailable() == DynazoomAvailability.TRUE;

				if (!dynazoomAvailable && isFabShown) { // Hide fab
					fab.hide();
					isFabShown = false;
				} else if (dynazoomAvailable && !isFabShown) { // Show fab
					isFabShown = true;
					fab.show();
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

        // ImageView dimensions
        viewPager.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageViewDimensions = new int[] {
                        viewPager.getWidth(),
                        viewPager.getHeight()
                };
                Util.removeOnGlobalLayoutListener(viewPager, this);
            }
        });

		fab = (FloatingActionButton) findViewById(R.id.fab);
		if (currentPlugin.getInstalledOn().getParent().isDynazoomAvailable() == DynazoomAvailability.TRUE) {
			fab.setVisibility(View.VISIBLE);
			fab.show();
		} else {
			fab.hide();
			isFabShown = false;
		}
		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				actionDynazoom();
			}
		});
		
		// Launch periodical check
		if (settings.getBool(Settings.PrefKeys.AutoRefresh)) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override
				public void run() {
					actionRefresh();
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}

		// HD Graphs
		switch (muninFoo.getCurrentNode().getParent().isDynazoomAvailable()) {
			case AUTO_DETECT:
				new DynazoomDetector(this, muninFoo.getCurrentNode()).execute();
				break;
			case FALSE:
				// Load as before
				loadGraphs = true;
				actionRefresh();
				break;
			case TRUE:
				// Attach a ViewTreeObserver. This is needed since
				// the ImageView dimensions aren't known right now.
				ViewTreeObserver vtObserver = viewPager.getViewTreeObserver();
				if (vtObserver.isAlive()) {
					vtObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							Util.removeOnGlobalLayoutListener(viewPager, this);

							// Now we have the dimensions.
							loadGraphs = true;
							actionRefresh();
						}
					});
				}
				break;
		}

		// Build secondary drawer for node change
		DrawerBuilder drawerBuilder = new DrawerBuilder().withActivity(this);

		for (MuninMaster master : muninFoo.getMasters()) {
			drawerBuilder.addDrawerItems(new SectionDrawerItem().withName(master.getName()));

			for (MuninNode node : master.getChildren()) {
				drawerBuilder.addDrawerItems(
						new PrimaryDrawerItem()
								.withName(node.getName())
								.withIdentifier((int) node.getId())
				);
			}
		}

		drawerBuilder
				.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
					@Override
					public boolean onItemClick(View view, int i, IDrawerItem iDrawerItem) {
						MuninNode node = muninFoo.getNodeById(iDrawerItem.getIdentifier());

						if (node != muninFoo.getCurrentNode()) {
							muninFoo.setCurrentNode(node);
							Intent intent = new Intent(Activity_GraphView.this, Activity_GraphView.class);
							if (node.hasPlugin(currentPlugin)) // Switch to the same plugin
								intent.putExtra("position", muninFoo.getCurrentNode().getPosition(currentPlugin));
							else if (node.hasCategory(currentPlugin.getCategory())) // Switch to same category
								intent.putExtra("position",
										muninFoo.getCurrentNode().getPosition(
												node.getFirstPluginFromCategory(currentPlugin.getCategory())));
							else
								intent.putExtra("position", 0);
							startActivity(intent);
							Util.setTransition(activity, TransitionStyle.DEEPER);
						}
						return false;
					}
				})
				.withDrawerGravity(Gravity.END)
				.withSelectedItem((int) muninFoo.getCurrentNode().getId())
				.append(drawerHelper.getDrawer());


        // Toolbar custom view (plugins spinner)
        actionBar.setDisplayShowTitleEnabled(false);
        final View customToolbarView = findViewById(R.id.actionbar_dropdown);
        customToolbarView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pluginsListAlertDialog == null) {
                    pluginsListAlertDialog = new PluginsListAlertDialog(context,
                            customToolbarView, muninFoo.getCurrentNode(),
                            new PluginsListAlertDialog.PluginsListAlertDialogClick() {
                                @Override
                                public void onItemClick(MuninPlugin plugin) {
                                    int index = muninFoo.getCurrentNode().getPlugins().indexOf(plugin);
                                    if (isDynazoomOpen())
                                        hideDynazoom();

                                    viewPager.setCurrentItem(index, true);
                                }
                            });
                }

                pluginsListAlertDialog.show();
            }
        });
        toolbarPluginName = (TextView) customToolbarView.findViewById(R.id.text);
        toolbarPluginName.setText(currentPlugin.getFancyNameOrDefault());
	}

	public void onResume() {
		super.onResume();

		// Coming from widget
		// This has to be done here, if the app is already started on this activity
		// and the user clicks on a widget.
		Intent thisIntent = getIntent();
		if (thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("period"))
			load_period = Period.get(thisIntent.getExtras().getString("period"));

		if (load_period == null)
			load_period = Period.DAY;

		if (item_period != null)
			item_period.setTitle(load_period.getLabel(context));
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		savedInstanceState.putInt("position", position);
	}
	
	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.graphview, menu);
		
		item_period = menu.findItem(R.id.menu_period);
		item_documentation = menu.findItem(R.id.menu_documentation);
		item_documentation.setVisible(muninFoo.documentationHelper.hasDocumentation(currentPlugin));
		MenuItem item_openInBrowser = menu.findItem(R.id.menu_openinbrowser);
        MenuItem item_fieldsDescription = menu.findItem(R.id.menu_fieldsDescription);
		
		if (muninFoo.getCurrentNode().getPlugin(0).hasPluginPageUrl()) {
			item_openInBrowser.setVisible(true);
			item_fieldsDescription.setVisible(true);
		}

		item_period.setTitle(load_period.getLabel(context));
	}
	
	private void changePeriod(Period newPeriod) {
		bitmaps.clear();
		
		load_period = newPeriod;
		
		adapter.refreshAll();

		if (isDynazoomOpen()) {
			dynazoom_from = DynazoomHelper.getFromPinPoint(load_period);
			dynazoom_to = DynazoomHelper.getToPinPoint();

			dynazoomFetcher = (DynazoomFetcher) new DynazoomFetcher(currentPlugin, (ImageView) findViewById(R.id.dynazoom_imageview),
					(ProgressBar) findViewById(R.id.dynazoom_progressbar), context, muninFoo.getUserAgent(),
					dynazoom_from, dynazoom_to).execute();
			dynazoom_updateFromTo();
			((RangeBar) findViewById(R.id.dynazoom_rangebar)).setThumbIndices(0, DynazoomHelper.RANGEBAR_TICKS_COUNT - 1);
		}
		
		item_period.setTitle(load_period.getLabel(context).toUpperCase());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_refresh:	actionRefresh(); 		return true;
			case R.id.menu_save:		actionSave();			return true;
			case R.id.menu_fieldsDescription: actionFieldsDescription(); return true;
			case R.id.menu_labels:    actionLabels();         return true;
			case R.id.period_day:     changePeriod(Period.DAY); return true;
			case R.id.period_week:    changePeriod(Period.WEEK); return true;
			case R.id.period_month:   changePeriod(Period.MONTH); return true;
			case R.id.period_year:    changePeriod(Period.YEAR); return true;
			case R.id.menu_openinbrowser: actionOpenInBrowser(); return true;
			case R.id.menu_documentation: actionDocumentation(); return true;
		}
		return true;
	}
	
	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        if (findViewById(R.id.documentation).getVisibility() == View.VISIBLE) {
			hideDocumentation();
			return;
		}

		if (isDynazoomOpen()) {
			hideDynazoom();
			return;
		}

		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("from")) {
			String from = thisIntent.getExtras().getString("from");
			switch (from) {
				case "labels":
					if (thisIntent.getExtras().containsKey("label")) {
						Intent intent = new Intent(Activity_GraphView.this, Activity_Labels.class);
						intent.putExtra("labelId", thisIntent.getExtras().getLong("labelId"));
						startActivity(intent);
						Util.setTransition(this, TransitionStyle.SHALLOWER);
					}
					break;
				case "main_labels": {
					startActivity(new Intent(Activity_GraphView.this, Activity_Main.class));
					Util.setTransition(this, TransitionStyle.SHALLOWER);
					break;
				}
				case "alerts":
					if (thisIntent.getExtras().containsKey("node")) {
						if (muninFoo.getNode(thisIntent.getExtras().getString("node")) != null)
							muninFoo.setCurrentNode(muninFoo.getNode(thisIntent.getExtras().getString("node")));
						Intent intent = new Intent(Activity_GraphView.this, Activity_AlertsPlugins.class);
						startActivity(intent);
						Util.setTransition(this, TransitionStyle.SHALLOWER);
					}
					break;
				case "grid":
					if (thisIntent.getExtras().containsKey("fromGrid")) {
						Intent intent = new Intent(Activity_GraphView.this, Activity_Grid.class);
						intent.putExtra("gridName", thisIntent.getExtras().getString("fromGrid"));
						startActivity(intent);
						Util.setTransition(this, TransitionStyle.SHALLOWER);
					} else {
						startActivity(new Intent(Activity_GraphView.this, Activity_Grids.class));
						Util.setTransition(this, TransitionStyle.SHALLOWER);
					}
					break;
				case "plugins":
					Intent intent = new Intent(this, Activity_Plugins.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					Util.setTransition(this, TransitionStyle.SHALLOWER);
					break;
			}
		} else {
			Intent intent = new Intent(this, Activity_Plugins.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(this, TransitionStyle.SHALLOWER);
		}
	}
	
	public void actionRefresh() {
		if (isDynazoomOpen()) {
			dynazoomFetcher = (DynazoomFetcher) new DynazoomFetcher(currentPlugin, (ImageView) findViewById(R.id.dynazoom_imageview),
					(ProgressBar) findViewById(R.id.dynazoom_progressbar), context, muninFoo.getUserAgent(),
					dynazoom_from, dynazoom_to).execute();
		} else {
			bitmaps.clear();
			adapter.refreshAll();
		}
	}
	private void actionSave() {
		Bitmap image = null;
		if (isDynazoomOpen() && ((ImageView) findViewById(R.id.dynazoom_imageview)).getDrawable() != null)
			image = ((BitmapDrawable) ((ImageView) findViewById(R.id.dynazoom_imageview)).getDrawable()).getBitmap();
		else if (bitmaps.keySet().contains(viewPager.getCurrentItem()))
			image = bitmaps.get(viewPager.getCurrentItem());

		if (image == null)
			return;

		String fileName = Util.saveBitmap(context, image, currentPlugin, load_period);

		if (fileName != null)
			Toast.makeText(this, getString(R.string.text28) + fileName, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(this, getString(R.string.text29), Toast.LENGTH_LONG).show();
	}
	
	private void actionAddLabel() {
		final LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(10, 30, 10, 10);
		final EditText input = new EditText(this);
		ll.addView(input);
		
		new AlertDialog.Builder(Activity_GraphView.this)
		.setTitle(getText(R.string.text70_2))
		.setView(ll)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (!value.trim().equals(""))
					muninFoo.addLabel(new Label(value));
				dialog.dismiss();
				actionLabels();
			}
		}).setNegativeButton(getText(R.string.text64), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		}).show();
	}
	
	private void actionLabels() {
		if (isDynazoomOpen())
			hideDynazoom();

		LinearLayout checkboxesContainer = new LinearLayout(this);
		checkboxesContainer.setPadding(10, 10, 10, 10);
		checkboxesContainer.setOrientation(LinearLayout.VERTICAL);
		final List<CheckBox> checkboxes = new ArrayList<>();
		int i = 0;
		for (Label l : muninFoo.labels) {
			LayoutInflater inflater = LayoutInflater.from(context);
			final View v = inflater.inflate(R.layout.labels_list_checkbox, checkboxesContainer, false);
			checkboxes.add((CheckBox) v.findViewById(R.id.line_0));
			v.findViewById(R.id.line).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					CheckBox cb = (CheckBox) v.findViewById(R.id.line_0);
					cb.setChecked(!cb.isChecked());
				}
			});
			
			if (l.contains(currentPlugin))	checkboxes.get(i).setChecked(true);
			
			((CheckBox) v.findViewById(R.id.line_0)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// Save
					String labelName = ((TextView)v.findViewById(R.id.line_a)).getText().toString();
					MuninPlugin p = currentPlugin;
					if (isChecked)
						muninFoo.getLabel(labelName).addPlugin(p);
					else
						muninFoo.getLabel(labelName).removePlugin(p);
					
					muninFoo.sqlite.saveLabels();
				}
			});
			
			((TextView)v.findViewById(R.id.line_a)).setText(l.getName());
			
			int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
			((CheckBox) v.findViewById(R.id.line_0)).setButtonDrawable(id);
			
			checkboxesContainer.addView(v);
			i++;
		}
		if (muninFoo.labels.size() == 0) {
			TextView tv = new TextView(this);
			tv.setText(getText(R.string.text62));
			tv.setTextSize(18f);
			tv.setPadding(20, 20, 0, 0);
			checkboxesContainer.addView(tv);
		}
		
		AlertDialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.button_labels));
		builder.setView(checkboxesContainer)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		})
		.setNeutralButton(getText(R.string.text70_2), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				actionAddLabel();
			}
		})
		.setNegativeButton(getText(R.string.text64), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		dialog = builder.create();
		dialog.show();
	}

	public void actionOpenInBrowser() {
		try {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentPlugin.getPluginPageUrl()));
			startActivity(browserIntent);
		} catch (Exception ex) { ex.printStackTrace(); }
	}
	
	private void actionFieldsDescription() {
		new FieldsDescriptionFetcher(currentPlugin, this).execute();
	}

	public void addBitmap(Bitmap bitmap, int position) {
		bitmaps.put(position, bitmap);
	}
	public boolean isBitmapNull(int position) {
		return !bitmaps.containsKey(position) || bitmaps.get(position) == null;
	}

	/**
	 * Deleted every bitmap that is out of the BITMAPS_PADDING range from position
	 */
	private void cleanBitmaps(int position) {
		// Can't remove items from a list we're browsing
		List<Integer> toRemove = new ArrayList<>();
		for (Integer i : bitmaps.keySet()) {
			if (i >= position-BITMAPS_PADDING
					&& i <= position+BITMAPS_PADDING)
				continue;

			toRemove.add(i);
		}

		// Remove items
		for (Integer i : toRemove)
			bitmaps.remove(i);
	}
	public Bitmap getBitmap(int position) {
		return bitmaps.containsKey(position) ? bitmaps.get(position) : null;
	}


	private void hideDocumentation() {
		toolbar.setNavigationIcon(toolbar_originalIcon);
		toolbar.setNavigationOnClickListener(drawerHelper.getToggleListener());

		final View documentation = findViewById(R.id.documentation);
		iv_documentation = (ImageView) findViewById(R.id.doc_imageview);
		iv_documentation.setTag("");

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int screenH = size.y;
		TranslateAnimation a1 = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0,
				Animation.ABSOLUTE, screenH);
		a1.setDuration(300);
		a1.setInterpolator(new AccelerateDecelerateInterpolator());
		a1.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) { }
			@Override
			public void onAnimationEnd(Animation animation) {
				documentation.setVisibility(View.GONE);
			}
			@Override public void onAnimationRepeat(Animation animation) { }
		});
		documentation.startAnimation(a1);

		if (isFabShown)
			fab.show();

		createOptionsMenu();
	}
	private void actionDocumentation() {
		menu.clear();

		if (this.toolbar_originalIcon == null)
			this.toolbar_originalIcon = toolbar.getNavigationIcon();

		toolbar.setNavigationIcon(ContextCompat.getDrawable(context, R.drawable.ic_action_navigation_arrow_back));
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		// Get file content
		String fileContent = muninFoo.documentationHelper.getDocumentation(context, currentPlugin, null);
		if (!fileContent.equals("")) {
			// Animation
			View documentation = findViewById(R.id.documentation);

			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int screenH = size.y;
			TranslateAnimation a1 = new TranslateAnimation(
					Animation.RELATIVE_TO_SELF, 0,
					Animation.RELATIVE_TO_SELF, 0,
					Animation.ABSOLUTE, screenH,
					Animation.RELATIVE_TO_SELF, 0);
			a1.setDuration(300);
			a1.setFillAfter(true);
			a1.setInterpolator(new AccelerateDecelerateInterpolator());
			documentation.setVisibility(View.VISIBLE);
			documentation.startAnimation(a1);

			fab.hide();

			// Content filling
			iv_documentation = (ImageView) findViewById(R.id.doc_imageview);
			iv_documentation.setImageBitmap(getBitmap(viewPager.getCurrentItem()));
			iv_documentation.setTag(currentPlugin.getName());

			TextView line1 = (TextView) findViewById(R.id.doc_line1);
			TextView line2 = (TextView) findViewById(R.id.doc_line2);
			line1.setText(currentPlugin.getFancyName());
			line2.setText(currentPlugin.getName());

			final TextView doc = (TextView) findViewById(R.id.doc);
			doc.setText(Html.fromHtml(fileContent));

			Spinner spinner = (Spinner) findViewById(R.id.doc_spinner);
			final List<String> nodes = muninFoo.documentationHelper.getNodes(currentPlugin);

			findViewById(R.id.doc_scrollview).setScrollY(0);

			if (nodes.size() > 1) {
				spinner.setVisibility(View.VISIBLE);
				findViewById(R.id.doc_divider).setVisibility(View.VISIBLE);

				ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
						android.R.layout.simple_spinner_item, nodes);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner.setAdapter(dataAdapter);

				spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
						String node = nodes.get(i);
						if (node.equals(""))
							node = "node";
						String fileContent = muninFoo.documentationHelper.getDocumentation(context, currentPlugin, node);
						doc.setText(Html.fromHtml(fileContent));
					}
					@Override public void onNothingSelected(AdapterView<?> adapterView) { }
				});
			} else spinner.setVisibility(View.GONE);

			findViewById(R.id.imageAndText).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					hideDocumentation();
				}
			});
		}
		else
			Toast.makeText(context, "No doc", Toast.LENGTH_SHORT).show();
	}

	private void actionDynazoom() {
		if (currentPlugin.getInstalledOn().getParent().isDynazoomAvailable() != DynazoomAvailability.TRUE)
			return;

		if (this.toolbar_originalIcon == null)
			this.toolbar_originalIcon = toolbar.getNavigationIcon();

		toolbar.setNavigationIcon(ContextCompat.getDrawable(context, R.drawable.ic_action_navigation_arrow_back));
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		// Animation
		View dynazoom = findViewById(R.id.dynazoom);
		View mainContainer = findViewById(R.id.mainContainer);
		dynazoom.setVisibility(View.VISIBLE);
		int cx = (fab.getLeft() + fab.getRight()) / 2;
		int cy = (fab.getTop() + fab.getBottom()) / 2;
		int finalRadius = Math.max(mainContainer.getWidth(), mainContainer.getHeight());
		Util.Animations.reveal_show(this, dynazoom, new int[]{cx, cy}, finalRadius, Util.Animations.CustomAnimation.SLIDE_IN);

		fab.hide();

		dynazoom_from = DynazoomHelper.getFromPinPoint(load_period);
		dynazoom_to = DynazoomHelper.getToPinPoint();

		((TextView) findViewById(R.id.dynazoom_pluginName)).setText(currentPlugin.getFancyName());
		((TextView) findViewById(R.id.dynazoom_from)).setText(Util.prettyDate(dynazoom_from));
		((TextView) findViewById(R.id.dynazoom_to)).setText(Util.prettyDate(dynazoom_to));

		final View highlight1 = findViewById(R.id.dynazoom_highlight1);
		final View highlight2 = findViewById(R.id.dynazoom_highlight2);
		highlight1.setVisibility(View.GONE);
		highlight2.setVisibility(View.GONE);

		final ImageView imageView = (ImageView) findViewById(R.id.dynazoom_imageview);
		imageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (dynazoomFetcher != null && !dynazoomFetcher.isCancelled())
					dynazoomFetcher.cancel(true);

				dynazoomFetcher = (DynazoomFetcher) new DynazoomFetcher(currentPlugin, imageView,
						(ProgressBar) findViewById(R.id.dynazoom_progressbar), context, muninFoo.getUserAgent(),
						dynazoom_from, dynazoom_to).execute();

				Util.removeOnGlobalLayoutListener(imageView, this);
			}
		});

		final RangeBar rangeBar = (RangeBar) findViewById(R.id.dynazoom_rangebar);
		rangeBar.setTickCount(DynazoomHelper.RANGEBAR_TICKS_COUNT);
		rangeBar.setThumbIndices(0, DynazoomHelper.RANGEBAR_TICKS_COUNT - 1);
		rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
			@Override
			public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
				if (imageView.getDrawable() == null)
					return;

				if (leftThumbIndex == 0 && rightThumbIndex == DynazoomHelper.RANGEBAR_TICKS_COUNT - 1) {
					highlight1.setVisibility(View.GONE);
					highlight2.setVisibility(View.GONE);
				} else {
					if (highlight1.getVisibility() == View.GONE && highlight2.getVisibility() == View.GONE) {
						highlight1.setVisibility(View.VISIBLE);
						highlight2.setVisibility(View.VISIBLE);
					}

					DynazoomHelper.updateHighlightedArea(highlight1, highlight2, rangeBar, (ImageView) findViewById(R.id.dynazoom_imageview));
				}

				int fromIndex = rangeBar.getLeftIndex();
				int toIndex = rangeBar.getRightIndex();

				long new_dynazoom_from = dynazoom_from + (dynazoom_to-dynazoom_from) * fromIndex / DynazoomHelper.RANGEBAR_TICKS_COUNT;
				long new_dynazoom_to = dynazoom_from + (dynazoom_to-dynazoom_from) * toIndex / DynazoomHelper.RANGEBAR_TICKS_COUNT;

				dynazoom_updateFromTo(new_dynazoom_from, new_dynazoom_to);
			}
		});

		imageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (highlight1.getVisibility() == View.GONE || highlight2.getVisibility() == View.GONE)
					return;

				if (rangeBar.getLeftIndex() == rangeBar.getRightIndex())
					return;

				int fromIndex = rangeBar.getLeftIndex();
				int toIndex = rangeBar.getRightIndex();

				dynazoom_from = dynazoom_from + (dynazoom_to-dynazoom_from) * fromIndex / DynazoomHelper.RANGEBAR_TICKS_COUNT;
				dynazoom_to = dynazoom_from + (dynazoom_to-dynazoom_from) * toIndex / DynazoomHelper.RANGEBAR_TICKS_COUNT;

				if (dynazoomFetcher != null && !dynazoomFetcher.isCancelled())
					dynazoomFetcher.cancel(true);

				highlight1.setVisibility(View.GONE);
				highlight2.setVisibility(View.GONE);
				rangeBar.setThumbIndices(0, DynazoomHelper.RANGEBAR_TICKS_COUNT-1);

				dynazoomFetcher = (DynazoomFetcher) new DynazoomFetcher(currentPlugin, imageView,
						(ProgressBar) findViewById(R.id.dynazoom_progressbar), context,
						muninFoo.getUserAgent(), dynazoom_from, dynazoom_to).execute();

				dynazoom_updateFromTo();
			}
		});
	}
	private void hideDynazoom() {
		toolbar.setNavigationIcon(toolbar_originalIcon);
		toolbar.setNavigationOnClickListener(drawerHelper.getToggleListener());

		final View dynazoom = findViewById(R.id.dynazoom);

		View mainContainer = findViewById(R.id.mainContainer);
		int cx = (fab.getLeft() + fab.getRight()) / 2;
		int cy = (fab.getTop() + fab.getBottom()) / 2;
		int initialRadius = Math.max(mainContainer.getWidth(), mainContainer.getHeight());
		Util.Animations.reveal_hide(context, dynazoom, new int[]{cx, cy}, initialRadius, Util.Animations.CustomAnimation.SLIDE_OUT);

		if (isFabShown)
			fab.show();
	}
	private boolean isDynazoomOpen() { return findViewById(R.id.dynazoom).getVisibility() == View.VISIBLE; }
	private void dynazoom_updateFromTo() {
		((TextView) findViewById(R.id.dynazoom_from)).setText(Util.prettyDate(dynazoom_from));
		((TextView) findViewById(R.id.dynazoom_to)).setText(Util.prettyDate(dynazoom_to));
	}
	private void dynazoom_updateFromTo(long from, long to) {
		((TextView) findViewById(R.id.dynazoom_from)).setText(Util.prettyDate(from));
		((TextView) findViewById(R.id.dynazoom_to)).setText(Util.prettyDate(to));
	}

	public void updateConnectionType(BaseResponse.ConnectionType connectionType) {
		ic_secure.setVisibility(View.GONE);
		ic_insecure.setVisibility(View.GONE);

		switch (connectionType) {
			case NORMAL: break;
			case INSECURE: ic_insecure.setVisibility(View.VISIBLE); break;
			case SECURE: ic_secure.setVisibility(View.VISIBLE); break;
		}
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Graphs; }

	@Override
	public void onStop() {
		super.onStop();
		
		if (settings.getBool(Settings.PrefKeys.ScreenAlwaysOn))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
