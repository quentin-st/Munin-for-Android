package com.chteuchteu.munin.hlpr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.chteuchteu.munin.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionsHelper {
    public static final int REQUEST_CODE_ASK_SINGLE_PERMISSION = 9876;
    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 9875;

    /**
     * Ensures that app has permission to achieve action. If true is returned, the action
     *  can be executed right away.
     * @param activity Activity
     * @param permission String
     * @return true if we have the permission
     */
    public static boolean ensurePermission(final Activity activity, final String permission) {
        // SDK < 23: permission has been granted at install
        if (Build.VERSION.SDK_INT < 23)
            return true;

        if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            return true;

        // Ask for permission
        // User checked "Never ask again", let's force-ask him
        if (!activity.shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.permission_required))
                    .setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            askForPermission(activity, permission);
                        }
                    })
                    .setNegativeButton(activity.getString(R.string.text64), null)
                    .create()
                    .show();

            return false;
        }

        // Ask for it
        askForPermission(activity, permission);
        return false;
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults, Runnable onAllGranted) {
        switch (requestCode) {
            case PermissionsHelper.REQUEST_CODE_ASK_SINGLE_PERMISSION:
            case PermissionsHelper.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                if (PermissionsHelper.allGranted(permissions, grantResults))
                    onAllGranted.run();
                else
                    Toast.makeText(activity, R.string.permission_missing, Toast.LENGTH_LONG).show();
                break;
        }
    }

    /**
     * Ensures that app has permissions to achieve action. If true is returned, the action
     *  can be executed right away.
     * @param activity Activity
     * @param permissions String[]
     * @return true if we have the permission
     */
    public static boolean ensurePermissions(final Activity activity, final String[] permissions) {
        // SDK < 23: permission has been granted at install
        if (Build.VERSION.SDK_INT < 23)
            return true;

        // Permissions granted for all
        List<String> missingPermissions = new ArrayList<>();
        boolean allGranted = true;
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                missingPermissions.add(permission);
            }
        }

        if (allGranted)
            return true;

        // Ask for permissions

        // User checked "Never ask again", let's force-ask him
        for (final String permission : permissions) {
            if (!activity.shouldShowRequestPermissionRationale(permission)) {
                missingPermissions.remove(permission);

                new AlertDialog.Builder(activity)
                        .setMessage(activity.getString(R.string.permission_required))
                        .setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                askForPermission(activity, permission);
                            }
                        })
                        .setNegativeButton(activity.getString(R.string.text64), null)
                        .create()
                        .show();
            }
        }

        // Ask for permissions
        if (!missingPermissions.isEmpty())
            askForPermissions(activity, missingPermissions.toArray(new String[missingPermissions.size()]));

        return false;
    }

    private static Map<String, Integer> getResultHash(String[] permissions, int[] granted) {
        Map<String, Integer> perms = new HashMap<>();

        for (int i=0; i<permissions.length; i++)
            perms.put(permissions[i], granted[i]);

        return perms;
    }

    public static boolean allGranted(String[] permissions, int[] granted) {
        Map<String, Integer> hash = getResultHash(permissions, granted);

        for (int i=0; i<hash.size(); i++) {
            if (hash.get(permissions[i]) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    private static void askForPermission(Activity activity, String permission) {
        askForPermissions(activity, new String[] {permission});
    }

    private static void askForPermissions(Activity activity, String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23)
            activity.requestPermissions(permissions, REQUEST_CODE_ASK_SINGLE_PERMISSION);
    }
}
