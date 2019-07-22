package com.hfad.stopwatch;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

// SOS: Pressing Back calls activity's onDestroy, but the process remains alive until Android is forced
// to kill it. So does the Activity obj until it's gc'ed. Whereas, swiping the app off Recents kills
// the process instantly.
public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String TENTHS_OF_SECOND_KEY = "mTenthsOfSecond";
    private static final String WAS_RUNNING_KEY = "mWasRunning";

    private int mTenthsOfSecond = 0;
    private boolean mRunning = false;
    private boolean mWasRunning = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mTenthsOfSecond = savedInstanceState.getInt(TENTHS_OF_SECOND_KEY);
            mWasRunning = savedInstanceState.getBoolean(WAS_RUNNING_KEY);
        }

        runTimer();
    }

    // SOS: starting w Android Pie, this is called after onStop! (the book's code is wrong because
    // it assumes that this runs before onPause -- I fixed it)
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(TENTHS_OF_SECOND_KEY, mTenthsOfSecond);
        savedInstanceState.putBoolean(WAS_RUNNING_KEY, mWasRunning);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWasRunning) {
            mRunning = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRunning = false;
    }

    // SOS: Suppose I DON'T call removeCallbacksAndMessages and I press Back. Runnables keep being
    // posted to the main/UI thread which is alive. These Runnables are updating a "stale" view, that
    // of the now "destroyed" but still in-memory Activity obj. The obj can't be gc'ed because the
    // Runnable has a ref to the timeView which has a ref to the Activity obj (what I get w view.getContext).
    // Now, if I restart the app, I get a NEW Activity obj (see log stmt in runTimer for proof!), which
    // creates a new handler that posts new Runnables to the UI thread. IOW, I now have 2 different
    // series of Runnables being posted, one updating the stale view, the other the now visible view!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void runTimer() {
        final TextView timeView = findViewById(R.id.time_view);

        // SOS: This is not an accurate timer, google "handler time not accurate android"
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = mTenthsOfSecond / 36000;
                int minutes = (mTenthsOfSecond % 36000) / 600;
                int secs = (mTenthsOfSecond % 600) / 10;
                int fraction = mTenthsOfSecond % 10;
                String time = String.format(Locale.getDefault(),
                        "%d:%02d:%02d.%d", hours, minutes, secs, fraction);
                timeView.setText(time);
                Log.i(LOG_TAG, "Runnable accessing " + timeView.getContext());
                if (mRunning) {
                    mTenthsOfSecond++;
                }
                handler.postDelayed(this, 100);
            }
        });
    }

    public void onClickStart(View view) {
        mRunning = true;
        mWasRunning = true;
    }

    public void onClickStop(View view) {
        mRunning = false;
        mWasRunning = false;
    }

    public void onClickReset(View view) {
        mRunning = false;
        mWasRunning = false;
        mTenthsOfSecond = 0;
    }
}
