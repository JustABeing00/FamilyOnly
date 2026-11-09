package com.familyonly.screener;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

public class LogCleaner {

    private static final String TAG = "FamilyOnly";

    // Delete system call-log rows for one normalized (last-10) number.
    // Returns rows deleted, or -1 if permission missing.
    public static int deleteNumberFromLog(Context ctx, String norm) {
        if (norm == null || norm.length() < 10) return 0;
        String last10 = norm.length() > 10
                ? norm.substring(norm.length() - 10) : norm;
        try {
            // Match +91 / 0-prefixed variants stored by Dialer.
            String sel = CallLog.Calls.NUMBER + " LIKE ?";
            String[] args = new String[]{"%" + last10};
            int deleted = ctx.getContentResolver().delete(
                    CallLog.Calls.CONTENT_URI, sel, args);
            // Also sweep: any non-whitelisted missed/rejected rows from last 2 min
            // are handled by MainActivity.cleanAllBlocked() on open.
            return deleted;
        } catch (SecurityException e) {
            Log.w(TAG, "cleanup needs WRITE_CALL_LOG", e);
            return -1;
        } catch (Exception e) {
            Log.w(TAG, "cleanup failed", e);
            return 0;
        }
    }

    // Delete log rows for every number NOT in whitelist (used on app open).
    public static int cleanAllBlocked(Context ctx) {
        int total = 0;
        try {
            java.util.Set<String> wl = ctx.getSharedPreferences(
                    MainActivity.PREFS, Context.MODE_PRIVATE)
                    .getStringSet(MainActivity.KEY_NUMBERS, null);
            java.util.HashSet<String> wlNorm = new java.util.HashSet<String>();
            if (wl != null) {
                for (String w : wl) {
                    String n = MainActivity.normalize(w);
                    if (!n.isEmpty()) wlNorm.add(n);
                }
            }
            Uri uri = CallLog.Calls.CONTENT_URI;
            String[] proj = new String[]{CallLog.Calls.NUMBER};
            Cursor c = ctx.getContentResolver().query(uri, proj, null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c == null) return 0;
            java.util.HashSet<String> toDelete = new java.util.HashSet<String>();
            while (c.moveToNext()) {
                String num = c.getString(0);
                String n = MainActivity.normalize(num == null ? "" : num);
                if (!n.isEmpty() && !wlNorm.contains(n)) {
                    toDelete.add(n);
                }
            }
            c.close();
            for (String n : toDelete) {
                int d = deleteNumberFromLog(ctx, n);
                if (d > 0) total += d;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "cleanAll needs WRITE_CALL_LOG", e);
        } catch (Exception e) {
            Log.w(TAG, "cleanAll failed", e);
        }
        return total;
    }
}
