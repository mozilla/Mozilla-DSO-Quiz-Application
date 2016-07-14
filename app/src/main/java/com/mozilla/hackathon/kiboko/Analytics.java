package com.mozilla.hackathon.kiboko;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by secretrobotron on 7/2/16.
 */

public class Analytics {

    private static final String ANALYTICS_FILENAME = "jisort_analytics.txt";
    private static final String ANALYTICS_ARCHIVE_FILENAME = "jisort_analytics.1.txt";
    private static final long TIME_BETWEEN_SAVES= 5000;
    private static final long FILE_SIZE_LIMIT = 100000; //bytes

    private class AnalyticsItem {
        String mName;
        String mData = null;
        Date mTime;

        public AnalyticsItem(String name) {
            mName = name;
            mTime = new Date(System.currentTimeMillis());
        }

        public AnalyticsItem(String name, String data) {
            mName = name;
            mData = data;
            mTime = new Date(System.currentTimeMillis());
        }

        public String toString() {
            String output = "[" + mTime.toString() + "] " + mName;
            if (mData != null) {
                output += " -> (" + mData.toString() + ")";
            }
            return output;
        }
    }

    protected static Analytics sAnalytics = null;

    private List<AnalyticsItem> mItems;

    private long mLastSaveTime = 0;

    private Analytics() {
        mItems = new ArrayList<AnalyticsItem>();

        // Prevent from saving right away.
        mLastSaveTime = System.currentTimeMillis();
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void flushItems() {
        save(true);
    }

    private void copyOldAnalytics() throws IOException {
        InputStream in = new FileInputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ANALYTICS_FILENAME));
        OutputStream out = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ANALYTICS_ARCHIVE_FILENAME));

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void save() {
        save(false);
    }

    private void save(boolean flush) {
        long now = System.currentTimeMillis();

        if (!flush && now - mLastSaveTime < TIME_BETWEEN_SAVES) {
            return;
        }

        mLastSaveTime = now;

        String output = "";

        // this should run every once in a while to save to perm memory
        for (AnalyticsItem item : mItems) {
            output += item.toString() + "\n";
        }

        // Replace this with save to disk functionality
        FileOutputStream outputStream;

        if (isExternalStorageWritable()) {
            try {
                File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ANALYTICS_FILENAME);
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                else if (outputFile.length() > FILE_SIZE_LIMIT){
                    copyOldAnalytics();
                    outputFile.delete();
                    outputFile.createNewFile();
                }
                outputStream = new FileOutputStream(outputFile, true);
                outputStream.write(output.getBytes());
                outputStream.close();
                mItems.clear();
            } catch (Exception e) {
                // Don't just consume a whole bunch of memory if something is going wrong.
                if (mItems.size() > 100) {
                    mItems.clear();
                }
            }
        }
        else {
            mItems.clear();
        }
    }

    public void addItem(String name, String data) {
        mItems.add(new AnalyticsItem(name, data));
        save();
    }

    public void addItem(String name) {
        mItems.add(new AnalyticsItem(name));
        save();
    }

    public static Analytics get() {
        // Generate one if it doesn't exist yet.
        if (sAnalytics == null) {
            sAnalytics = new Analytics();
        }
        return sAnalytics;
    }

    public static void flush() {
        Analytics.get().flushItems();
    }

    public static void add(String name, String data) {
        Analytics.get().addItem(name, data);
    }

    public static void add(String name) {
        Analytics.get().addItem(name);
    }
}
