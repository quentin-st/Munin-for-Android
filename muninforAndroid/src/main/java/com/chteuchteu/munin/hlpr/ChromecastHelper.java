package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * The aim of this class is to avoid flooding an activity with ChromeCast code.
 * The activity classes will be created here and manually called from activity.
 */
public class ChromecastHelper {
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

	private ChromecastHelper(Context context) {
		this.context = context;
	}
	public static ChromecastHelper create(Context context) { return new ChromecastHelper(context); }

	public void onCreate() {
		mMediaRouter = MediaRouter.getInstance(context);
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(MuninFoo.CHROMECAST_APPLICATION_ID))
				.build();
		mMediaRouterCallback = new CustomMediaRouterCallback();
	}

	private static void log(String msg) { MuninFoo.log("Chromecast", msg); }

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
					if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
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
							.launchApplication(mApiClient, MuninFoo.CHROMECAST_APPLICATION_ID, false)
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

												// set the initial instructions
												// on the receiver
												sendMessage("Nuthin");
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
						Cast.CastApi.stopApplication(mApiClient, mSessionId);
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
	public void sendMessage(String message) {
		if (mApiClient != null && mHelloWorldChannel != null) {
			try {
				log("Sending message [" + message + "]...");
				Cast.CastApi.sendMessage(mApiClient, mHelloWorldChannel.getNamespace(), message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								log("sendMessage.onResult. Success: " + result.isSuccess() + ", " + result.toString());
								if (!result.isSuccess())
									log("Sending message failed");
							}
						});
			} catch (Exception e) {
				log("Exception while sending message: ");
				e.printStackTrace();
			}
		} else
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Custom message channel
	 */
	class CustomMessageChannel implements Cast.MessageReceivedCallback {
		public String getNamespace() { return MuninFoo.CHROMECAST_CHANNEL_NAMESPACE; }

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
}
