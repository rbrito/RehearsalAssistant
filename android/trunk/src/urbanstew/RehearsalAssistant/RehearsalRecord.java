/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2008 Stjepan Rajko.
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.media.MediaRecorder;
import android.os.SystemClock;

/** The RehearsalRecord Activity handles recording annotations
 * 	for a particular project.
 */
public class RehearsalRecord extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.record);
        findViewById(R.id.button).setOnClickListener(mClickListener);
        run_id = getIntent().getData().getPathSegments().get(1);
        
        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED))
    	{
        	Request.notification(this,
            		"Media Missing",
            		"Your external media (e.g., sdcard) is not mounted (it is " + state + ").  Rehearsal Assistant will not function properly, as it uses external storage for the recorded audio annotation files."
            	);
    	}
    }
    
    public void onPause()
    {
    	super.onPause();
    	mCurrentTimeTask.cancel();
    }
    
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v)
        {
        	if(!going)
        	{
        		// clear the annotations
        		getContentResolver().delete(Annotations.CONTENT_URI, "run_id =" + run_id, null);

        		// grab start time, change UI
        		mTimeAtStart = SystemClock.elapsedRealtime();
        		((android.widget.Button)findViewById(R.id.button)).setText("Record");
        		
        		mTimer.scheduleAtFixedRate(
        				mCurrentTimeTask,
        				1000,
        				1000);

        		going = true;
        		return;
        	}
            if(!recording)
            {
            	if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            	{
            		File external = Environment.getExternalStorageDirectory();
            		File audio = new File(external.getAbsolutePath() + "/rehearsal/" + run_id); 
            		audio.mkdirs();
            		Log.w("Rehearsal Assistant", "writing to directory " + audio.getAbsolutePath());
            		output_file = audio.getAbsolutePath() + "/audio" + cnt + ".3gpp";
	            	recorder = new MediaRecorder();
	            	recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		            recorder.setOutputFile(output_file);
		            recorder.prepare();
		            recorder.start();   // Recording is now started*/
            	}
            	else
            	{
            		output_file = null;
            	}
	            recording = true;
	            ((android.widget.Button)findViewById(R.id.button)).setText("Recording...");
            }
            else
            {
            	if(recorder != null)
            	{
            		recorder.stop();
    	            recorder.release();
            	}
	            long time = SystemClock.elapsedRealtime() - mTimeAtStart;
	            
	            ContentValues values = new ContentValues();
	        	values.put(Annotations.RUN_ID, run_id);
	        	values.put(Annotations.START_TIME, time);
	        	values.put(Annotations.FILE_NAME, output_file);
	        	getContentResolver().insert(Annotations.CONTENT_URI, values);

	        	((android.widget.Button)findViewById(R.id.button)).setText("Record");
	            recording = false;
	            cnt++;
            }
        }
    };
    
    TextView mCurrentTime;
    SimpleDateFormat mFormatter = new SimpleDateFormat("HH:mm:ss");
    TimerTask mCurrentTimeTask = new TimerTask()
	{
		public void run()
		{
			RehearsalRecord.this.runOnUiThread(new Runnable()
			{
				public void run()
				{
					mCurrentTime.setText(mFormatter.format(SystemClock.elapsedRealtime() - mTimeAtStart));
				}
			});                                
		}
	};
	
    RehearsalData data;
    
    MediaRecorder recorder = null;
    boolean recording = false;
    boolean going = false;
    
    int cnt = 1;
    
    long mTimeAtStart;
    long project_id;
    
    String run_id;
    String output_file;
    Timer mTimer = new Timer();
}