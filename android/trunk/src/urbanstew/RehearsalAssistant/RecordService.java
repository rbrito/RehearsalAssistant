package urbanstew.RehearsalAssistant;

import java.io.File;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;

public class RecordService extends Service
{
	enum State { INITIALIZING, READY, STARTED, RECORDING };

	public void onCreate()
	{
		mSessionId = -1;
		mState = State.INITIALIZING;
	}

	public void onDestroy()
	{
		if(mState == State.RECORDING)
		{
			stopRecording();
			updateViews();
		}
	}
	public void onStart(Intent intent, int startId)
	{
		if(mState == State.RECORDING)
		{
			stopRecording();
		}
		else
		{
	        long sessionId = SimpleProject.getSessionId(getContentResolver(), new AppDataAccess(this).getRecorderWidgetProjectId());
			startRecording(sessionId);
		}
		updateViews();
	}
	void setSession(long sessionId)
	{
		if(mSessionId != sessionId)
		{
	    	if(mState == State.RECORDING)
	    		stopRecording();
			
			mSessionId = sessionId;
			
	        String[] projection =
	        {
	        	Sessions._ID,
	        	Sessions.START_TIME,
	        	Sessions.END_TIME,
	        	Sessions.TITLE
	        };
	        Log.d("Rehearsal Assistant", "RecordService opening Session ID: " + mSessionId);
	        Cursor cursor = getContentResolver().query(Sessions.CONTENT_URI, projection, Sessions._ID + "=" + mSessionId, null,
	                Sessions.DEFAULT_SORT_ORDER);
	    	cursor.moveToFirst();
	        if(!cursor.isNull(1) && cursor.isNull(2))
	        {
	    		mState = State.STARTED;
	    		mTimeAtStart = cursor.getLong(1);
	        }
	        else
	        	mState = State.READY;
	        mTitle = cursor.getString(3);
	        cursor.close();
		}
	}
	void startSession(long sessionId)
	{
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
	void stopSession(long sessionId)
	{
		ContentValues values = new ContentValues();
    	values.put(Sessions.END_TIME, System.currentTimeMillis());
		getContentResolver().update(ContentUris.withAppendedId(Sessions.CONTENT_URI, sessionId), values, null, null);
		mState = State.READY;
	}
	void toggleRecording(long sessionId)
	{		
		if(mState == State.STARTED)
			startRecording(sessionId);
		else
			stopRecording();
		
		updateViews();
	}
	void startRecording(long sessionId)
	{		
		setSession(sessionId);

		if(mState != State.STARTED)
			return;

		// insert a new Annotation
        ContentValues values = new ContentValues();
    	values.put(Annotations.SESSION_ID, mSessionId);
    	Uri annotationUri = getContentResolver().insert(Annotations.CONTENT_URI, values);
    	mRecordedAnnotationId = Long.parseLong(annotationUri.getPathSegments().get(1));

    	if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
	    	File external = Environment.getExternalStorageDirectory();
			File audio = new File(external.getAbsolutePath() + "/urbanstew.RehearsalAssistant/" + mSessionId); 
			audio.mkdirs();
			Log.w("Rehearsal Assistant", "writing to directory " + audio.getAbsolutePath());
			mOutputFile = audio.getAbsolutePath() + "/audio_" + mRecordedAnnotationId + ".3gp";
			Log.w("Rehearsal Assistant", "writing to file " + mOutputFile);
	    	mRecorder = new MediaRecorder();
	        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	        mRecorder.setOutputFile(mOutputFile);
//	        try
//	        {
	        	mRecorder.prepare();
		        mRecorder.start();   // Recording is now started*/
		        mTimeAtAnnotationStart = System.currentTimeMillis() - mTimeAtStart;
/*	        } catch(IOException e)
	        {
				mOutputFile = null;
	        }*/ 
		}
		else
		{
			mOutputFile = null;
		}
	    mState = State.RECORDING;
	    updateViews();
	}
	void stopRecording()
	{
		if(mState != State.RECORDING)
			return;
		
    	if(mRecorder != null)
    	{
    		mRecorder.stop();
            mRecorder.release();
    	}
        long time = System.currentTimeMillis() - mTimeAtStart;
        
        ContentValues values = new ContentValues();
    	values.put(Annotations.SESSION_ID, mSessionId);
    	values.put(Annotations.START_TIME, mTimeAtAnnotationStart);
    	values.put(Annotations.END_TIME, time);
    	values.put(Annotations.FILE_NAME, mOutputFile);
    	getContentResolver().update(ContentUris.withAppendedId(Annotations.CONTENT_URI, mRecordedAnnotationId), values, null, null);

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
		if(c.getCount()>0)
		{
			c.moveToFirst();

	        ContentValues audioValues = new ContentValues(2);

	        audioValues.put(MediaStore.Audio.AudioColumns.TITLE, "audio_" + mRecordedAnnotationId + ".3gp");
	        audioValues.put(MediaStore.Audio.AudioColumns.ALBUM, mTitle);
	        audioValues.put(MediaStore.Audio.AudioColumns.ARTIST, "Rehearsal Assistant");

	        getContentResolver().update(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0)), audioValues, null, null);
		}
		c.close();

        mState = State.STARTED;
        updateViews();
	}
	
	void updateViews()
	{
		this.sendBroadcast
		(
			new Intent
			(
				mState == State.RECORDING
					?	"urbanstew.RehearsalAssistant.RecordWidget.update_recording"
					:	"urbanstew.RehearsalAssistant.RecordWidget.update"
			)
		);
	}
	
	long timeInRecording()
	{
		if(mState != State.RECORDING)
			return 0;
		return System.currentTimeMillis() - mTimeAtAnnotationStart;
	}
	
	long timeInSession()
	{
		if(mState == State.INITIALIZING)
			return 0;
		return System.currentTimeMillis() - mTimeAtStart;
	}
	
	int getMaxAmplitude()
	{
		if(mRecorder == null || mState != State.RECORDING)
			return 0;
		return mRecorder.getMaxAmplitude();
	}


	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	long mSessionId;
    State mState;
    long mTimeAtStart;
    long mRecordedAnnotationId;
	MediaRecorder mRecorder = null;
    
    long mTimeAtAnnotationStart;
    String mOutputFile;
    String mTitle;

    /**
     * A secondary interface to the service.
     */
    private final IRecordService.Stub mBinder = new IRecordService.Stub() {
		public long getTimeInRecording() throws RemoteException
		{
			return timeInRecording();
		}
		public long getTimeInSession() throws RemoteException
		{
			return timeInSession();
		}
		public void stopRecording() throws RemoteException
		{
			RecordService.this.stopRecording();
		}
		public int getState() throws RemoteException
		{
			return mState.ordinal();
		}
		public void toggleRecording(long sessionId) throws RemoteException
		{
			RecordService.this.toggleRecording(sessionId);
		}
		public int getMaxAmplitude() throws RemoteException
		{
			return RecordService.this.getMaxAmplitude();
		}
		public void setSession(long sessionId) throws RemoteException
		{
			RecordService.this.setSession(sessionId);
		}
		public void startRecording(long sessionId) throws RemoteException
		{
			RecordService.this.startRecording(sessionId);			
		}
		public void startSession(long sessionId) throws RemoteException
		{
			RecordService.this.startSession(sessionId);
		}
		public void stopSession(long sessionId) throws RemoteException
		{
			RecordService.this.stopSession(sessionId);
		}
    };
}
