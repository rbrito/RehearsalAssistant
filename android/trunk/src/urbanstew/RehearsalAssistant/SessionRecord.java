package urbanstew.RehearsalAssistant;

import java.io.File;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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
        	Sessions.END_TIME,
        	Sessions.TITLE
        };
        Log.d("Rehearsal Assistant", "SessionRecord opening Session URI: " + uri + " ID: " + mSessionId);
        Cursor cursor = resolver.query(Sessions.CONTENT_URI, projection, Sessions._ID + "=" + mSessionId, null,
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
		
	    mState = State.STARTED;
	}
	
	public void stopSession()
	{
		ContentValues values = new ContentValues();
    	values.put(Sessions.END_TIME, System.currentTimeMillis());
		mContentResolver.update(mUri, values, null, null);
	}
	
	public void startRecording()
	{
		// insert a new Annotation
        ContentValues values = new ContentValues();
    	values.put(Annotations.SESSION_ID, mSessionId);
    	Uri annotationUri = mContentResolver.insert(Annotations.CONTENT_URI, values);
    	mRecordedAnnotationId = Long.parseLong(annotationUri.getPathSegments().get(1));

    	if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
	    	File external = Environment.getExternalStorageDirectory();
			File audio = new File(external.getAbsolutePath() + "/urbanstew.RehearsalAssistant/" + mSessionId); 
			audio.mkdirs();
			Log.w("Rehearsal Assistant", "writing to directory " + audio.getAbsolutePath());
			mOutputFile = audio.getAbsolutePath() + "/audio_" + mRecordedAnnotationId + ".3gp";
			Log.w("Rehearsal Assistant", "writing to file " + mOutputFile);
	    	recorder = new MediaRecorder();
	        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	        recorder.setOutputFile(mOutputFile);
//	        try
//	        {
	        	recorder.prepare();
		        recorder.start();   // Recording is now started*/
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
    	mContentResolver.update(ContentUris.withAppendedId(Annotations.CONTENT_URI, mRecordedAnnotationId), values, null, null);

        String[] mediaProjection =
        {
            MediaStore.MediaColumns._ID,
        };
        		
		Cursor c =
			mContentResolver.query
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

	        mContentResolver.update(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0)), audioValues, null, null);
		}
		c.close();

        mState = State.STARTED;
	}
	
	public State state()
	{	return mState; }
	
	public long timeAtStart()
	{	return mTimeAtStart; }
	
	public String getSessionTitle()
	{
		return mTitle;
	}
	long timeInRecording()
	{
		if(recorder == null)
			return 0;
		return System.currentTimeMillis() - mTimeAtAnnotationStart;
	}
	
	MediaRecorder recorder = null;
    State mState = State.INITIALIZING;
        
    long mSessionId;
    long mTimeAtStart;
    long mTimeAtAnnotationStart;
    long mRecordedAnnotationId;
    
    ContentResolver mContentResolver;
    Uri mUri;
    
    String mOutputFile;
    String mTitle;
}
