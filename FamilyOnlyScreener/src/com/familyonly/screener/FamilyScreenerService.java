package com.familyonly.screener;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class FamilyScreenerService extends CallScreeningService {

    private static final String TAG = "FamilyOnly";

    @Override
    public void onScreenCall(Call.Details details) {
        boolean isIncoming = details.getCallDirection() == Call.Details.DIRECTION_INCOMING;
        String handle = null;
        try {
            Uri h = details.getHandle();
            if (h != null) handle = h.getSchemeSpecificPart();
        } catch (Exception e) {
            Log.w(TAG, "handle parse failed", e);
        }

        if (!isIncoming) {
            respondToCall(details, new CallResponse.Builder().setDisallowCall(false).build());
            return;
        }

        Set<String> whitelist = loadWhitelist();
        String incomingNorm = MainActivity.normalize(handle == null ? "" : handle);

        Log.i(TAG, "incoming=" + handle + " norm=" + incomingNorm
                + " whitelistSize=" + whitelist.size());

        if (whitelist.isEmpty()) {
            // Safety: do not lock user out before they add numbers.
            Log.w(TAG, "whitelist empty, allowing call");
            respondToCall(details,
                    new CallResponse.Builder().setDisallowCall(false).build());
            return;
        }

        boolean allowed = false;
        if (incomingNorm.length() >= 10) {
            for (String w : whitelist) {
                String wn = MainActivity.normalize(w);
                if (!wn.isEmpty() && wn.equals(incomingNorm)) {
                    allowed = true;
                    break;
                }
            }
        } else {
            Log.w(TAG, "no caller id, blocking");
        }

        CallResponse.Builder b = new CallResponse.Builder();
        if (allowed) {
            Log.i(TAG, "ALLOW " + handle);
            b.setDisallowCall(false);
        } else {
            Log.i(TAG, "BLOCK " + handle);
            b.setDisallowCall(true);
            b.setRejectCall(true);
            b.setSkipCallLog(true);
            b.setSkipNotification(true);
            scheduleLogCleanup(handle, incomingNorm);
        }
        respondToCall(details, b.build());
    }

    private void scheduleLogCleanup(final String handle, final String norm) {
        // Telecom on this device still writes "logged" despite setSkipCallLog(true).
        // Delete the trace after it lands. No anonymous class (old d8 bug).
        Thread t = new Thread(new CleanupRunner(this, handle, norm));
        t.start();
    }

    private Set<String> loadWhitelist() {
        try {
            SharedPreferences p = getSharedPreferences(
                    MainActivity.PREFS, Context.MODE_PRIVATE);
            Set<String> s = p.getStringSet(MainActivity.KEY_NUMBERS,
                    new HashSet<String>());
            return s == null ? new HashSet<String>() : s;
        } catch (Exception e) {
            Log.w(TAG, "prefs read failed", e);
            return new HashSet<String>();
        }
    }
}
