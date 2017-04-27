/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2008,2009 Stjepan Rajko.
 *
 *  This file is part of the Android version of Rehearsal Assistant.
 *
 *  Rehearsal Assistant is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  Rehearsal Assistant is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Rehearsal Assistant.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package urbanstew.RehearsalAssistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The RehearsalRecord Activity handles recording annotations
 * for a particular project.
 */
public class RehearsalRecord extends RehearsalActivity {

    private Button mRecordButton;
    private IRecordService mRecordService = null;
    private TimerTask mCurrentTimeTask;
    private long mSessionId;
    private TextView mCurrentTime;
    private final SimpleDateFormat mFormatter = new SimpleDateFormat("HH:mm:ss");
    private final Timer mTimer = new Timer();
    private ImageView mLeftRecordIndicator;
    private ImageView mRightRecordIndicator;
    /**
     * Called when the button is pushed
     */
    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                switch (RecordService.State.values()[mRecordService.getState()]) {
                    case READY:
                        startSession();
                        return;
                    case STARTED:
                        startRecording();
                        return;
                    default:
                        stopRecording();
                }
            } catch (RemoteException e) {
            }
        }
    };
    /**
     * Class for interacting with the secondary interface of the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mRecordService = IRecordService.Stub.asInterface(service);
            try {
                mRecordService.setSession(mSessionId);
                mRecordButton.setClickable(true);
                // Find out whether the session is already going
                if (mRecordService.getState() == RecordService.State.STARTED.ordinal()) {
                    ((TextView) findViewById(R.id.record_instructions)).setText(R.string.recording_instructions_started);
                    startedSession();
                }
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mRecordService = null;
            mRecordButton.setClickable(false);
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.record);

        findViewById(R.id.button).setOnClickListener(mClickListener);

        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mLeftRecordIndicator = ((android.widget.ImageView) findViewById(R.id.left_record_indicator));
        mRightRecordIndicator = ((android.widget.ImageView) findViewById(R.id.right_record_indicator));

        mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        RehearsalAssistant.checkSdCard(this);

        mRecordButton = ((Button) findViewById(R.id.button));
        mSessionId = Long.parseLong(getIntent().getData().getPathSegments().get(1));

        // Make Intent explicit see:
        // http://stackoverflow.com/a/42341947
        Intent i = new Intent(IRecordService.class.getName());
        i.setPackage("urbanstew.RehearsalAssistant"); // FIXME: use the proper package
        bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);

        setTitle(this.getString(R.string.recording_session));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(this.getString(R.string.stop_session)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        super.onCreateOptionsMenu(menu); // FIXME: this was commented and I have to check why

        return true;
    }

    public void onResume() {
        super.onResume();

        if (mRecordService != null)
            try {
                mRecordService.setSession(mSessionId);
                updateViews();
            } catch (RemoteException e) {
            }

        mCurrentTimeTask = new TimerTask() {
            public void run() {
                RehearsalRecord.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (mRecordService == null)
                            return;
                        try {
                            if (mRecordService.getState() != RecordService.State.READY.ordinal())
                                mCurrentTime.setText(mFormatter.format(mRecordService.getTimeInSession()));
                        } catch (RemoteException e) {
                        }
                    }
                });
            }
        };
        mTimer.scheduleAtFixedRate(
                mCurrentTimeTask,
                0,
                100);
    }

    public void onPause() {
        mCurrentTimeTask.cancel();
        super.onPause();
    }

    public void onDestroy() {
        mTimer.cancel();
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    /* User interaction events */
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (RecordService.State.values()[mRecordService.getState()]) {
                case RECORDING:
                    stopRecording();
                case STARTED:
                    stopSession();
            }
        } catch (RemoteException e) {
        }
        return true;
    }

    /**
     * State changes.
     */
    private void startSession() {
        try {
            mRecordService.startSession(mSessionId);
            startedSession();

            Intent intent = new Intent("urbanstew.RehearsalAssistant.NetPlugin.startSession");
            sendBroadcast(intent);
        } catch (RemoteException e) {
        }
    }

    private void startedSession() {
        ((android.widget.Button) findViewById(R.id.button)).setText(R.string.record);
        findViewById(R.id.button).setKeepScreenOn(true);
    }

    private void stopSession() {
        try {
            mRecordService.stopSession(mSessionId);

            startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
            finish();

            Intent intent = new Intent("urbanstew.RehearsalAssistant.NetPlugin.stopSession");
            sendBroadcast(intent);
        } catch (RemoteException e) {
        }
    }

    private void startRecording() {
        try {
            mRecordService.startRecording(mSessionId);
            updateViews();
        } catch (RemoteException e) {
        }

    }

    private void stopRecording() {
        try {
            mRecordService.stopRecording();
            updateViews();

        } catch (RemoteException e) {
        }
    }

    private void updateViews() throws RemoteException {
        boolean value = mRecordService.getState() == RecordService.State.RECORDING.ordinal();
        mRecordButton.setText(value ? R.string.stop_recording : R.string.record);
        mLeftRecordIndicator.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
        mRightRecordIndicator.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
    }
}