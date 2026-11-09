package com.familyonly.screener;

import android.content.Context;
import android.util.Log;

public class CleanupRunner implements Runnable {

    private static final String TAG = "FamilyOnly";

    private final Context appCtx;
    private final String handle;
    private final String norm;

    public CleanupRunner(Context ctx, String handle, String norm) {
        this.appCtx = ctx.getApplicationContext();
        this.handle = handle;
        this.norm = norm;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            int deleted = LogCleaner.deleteNumberFromLog(appCtx, norm);
            Log.i(TAG, "cleanup " + handle + " deleted=" + deleted);
            Thread.sleep(10000);
            int deleted2 = LogCleaner.deleteNumberFromLog(appCtx, norm);
            Log.i(TAG, "cleanup2 " + handle + " deleted=" + deleted2);
        } catch (Exception e) {
            Log.w(TAG, "cleanup failed", e);
        }
    }
}
