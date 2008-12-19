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

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import urbanstew.RehearsalAssistant.Rehearsal.Annotations;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.media.MediaPlayer;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class RehearsalPlayback extends Activity
{
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.playback);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        String[] projection =
        {
        	Annotations._ID,
        	Annotations.START_TIME,
        	Annotations.FILE_NAME        	
        };
        String run_id = getIntent().getData().getPathSegments().get(1);

        cursor = managedQuery(Annotations.CONTENT_URI, projection, Annotations.RUN_ID + "=" + run_id, null,
                Annotations.DEFAULT_SORT_ORDER);
        Log.w("RehearsalAssistant", "Read " + cursor.getCount() + " annotations.");

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getApplication(), R.layout.annotationslist_item, cursor,
                new String[] { Annotations.START_TIME }, new int[] { android.R.id.text1 });
        adapter.setCursorToStringConverter(new CursorToStringConverter()
        {
			public CharSequence convertToString(Cursor cursor)
			{
				return formatter.format(cursor.getString(0));
			}	
        });
        adapter.setViewBinder(new ViewBinder()
        {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex)
			{
				TextView v = (TextView)view;
				v.setText(formatter.format(new Date(cursor.getInt(columnIndex))));
				return true;
			}
        	
        });
        ListView list = (ListView)findViewById(R.id.annotation_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(mSelectedListener);
        
        
    }
    
    /** Called when the user selects a list item. */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
			String state = android.os.Environment.getExternalStorageState();
	    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED)
	    			&& !state.equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY))
	    	{
	        	Request.notification(RehearsalPlayback.this,
	            		"Media Missing",
	            		"Your external media (e.g., sdcard) is not mounted (it is " + state + ").  Rehearsal Assistant cannot access the saved file."
	            	);
	        	return;
	    	}
	    	
			cursor.moveToPosition(position);

        	if(player != null)
        	{
        		player.stop();
        		player.release();
        	}
            try
            {
            	player = new MediaPlayer();
            	player.setDataSource(cursor.getString(2));
            	player.prepare();
            	player.start();
            }
            catch(java.io.IOException e)
            {
            }
        }
    };

    Cursor cursor;
    MediaPlayer player = null;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

}