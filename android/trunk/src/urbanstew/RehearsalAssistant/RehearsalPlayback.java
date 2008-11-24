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

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;


import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
        
        // setup the playback list
        setContentView(R.layout.playback);
        ListView list = (ListView)findViewById(R.id.list);
        listAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings);
        list.setAdapter(listAdapter);
        list.setTextFilterEnabled(true);
        list.setOnItemClickListener(mSelectedListener);
        
        // read in the data and display a list of times
        RehearsalData data = new RehearsalData(getApplication());
        Cursor c = data.getAnnotations(data.getProjectID());
        if (c.getCount()>0)
	        for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
	        	listAdapter.add(formatter.format(c.getLong(2)));
    }
    
    /** Called when the user selects a list item. */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3)
        {   
        	if(player != null)
        	{
        		player.stop();
        		player.release();
        	}
            try
            {
            	player = new MediaPlayer();
            	player.setDataSource("/sdcard/test" + (arg2 + 1) + ".3gpp");
            	player.prepare();
            	player.start();
            }
            catch(java.io.IOException e)
            {
            }
        }
    };

    MediaPlayer player = null;
    List<String> mStrings = new LinkedList<String>();
    ArrayAdapter<String> listAdapter;
    
    SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");

}