package com.oasisfeng.island.provisioning;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import com.oasisfeng.island.InternalBroadcastReceiver;
import com.oasisfeng.island.util.DevicePolicies;
import com.oasisfeng.island.util.Users;

import java8.util.Optional;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

/**
 * Receiver for starting post-provisioning procedure for manual provisioning.
 * The enabled state of this receiver also serves as an indication of pending manual provisioning.
 *
 * Created by Oasis on 2017/4/8.
 */
public abstract class ManualProvisioningReceiver extends InternalBroadcastReceiver {

	@Override public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (action == null || Intent.ACTION_USER_INITIALIZE.equals(action)) {
			if (Users.isOwner()) return;	// Should never happen
			if (Users.profile == null) {
				Log.d(TAG, "Profile is disabled");	// Profile is not enabled yet, that means we are currently in the managed provisioning flow
				return;									// Nothing needs to be done here, we will receive ACTION_PROFILE_PROVISIONING_COMPLETE soon.
			}
			final Optional<Boolean> is_profile_owner = DevicePolicies.isProfileOwner(context, Process.myUserHandle());
			if (is_profile_owner == null || ! is_profile_owner.orElse(false)) {	// Current user is not profile, or we are not profile owner
				Log.d(TAG, "Not profile owner");
				return;
			}
			Log.i(TAG, (action != null ? "User initialized: " : "Provisioning resumed: ") + Users.toId(android.os.Process.myUserHandle()));
			IslandProvisioning.start(context, action);
			context.getPackageManager().setComponentEnabledSetting(getComponent(context), COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP);
		} else if (DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED.equals(action)){
			Log.i(TAG, "Device owner changed.");
			if (new DevicePolicies(context).isDeviceOwner()) IslandProvisioning.start(context, action);
		}
	}

	private static final String TAG = ManualProvisioningReceiver.class.getSimpleName();
}
