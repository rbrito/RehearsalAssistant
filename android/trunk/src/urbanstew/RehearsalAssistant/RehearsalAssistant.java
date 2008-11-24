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

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/** The RehearsalAssistant Activity is the top-level activity.
 */
public class RehearsalAssistant extends Activity
{
    /** Called when the activity is first created.
     *  
     *  For now, provides access to the recording and playback activities.
     *  
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        // Setup list and the click listener
        ListView list = (ListView)findViewById(R.id.project_list);
        listAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings);
        list.setAdapter(listAdapter);
        list.setTextFilterEnabled(true);
        list.setOnItemClickListener(mSelectedListener);
         
        // Add record and playback items to the list
        listAdapter.add("Record");
        listAdapter.add("Playback");
    }

    
    /** Called when the user selects an item in the list.
     *  
     *  Currently, starts RehearsalRecord or RehearsalPlayback activities.
     *  
     */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3)
        {
			if(arg2==0)
			{
				startActivity(new Intent(getApplication(), RehearsalRecord.class));
			}
			if(arg2==1)
			{
				startActivity(new Intent(getApplication(), RehearsalPlayback.class));
			}
        }
    };
        
    private List<String> mStrings = new LinkedList<String>();
    private ArrayAdapter<String> listAdapter;    
}