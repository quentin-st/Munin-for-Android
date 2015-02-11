package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Activity_Grid extends MuninActivity implements IGridActivity {
	private MenuItem menu_refresh;
	private MenuItem menu_edit;
	private MenuItem menu_period;
	private MenuItem menu_open;

	/**
	 * Temporary grid object used activity instantiation. Should not be used
	 *  for main interactions with grid (ie. grid.currentlyOpenedGridItem)
	 */
	private Grid tmpGrid;
	private List<Grid> grids;
	private Handler mHandler;
	private Runnable mHandlerTask;

	private Fragment_Grid fragment;

	// Chromecast
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private Cast.Listener mCastListener;
	private ConnectionCallbacks mConnectionCallbacks;
	private ConnectionFailedListener mConnectionFailedListener;
	private HelloWorldChannel mHelloWorldChannel;
	private boolean mApplicationStarted;
	private boolean mWaitingForReconnect;
	private String mSessionId;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grid);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Init fragment
		this.fragment = new Fragment_Grid();
		// Pass the gridId
		Bundle bundle = new Bundle();
		long gridId = getIntent().getExtras().getLong("gridId");
		bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
		this.fragment.setArguments(bundle);
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, this.fragment).commit();

		this.grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);
		// tmpGrid: temporary grid object used to instantiate activity.
		// Should not be used afterwards (use Fragment_Grid.grid instead)
		tmpGrid = getGrid(grids, gridId);
		actionBar.setTitle(getText(R.string.text75) + " " + tmpGrid.getName());

		if (!Util.isOnline(context))
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();

		
		// ActionBar spinner if needed
		if (grids.size() > 1) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);

			SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(getApplicationContext(),
					android.R.layout.simple_spinner_dropdown_item, getGridsNames(grids));
			
			ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					if (itemPosition != grids.indexOf(tmpGrid)) {
						long gridId = grids.get(itemPosition).getId();
						tmpGrid = getGrid(grids, gridId);

						// If editing / previewing: cancel those
						if (fragment.isPreviewing())
							fragment.hidePreview();

						if (fragment.isEditing())
							fragment.edit();

						fragment = new Fragment_Grid();
						Bundle bundle = new Bundle();
						bundle.putLong(Fragment_Grid.ARG_GRIDID, grids.get(itemPosition).getId());
						fragment.setArguments(bundle);
						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
					}
					return false;
				}
			};
			actionBar.setListNavigationCallbacks(spinnerAdapter, navigationListener);
			if (grids.indexOf(tmpGrid) != -1)
				actionBar.setSelectedNavigationItem(grids.indexOf(tmpGrid));
		}

		// Launch periodical check
		if (Util.getPref(this, Util.PrefKeys.AutoRefresh).equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					if (!fragment.isUpdating())
						fragment.autoRefresh();
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}

		// Chromecast
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(MuninFoo.CHROMECAST_APPLICATION_ID))
				.build();
		mMediaRouterCallback = new MyMediaRouterCallback();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	protected void onPause() {
		if (isFinishing())
			mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	public void onStop() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onStop();

		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private static Grid getGrid(List<Grid> grids, long gridId) {
		for (Grid grid : grids) {
			if (grid.getId() == gridId)
				return grid;
		}
		return null;
	}

	private static List<String> getGridsNames(List<Grid> grids) {
		List<String> list = new ArrayList<>();
		for (Grid grid : grids)
			list.add(grid.getName());
		return list;
	}

	@Override
	public void updatePeriodMenuItem(Period period) {
		if (menu_period != null)
			menu_period.setTitle(period.getLabel(this));
	}

	@Override
	public void onPreview() {
		menu_open.setVisible(true);
		menu_period.setVisible(false);
		menu_refresh.setVisible(false);
		menu_edit.setVisible(false);
	}

	@Override
	public void onPreviewHide() {
		if (menu_refresh != null)	menu_refresh.setVisible(true);
		if (menu_edit != null)		menu_edit.setVisible(true);
		if (menu_period != null)	menu_period.setVisible(true);
		if (menu_open != null)		menu_open.setVisible(false);
	}

	@Override
	public void onEditModeChange(boolean editing) {
		if (menu_refresh != null)	menu_refresh.setVisible(editing);
		if (menu_period != null)	menu_period.setVisible(editing);
		if (menu_edit != null)
			menu_edit.setIcon(
					editing ? R.drawable.ic_action_image_edit
							: R.drawable.ic_action_navigation_check);
	}

	@Override
	public void onGridLoaded(Grid grid) { }

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.grid, menu);
		menu_refresh = menu.findItem(R.id.menu_refresh);
		menu_edit = menu.findItem(R.id.menu_edit);
		menu_period = menu.findItem(R.id.menu_period);
		menu_open = menu.findItem(R.id.menu_open);
		menu_refresh.setVisible(!fragment.isEditing());
		if (fragment.isEditing())
			menu_edit.setIcon(R.drawable.ic_action_navigation_check);
		else
			menu_edit.setIcon(R.drawable.ic_action_image_edit);
		menu_period.setTitle(fragment.getCurrentPeriod().getLabel(this));

		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
	}

	private void openGraph() {
		if (fragment.getGrid().currentlyOpenedGridItem == null)
			return;

		fragment.getGrid().f.setCurrentServer(fragment.getGrid().currentlyOpenedGridItem.getPlugin().getInstalledOn());
		Intent i = new Intent(context, Activity_GraphView.class);
		i.putExtra("plugin", fragment.getGrid().currentlyOpenedGridItem.getPlugin().getName());
		i.putExtra("from", "grid");
		Intent gridIntent = ((Activity) context).getIntent();
		if (gridIntent != null && gridIntent.getExtras() != null && gridIntent.getExtras().containsKey("gridName"))
			i.putExtra("fromGrid", gridIntent.getExtras().getString("gridName"));
		context.startActivity(i);
		Util.setTransition(context, TransitionStyle.DEEPER);
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Grid; }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_refresh: fragment.refresh(); return true;
			case R.id.menu_edit: fragment.edit(); return true;
			case R.id.period_day:
				fragment.setCurrentPeriod(Period.DAY);
				menu_period.setTitle(Period.DAY.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_week:
				fragment.setCurrentPeriod(Period.WEEK);
				menu_period.setTitle(Period.WEEK.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_month:
				fragment.setCurrentPeriod(Period.MONTH);
				menu_period.setTitle(Period.MONTH.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.period_year:
				fragment.setCurrentPeriod(Period.YEAR);
				menu_period.setTitle(Period.YEAR.getLabel(context));
				fragment.refresh();
				return true;
			case R.id.menu_open: openGraph(); return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		if (findViewById(R.id.fullscreen).getVisibility() == View.VISIBLE)
			fragment.hidePreview();
		else {
			if (fragment.isEditing())
				fragment.edit(); // quit edit mode
			else {
				Intent intent = new Intent(this, Activity_Grids.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.SHALLOWER);
			}
		}
	}

	/* Chromecast */
	private String TAG = "chromecast";
	/**
	 * Callback for MediaRouter events
	 */
	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
			log("onRouteSelected");
			// Handle the user route selection.
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

			launchReceiver();
		}

		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
			log("onRouteUnselected: info=" + info);
			teardown();
			mSelectedDevice = null;
		}
	}

	/**
	 * Start the receiver app
	 */
	private void launchReceiver() {
		try {
			mCastListener = new Cast.Listener() {

				@Override
				public void onApplicationDisconnected(int errorCode) {
					log("application has stopped");
					teardown();
				}

			};
			// Connect to Google Play services
			mConnectionCallbacks = new ConnectionCallbacks();
			mConnectionFailedListener = new ConnectionFailedListener();
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
					.builder(mSelectedDevice, mCastListener);
			mApiClient = new GoogleApiClient.Builder(this)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(mConnectionCallbacks)
					.addOnConnectionFailedListener(mConnectionFailedListener)
					.build();

			mApiClient.connect();
		} catch (Exception e) {
			Log.e(TAG, "Failed launchReceiver", e);
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionCallbacks implements
			GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "onConnected");

			if (mApiClient == null) {
				// We got disconnected while this runnable was pending
				// execution.
				return;
			}

			try {
				if (mWaitingForReconnect) {
					mWaitingForReconnect = false;

					// Check if the receiver app is still running
					if ((connectionHint != null)
							&& connectionHint
							.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
						Log.d(TAG, "App  is no longer running");
						teardown();
					} else {
						// Re-create the custom message channel
						try {
							Cast.CastApi.setMessageReceivedCallbacks(
									mApiClient,
									mHelloWorldChannel.getNamespace(),
									mHelloWorldChannel);
						} catch (IOException e) {
							Log.e(TAG, "Exception while creating channel", e);
						}
					}
				} else {
					// Launch the receiver app
					Cast.CastApi
							.launchApplication(mApiClient, MuninFoo.CHROMECAST_APPLICATION_ID, false)
							.setResultCallback(
									new ResultCallback<Cast.ApplicationConnectionResult>() {
										@Override
										public void onResult(
												Cast.ApplicationConnectionResult result) {
											Status status = result.getStatus();
											Log.d(TAG,
													"ApplicationConnectionResultCallback.onResult: statusCode"
															+ status.getStatusCode());
											Log.d(TAG, "...onResult: message: " + status.getStatusMessage());
											if (status.isSuccess()) {
												ApplicationMetadata applicationMetadata = result
														.getApplicationMetadata();
												mSessionId = result
														.getSessionId();
												String applicationStatus = result
														.getApplicationStatus();
												boolean wasLaunched = result
														.getWasLaunched();
												Log.d(TAG,
														"application name: "
																+ applicationMetadata
																.getName()
																+ ", status: "
																+ applicationStatus
																+ ", sessionId: "
																+ mSessionId
																+ ", wasLaunched: "
																+ wasLaunched);
												mApplicationStarted = true;

												// Create the custom message
												// channel
												mHelloWorldChannel = new HelloWorldChannel();
												try {
													Cast.CastApi
															.setMessageReceivedCallbacks(
																	mApiClient,
																	mHelloWorldChannel.getNamespace(),
																	mHelloWorldChannel);
												} catch (IOException e) {
													Log.e(TAG,
															"Exception while creating channel",
															e);
												}

												// set the initial instructions
												// on the receiver
												sendMessage("Nuthin");
											} else {
												Log.e(TAG,
														"application could not launch");
												teardown();
											}
										}
									});
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "onConnectionSuspended");
			mWaitingForReconnect = true;
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionFailedListener implements
			GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.e(TAG, "onConnectionFailed ");

			teardown();
		}
	}

	/**
	 * Tear down the connection to the receiver
	 */
	private void teardown() {
		Log.d(TAG, "teardown");
		if (mApiClient != null) {
			if (mApplicationStarted) {
				if (mApiClient.isConnected() || mApiClient.isConnecting()) {
					try {
						Cast.CastApi.stopApplication(mApiClient, mSessionId);
						if (mHelloWorldChannel != null) {
							Cast.CastApi.removeMessageReceivedCallbacks(
									mApiClient,
									mHelloWorldChannel.getNamespace());
							mHelloWorldChannel = null;
						}
					} catch (IOException e) {
						Log.e(TAG, "Exception while removing channel", e);
					}
					mApiClient.disconnect();
				}
				mApplicationStarted = false;
			}
			mApiClient = null;
		}
		mSelectedDevice = null;
		mWaitingForReconnect = false;
		mSessionId = null;
	}

	/**
	 * Send a text message to the receiver
	 * @param message
	 */
	private void sendMessage(String message) {
		if (mApiClient != null && mHelloWorldChannel != null) {
			try {
				Cast.CastApi.sendMessage(mApiClient,
						mHelloWorldChannel.getNamespace(), message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								if (!result.isSuccess()) {
									Log.e(TAG, "Sending message failed");
								}
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Exception while sending message", e);
			}
		} else {
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Custom message channel
	 */
	class HelloWorldChannel implements Cast.MessageReceivedCallback {

		/**
		 * @return custom namespace
		 */
		public String getNamespace() {
			return MuninFoo.CHROMECAST_CHANNEL_NAMESPACE;
		}

		/*
		 * Receive message from the receiver app
		 */
		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace,
		                              String message) {
			Log.d(TAG, "onMessageReceived: " + message);
		}

	}
}
