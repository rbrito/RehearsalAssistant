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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
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
        
        // clear the annotations
        data = new RehearsalData(getApplication());
        project_id = data.getProjectID();
        data.clearAnnotations(project_id);
    }
    
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v)
        {
        	if(!going)
        	{
        		mTimeAtStart = SystemClock.elapsedRealtime();
        		going = true;
        		((android.widget.Button)findViewById(R.id.button)).setText("Record");
        		return;
        	}
            if(!recording)
            {
            	recorder = new MediaRecorder();
            	recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	            recorder.setOutputFile("/sdcard/test" + cnt + ".3gpp");
	            recorder.prepare();
	            recorder.start();   // Recording is now started
	            recording = true;
	            ((android.widget.Button)findViewById(R.id.button)).setText("Recording...");
            }
            else
            {
	            recorder.stop();
	            long time = SystemClock.elapsedRealtime() - mTimeAtStart;
	            data.insertAnnotation(project_id, time, "/sdcard/test" + cnt + ".3gpp");
	            recorder.release();
	            ((android.widget.Button)findViewById(R.id.button)).setText("Record");
	            recording = false;
	            cnt++;
            }
        }
    };
    
    RehearsalData data;
    
    MediaRecorder recorder;
    boolean recording = false;
    boolean going = false;
    
    int cnt = 1;
    
    long mTimeAtStart;
    long project_id;
}