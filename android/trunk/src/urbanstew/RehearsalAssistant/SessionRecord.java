package urbanstew.RehearsalAssistant;

import java.io.File;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class SessionRecord
{
	enum State { INITIALIZING, READY, STARTED, RECORDING };

	SessionRecord(Uri uri, ContentResolver resolver)
	{
		mUri = uri;
		mSessionId = Long.parseLong(mUri.getPathSegments().get(1));
		mContentResolver = resolver;
		
        String[] projection =
        {
        	Sessions._ID,
        	Sessions.START_TIME,
        	Sessions.END_TIME        	
        };
        Log.d("Rehearsal Assistant", "SessionRecord opening Session URI: " + uri + " ID: " + mSessionId);
        Cursor cursor = resolver.query(Sessions.CONTENT_URI, projection, Sessions._ID + "=" + mSessionId, null,
                Sessions.DEFAULT_SORT_ORDER);
    	cursor.moveToFirst();
        if(!cursor.isNull(1) && cursor.isNull(2))
        {
    		mState = State.STARTED;

    		mTimeAtStart = cursor.getLong(1);
            String[] annotation_projection =
            {
            	Annotations._ID        	
            };
        	Cursor annotationCursor = resolver.query(Annotations.CONTENT_URI, annotation_projection, Annotations.SESSION_ID + "=" + mSessionId, null,
                    Annotations.DEFAULT_SORT_ORDER);
        	cnt = annotationCursor.getCount() + 1;
        }
        else
        	mState = State.READY;
        cursor.close();
	}
	
	public void onDestroy()
	{
    	if(mState == State.RECORDING)
    		stopRecording();
	}
	
	public void startSession()
	{
		// clear the annotations
		mContentResolver.delete(Annotations.CONTENT_URI, Annotations.SESSION_ID + "=" + mSessionId, null);

		// grab start time, change UI
		mTimeAtStart = System.currentTimeMillis();

		ContentValues values = new ContentValues();
    	values.put(Annotations.START_TIME, mTimeAtStart);
    	values.putNull(Annotations.END_TIME);
		mContentResolver.update(mUri, values, null, null);
	}
	
	public void stopSession()
	{
		ContentValues values = new ContentValues();
    	values.put(Annotations.END_TIME, System.currentTimeMillis());
		mContentResolver.update(mUri, values, null, null);
	}
	
	public void startRecording()
	{
		if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			File external = Environment.getExternalStorageDirectory();
			File audio = new File(external.getAbsolutePath() + "/rehearsal/" + mSessionId); 
			audio.mkdirs();
			Log.w("Rehearsal Assistant", "writing to directory " + audio.getAbsolutePath());
			mOutputFile = audio.getAbsolutePath() + "/audio" + cnt + ".3gp";
			Log.w("Rehearsal Assistant", "writing to file " + mOutputFile);
	    	recorder = new MediaRecorder();
	    	recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	        recorder.setOutputFile(mOutputFile);
	        recorder.prepare();
	        recorder.start();   // Recording is now started*/
	        mTimeAtAnnotationStart = System.currentTimeMillis() - mTimeAtStart;
		}
		else
		{
			mOutputFile = null;
		}
	    mState = State.RECORDING;
	}
	
	public void stopRecording()
	{
    	if(recorder != null)
    	{
    		recorder.stop();
            recorder.release();
    	}
        long time = System.currentTimeMillis() - mTimeAtStart;
        
        ContentValues values = new ContentValues();
    	values.put(Annotations.SESSION_ID, mSessionId);
    	values.put(Annotations.START_TIME, mTimeAtAnnotationStart);
    	values.put(Annotations.END_TIME, time);
    	values.put(Annotations.FILE_NAME, mOutputFile);
    	mContentResolver.insert(Annotations.CONTENT_URI, values);

        mState = State.STARTED;
        cnt++;
	}
	
	public State state()
	{	return mState; }
	
	public long timeAtStart()
	{	return mTimeAtStart; }
	
	long timeInRecording()
	{
		if(recorder == null)
			return 0;
		return System.currentTimeMillis() - mTimeAtAnnotationStart;
	}
	
	MediaRecorder recorder = null;
    State mState = State.INITIALIZING;
    
    int cnt = 1;
    
    long mSessionId;
    long mTimeAtStart;
    long mTimeAtAnnotationStart;
    
    ContentResolver mContentResolver;
    Uri mUri;
    
    String mOutputFile;
}
