package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.os.Bundle;
import androidx.core.view.MenuItemCompat;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninNode;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * The aim of this class is to avoid flooding an activity with ChromeCast code.
 * The activity corresponding methods will be manually called from activity.
 */
public class ChromecastHelper {
    public static final String CHROMECAST_APPLICATION_ID = "31C83628";
    public static final String CHROMECAST_CHANNEL_NAMESPACE = "urn:x-cast:com.chteuchteu.munin";

	private Context context;

	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	private CustomMessageChannel mHelloWorldChannel;
	private boolean mApplicationStarted;
	private boolean mWaitingForReconnect;
	private String mSessionId;
	private Runnable onConnectionSuccess;

	private ChromecastHelper(Context context) {
		this.context = context;
	}
	public static ChromecastHelper create(Context context) { return new ChromecastHelper(context); }

	public void onCreate(Runnable onConnectionSuccess) {
		mMediaRouter = MediaRouter.getInstance(context);
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(getChromecastApplicationId(context)))
				.build();
		mMediaRouterCallback = new CustomMediaRouterCallback();
		this.onConnectionSuccess = onConnectionSuccess;
	}

	private static void log(String msg) { MuninFoo.log("Chromecast", msg); }

    public boolean isConnected() {
        return mApiClient != null
                && mHelloWorldChannel != null;
    }

    public static boolean isConnected(ChromecastHelper chromecastHelperInstance) {
        return chromecastHelperInstance != null
                && chromecastHelperInstance.isConnected();
    }

	/**
	 * Callback for MediaRouter events
	 */
	private class CustomMediaRouterCallback extends MediaRouter.Callback {
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
			shutdownConnection();
			mSelectedDevice = null;
		}
	}

	/**
	 * Start the receiver app
	 */
	private void launchReceiver() {
		try {
			Cast.Listener mCastListener = new Cast.Listener() {
				@Override
				public void onApplicationDisconnected(int errorCode) {
					log("Application has stopped. Error code: " + errorCode);
					shutdownConnection();
				}
			};

			// Connect to Google Play services
			ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
			ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
					.builder(mSelectedDevice, mCastListener);
			mApiClient = new GoogleApiClient.Builder(context)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(mConnectionCallbacks)
					.addOnConnectionFailedListener(mConnectionFailedListener)
					.build();

			mApiClient.connect();
		} catch (Exception e) {
			log("Failed launchReceiver");
			e.printStackTrace();
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			log("onConnected");

			if (mApiClient == null) {
				// We got disconnected while this runnable was pending
				// execution.
				return;
			}

			try {
				if (mWaitingForReconnect) {
					mWaitingForReconnect = false;

					// Check if the receiver app is still running
					if (connectionHint != null && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
						log("App is no longer running");
						shutdownConnection();
					} else {
						// Re-create the custom message channel
						try {
							Cast.CastApi.setMessageReceivedCallbacks(
									mApiClient,
									mHelloWorldChannel.getNamespace(),
									mHelloWorldChannel);
						} catch (IOException e) {
							log("Exception while creating channel");
							e.printStackTrace();
						}
					}
				} else {
					// Launch the receiver app
					Cast.CastApi
							.launchApplication(mApiClient, getChromecastApplicationId(context), false)
							.setResultCallback(
									new ResultCallback<Cast.ApplicationConnectionResult>() {
										@Override
										public void onResult(Cast.ApplicationConnectionResult result) {
											Status status = result.getStatus();
											log("ApplicationConnectionResultCallback.onResult: statusCode"
													+ status.getStatusCode());

											if (status.isSuccess()) {
												ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
												mSessionId = result.getSessionId();
												String applicationStatus = result.getApplicationStatus();
												boolean wasLaunched = result.getWasLaunched();

												log("application name: " + applicationMetadata.getName()
																+ ", status: " + applicationStatus
																+ ", sessionId: " + mSessionId
																+ ", wasLaunched: " + wasLaunched);
												mApplicationStarted = true;

												// Create the custom message
												// channel
												mHelloWorldChannel = new CustomMessageChannel();
												try {
													Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mHelloWorldChannel.getNamespace(),
															mHelloWorldChannel);
												} catch (IOException e) {
													log("Exception while creating channel");
													e.printStackTrace();
												}

												if (onConnectionSuccess != null)
													onConnectionSuccess.run();
											} else {
												log("Application could not launch");
												shutdownConnection();
											}
										}
									});
				}
			} catch (Exception e) {
				log("Failed to launch application");
				e.printStackTrace();
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			log("onConnectionSuspended, cause: " + cause);
			mWaitingForReconnect = true;
		}
	}

	/**
	 * Google Play services callbacks
	 */
	private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			log("Connection failed. Shutting down connection.");

			shutdownConnection();
		}
	}

	/**
	 * Tear down the connection to the receiver
	 */
	private void shutdownConnection() {
		log("Shutting down connection");
		if (mApiClient != null) {
			if (mApplicationStarted) {
				if (mApiClient.isConnected() || mApiClient.isConnecting()) {
					try {
						// Don't stop receiver app when the phone becomes idle
						//Cast.CastApi.stopApplication(mApiClient, mSessionId);
						if (mHelloWorldChannel != null) {
							Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mHelloWorldChannel.getNamespace());
							mHelloWorldChannel = null;
						}
					} catch (IOException e) {
						log("Exception while removing channel");
						e.printStackTrace();
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
	 */
	private void sendMessage(final String message) {
		if (mApiClient != null && mHelloWorldChannel != null) {
			try {
				log("Sending message [" + message + "]...");
				Cast.CastApi.sendMessage(mApiClient, mHelloWorldChannel.getNamespace(), message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								if (!result.isSuccess()) {
									log("Sending message failed - " + result.toString());
									try {
										log("The message may be too long (" + message.getBytes("UTF-8").length);
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
							}
						});
			} catch (Exception e) {
				log("Exception while sending message: ");
				e.printStackTrace();
			}
		}
	}

	public void sendMessage_inflateGrid(Grid grid, MuninPlugin.Period period) {
        if (mApiClient == null || mHelloWorldChannel == null)
            return;

		// Show a warning toast about Chromecast feature not being available
		// with apache digest/basic auth
		boolean warningToast = false;

		try {
			JSONObject msg = new JSONObject();
			msg.put("action", "inflate_grid");
			msg.put("gridName", grid.getName());
            msg.put("period", period.name());
			JSONArray msg_gridItems = new JSONArray();
			for (GridItem item : grid.getItems()) {
				MuninNode node = item.getPlugin().getInstalledOn();
				MuninMaster master = node.getParent();
				if (master.isAuthNeeded())
					warningToast = true;

				JSONObject msg_GridItem = new JSONObject();

				msg_GridItem.put("x", item.getX());
				msg_GridItem.put("y", item.getY());
				msg_GridItem.put("graphUrl", item.getPlugin().getImgUrl("{period}"));
				if (master.isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE)
					msg_GridItem.put("hdGraphUrl", item.getPlugin().getHDImgUrlWithPlaceholders());
				msg_GridItem.put("pluginName", item.getPlugin().getFancyName());
				msg_GridItem.put("nodeName", node.getName());
				msg_GridItem.put("masterName", master.getName());

				msg_gridItems.put(msg_GridItem);
			}
			msg.put("gridItems", msg_gridItems);

			sendMessage(msg.toString());
		} catch (JSONException ex) {
			ex.printStackTrace();
		}

		if (warningToast)
			Toast.makeText(context, R.string.chromecastAuthWarning, Toast.LENGTH_LONG).show();
	}

	public void sendMessage_preview(GridItem gridItem) {
        if (mApiClient == null || mHelloWorldChannel == null)
            return;

		try {
			JSONObject msg = new JSONObject();
			msg.put("action", "preview");
			msg.put("x", gridItem.getX());
			msg.put("y", gridItem.getY());

			sendMessage(msg.toString());
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}

    public void sendMessage_changePeriod(MuninPlugin.Period period) {
        if (mApiClient == null || mHelloWorldChannel == null)
            return;

        try {
            JSONObject msg = new JSONObject();
            msg.put("action", "change_period");
            msg.put("period", period.name());

            sendMessage(msg.toString());
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

	public enum SimpleChromecastAction { CANCEL_PREVIEW, REFRESH }
	public void sendMessage(SimpleChromecastAction chromecastAction) {
        if (mApiClient == null || mHelloWorldChannel == null)
            return;

		try {
			JSONObject msg = new JSONObject();
			msg.put("action", chromecastAction.name().toLowerCase());

			sendMessage(msg.toString());
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Custom message channel
	 */
	class CustomMessageChannel implements Cast.MessageReceivedCallback {
		public String getNamespace() { return CHROMECAST_CHANNEL_NAMESPACE; }

		/*
		 * Receive message from the receiver app
		 */
		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
			log("onMessageReceived: " + message);
		}
	}

	public void createOptionsMenu(Menu menu) {
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
	}
	public void onResume() {
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}
	public void onPause() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
	}
	public void onStart() {
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}
	public void onStop() {
		mMediaRouter.removeCallback(mMediaRouterCallback);
	}

    public static String getChromecastApplicationId(Context context) {
		return Settings.getInstance(context).getString(Settings.PrefKeys.ChromecastApplicationId, CHROMECAST_APPLICATION_ID);
    }
}
