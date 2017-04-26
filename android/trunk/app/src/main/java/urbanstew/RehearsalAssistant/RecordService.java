package urbanstew.RehearsalAssistant;

import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;


public class RecordService extends Service {
    private final static int[] sampleRates = {44100, 22050, 11025, 8000};
    private static final String TAG = "Rehearsal Assistant";
    private long mSessionId;
    private State mState;
    private long mTimeAtStart;
    private long mRecordedAnnotationId;
    private RehearsalAudioRecorder mRecorder = null;
    private long mTimeAtAnnotationStart;
    private String mOutputFile;
    private String mTitle;
    private PowerManager.WakeLock mWakeLock;
    /**
     * A secondary interface to the service.
     */
    private final IRecordService.Stub mBinder = new IRecordService.Stub() {
        public long getTimeInRecording() throws RemoteException {
            return timeInRecording();
        }

        public long getTimeInSession() throws RemoteException {
            return timeInSession();
        }

        public void stopRecording() throws RemoteException {
            RecordService.this.stopRecording();
        }

        public int getState() throws RemoteException {
            return mState.ordinal();
        }

        public void toggleRecording(long sessionId) throws RemoteException {
            RecordService.this.toggleRecording(sessionId);
        }

        public int getMaxAmplitude() throws RemoteException {
            return RecordService.this.getMaxAmplitude();
        }

        public void setSession(long sessionId) throws RemoteException {
            RecordService.this.setSession(sessionId);
        }

        public void startRecording(long sessionId) throws RemoteException {
            RecordService.this.startRecording(sessionId);
        }

        public void startSession(long sessionId) throws RemoteException {
            RecordService.this.startSession(sessionId);
        }

        public void stopSession(long sessionId) throws RemoteException {
            RecordService.this.stopSession(sessionId);
        }
    };

    public void onCreate() {
        mSessionId = -1;
        mState = State.INITIALIZING;

        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RecordService.class.getName());

    }

    public void onDestroy() {
        if (mState == State.RECORDING) {
            stopRecording();
            updateViews();
        }
        if (mWakeLock.isHeld())
            mWakeLock.release();
    }

    /**
     * Starting the activity will toggle the recording state.
     * When this starts recording, it will always start recording
     * in the project designated for Recorder Widget recordings
     * (which is guaranteed to exist / will be created when needed,
     * and has a single session that is STARTED).
     */
    public void onStart(Intent intent, int startId) {
        if (mState == State.RECORDING) {
            stopRecording();
        } else {
            long sessionId = SimpleProject.getSessionId(getContentResolver(), new AppDataAccess(this).getRecorderWidgetProjectId());
            startRecording(sessionId);
        }
        updateViews();
    }

    /**
     * Selects the active session.
     *
     * @param sessionId id of the session to select.
     */
    private void setSession(long sessionId) {
        if (mSessionId != sessionId) {
            if (mState == State.RECORDING)
                stopRecording();

            Log.d(TAG, "RecordService opening Session ID: " + mSessionId);

            mSessionId = sessionId;

            String[] projection =
                    {
                            Sessions._ID,
                            Sessions.START_TIME,
                            Sessions.END_TIME,
                            Sessions.TITLE
                    };

            // determine whether the session is already started
            Cursor cursor = getContentResolver().query(Sessions.CONTENT_URI, projection, Sessions._ID + "=" + mSessionId, null,
                    Sessions.DEFAULT_SORT_ORDER);
            cursor.moveToFirst();
            if (!cursor.isNull(1) && cursor.isNull(2)) {
                mState = State.STARTED;
                mTimeAtStart = cursor.getLong(1);
            } else
                mState = State.READY;
            mTitle = cursor.getString(3);
            cursor.close();
        }
    }

    /**
     * Starts the designated session.  This will erase all existing recordings in
     * the session, set its end time to null, and update the session's start
     * time with the current time.
     * <p>
     * Changes the RecordService state to STARTED.
     *
     * @param sessionId id of the session to start.
     */
    private void startSession(long sessionId) {
        setSession(sessionId);

        // clear the annotations
        getContentResolver().delete(Annotations.CONTENT_URI, Annotations.SESSION_ID + "=" + mSessionId, null);

        // grab start time, change UI
        mTimeAtStart = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(Annotations.START_TIME, mTimeAtStart);
        values.putNull(Annotations.END_TIME);
        getContentResolver().update(ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId), values, null, null);

        mState = State.STARTED;
    }

    /**
     * Stops the designated session.  This will set its end time to
     * the current time.
     * <p>
     * Changes the RecordService state to READY.
     *
     * @param sessionId id of the session to start.
     */
    private void stopSession(long sessionId) {
        ContentValues values = new ContentValues();
        values.put(Sessions.END_TIME, System.currentTimeMillis());
        getContentResolver().update(ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId), values, null, null);
        mState = State.READY;
    }

    /**
     * Toggles recording.
     * <p>
     * Calls startRecording or stopRecording depending on the current state.
     *
     * @param sessionId id of the session for the recording (used only when starting a recording)
     * @see this.startRecording
     * @see this.stopRecording
     */
    private void toggleRecording(long sessionId) {
        if (mState == State.STARTED)
            startRecording(sessionId);
        else
            stopRecording();

        updateViews();
    }

    /**
     * Starts recording.
     * <p>
     * Changes state to RECORDING.
     *
     * @param sessionId id of the session for the recording
     */
    private void startRecording(long sessionId) {
        setSession(sessionId);

        // session must be in STARTED state
        if (mState != State.STARTED)
            return;

        // insert a new Annotation into the Session
        ContentValues values = new ContentValues();
        values.put(Annotations.SESSION_ID, mSessionId);
        Uri annotationUri = getContentResolver().insert(Annotations.CONTENT_URI, values);
        mRecordedAnnotationId = Long.parseLong(annotationUri.getPathSegments().get(1));

        // make sure the SD card is present for the recording
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            // create the directory
            File external = Environment.getExternalStorageDirectory();
            File audio = new File(external.getAbsolutePath() + "/urbanstew.RehearsalAssistant/" + mSessionId);
            audio.mkdirs();
            Log.w(TAG, "writing to directory " + audio.getAbsolutePath());


            // get the recording type from preferences
            boolean uncompressed = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("uncompressed_recording", false);

            // construct file name
            mOutputFile =
                    audio.getAbsolutePath() + "/audio_" + mRecordedAnnotationId
                            + (uncompressed ? ".wav" : ".m4a");
            Log.w(TAG, "writing to file " + mOutputFile);

            // start the recording
            if (!uncompressed) {
                mRecorder = new RehearsalAudioRecorder(false, 0, 0, 0, 0);
            } else {
                int i = 0;
                do {
                    if (mRecorder != null)
                        mRecorder.release();
                    mRecorder = new RehearsalAudioRecorder(true, AudioSource.MIC, sampleRates[i], AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                }
                while ((++i < sampleRates.length) & !(mRecorder.getState() == RehearsalAudioRecorder.State.INITIALIZING));
            }
            mRecorder.setOutputFile(mOutputFile);
            mRecorder.prepare();
            mRecorder.start(); // Recording is now started
            mTimeAtAnnotationStart = System.currentTimeMillis() - mTimeAtStart;
            if (mRecorder.getState() == RehearsalAudioRecorder.State.ERROR) {
                mOutputFile = null;
            }
        } else {
            mOutputFile = null;
            mTimeAtAnnotationStart = System.currentTimeMillis() - mTimeAtStart;
        }
        mState = State.RECORDING;
        mWakeLock.acquire();
        updateViews();
    }

    /**
     * Stops recording.
     * <p>
     * Changes state to STARTED.
     */
    private void stopRecording() {
        // state must be RECORDING
        if (mState != State.RECORDING)
            return;

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
        }

        // complete the Annotation entry in the database
        long time = System.currentTimeMillis() - mTimeAtStart;

        ContentValues values = new ContentValues();
        values.put(Annotations.SESSION_ID, mSessionId);
        values.put(Annotations.START_TIME, mTimeAtAnnotationStart);
        values.put(Annotations.END_TIME, time);
        values.put(Annotations.FILE_NAME, mOutputFile);
        getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI, mRecordedAnnotationId), values, null, null);

        // Add some information to the MediaStore
        String[] mediaProjection =
                {
                        MediaStore.MediaColumns._ID,
                };

        Cursor c =
                getContentResolver().query
                        (
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                mediaProjection,
                                MediaStore.MediaColumns.DATA + "='" + mOutputFile + "'",
                                null,
                                null
                        );
        if (c.getCount() > 0) {
            c.moveToFirst();

            ContentValues audioValues = new ContentValues(2);

            audioValues.put(MediaStore.Audio.AudioColumns.TITLE, "audio_" + mRecordedAnnotationId + ".m4a");
            audioValues.put(MediaStore.Audio.AudioColumns.ALBUM, mTitle);
            audioValues.put(MediaStore.Audio.AudioColumns.ARTIST, "Rehearsal Assistant");

            getContentResolver().update(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0)), audioValues, null, null);
        }
        c.close();

        mState = State.STARTED;
        updateViews();

        if (mWakeLock.isHeld())
            mWakeLock.release();
    }

    private void updateViews() {
        this.sendBroadcast
                (
                        new Intent
                                (
                                        mState == State.RECORDING
                                                ? "urbanstew.RehearsalAssistant.RecordWidget.update_recording"
                                                : "urbanstew.RehearsalAssistant.RecordWidget.update"
                                )
                );
    }

    private long timeInRecording() {
        if (mState != State.RECORDING)
            return 0;
        return System.currentTimeMillis() - mTimeAtAnnotationStart;
    }

    private long timeInSession() {
        if (mState == State.INITIALIZING)
            return 0;
        return System.currentTimeMillis() - mTimeAtStart;
    }

    private int getMaxAmplitude() {
        if (mRecorder == null || mState != State.RECORDING)
            return 0;
        return mRecorder.getMaxAmplitude();
    }

    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    /**
     * INITIALIZING : service is initializing; no session selected
     * READY : session has been selected, session is not started
     * STARTED : session has been started, not currently recording
     * RECORDING : recording
     */
    enum State {
        INITIALIZING, READY, STARTED, RECORDING
    }
}
