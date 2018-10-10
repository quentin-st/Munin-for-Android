package com.chteuchteu.munin.ntfs.push;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.ui.Fragment_Notifications_Push;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

public class RegistrationIntentService extends IntentService {
    public RegistrationIntentService() {
        super("RegIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_pref", Context.MODE_PRIVATE);

        try {
            // Initially this call goes out to the network to retrieve the token, subsequent calls are local.
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            sharedPreferences.edit().putString(Settings.PrefKeys.Notifs_Push_regId.getKey(), token).apply();
        } catch (Exception e) {
            e.printStackTrace();
            sharedPreferences.edit().putString(Settings.PrefKeys.Notifs_Push_regId.getKey(), null).apply();
        }

        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Fragment_Notifications_Push.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }
}
