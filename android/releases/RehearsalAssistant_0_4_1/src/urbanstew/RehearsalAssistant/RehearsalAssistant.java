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

import urbanstew.RehearsalAssistant.Rehearsal.AppData;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/** The RehearsalAssistant Activity is the top-level activity.
 */
public class RehearsalAssistant extends Activity implements View.OnClickListener
{
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_RECORD = Menu.FIRST + 1;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 2;

    private static final String[] PROJECTION = new String[]
    {
        "_id", // 0
        "title" // 1
    };
    
    /** Called when the activity is first created.
     *  
     *  For now, provides access to the recording and playback activities.
     *  
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        // Set intent to sessions
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Sessions.CONTENT_URI);
        }
        
        // Read sessions
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
                Sessions.DEFAULT_SORT_ORDER);
        
        Log.w("RehearsalAssistant", "Read " + cursor.getCount() + Sessions.TABLE_NAME);
        
        // Map Sessions to ListView
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.runslist_item, cursor,
                new String[] { "title" }, new int[] { android.R.id.text1 });
        ListView list = (ListView)findViewById(R.id.run_list);
        list.setAdapter(adapter);
        
        // Setup list and the click listener
        ((Button)findViewById(R.id.new_run)).setOnClickListener(this);
        ((Button)findViewById(R.id.help)).setOnClickListener(this);
        list.setOnCreateContextMenuListener(mCreateContextMenuListener);	
        list.setOnItemClickListener(mSelectedListener);
        
        // Display license if this is the first time running this version.
        String[] appDataProjection =
        {
        	AppData._ID,
            AppData.KEY,
        	AppData.VALUE
        };
        Cursor appDataCursor = managedQuery(AppData.CONTENT_URI, appDataProjection, AppData.KEY + "=" + "'app_visited_version'", null, AppData.DEFAULT_SORT_ORDER);
        if(appDataCursor.getCount()>0)
        	appDataCursor.moveToFirst();
        if(appDataCursor.getCount()==0 || !appDataCursor.getString(2).equals("0.3"))
        {
    		Request.notification(this,
    				"Warning",
    				getString(R.string.beta_warning));
    		Request.notification(this,
    				"License",
    				getString(R.string.license));
        }
    	if(appDataCursor.getCount()==0)
    	{
    		ContentValues values = new ContentValues();
        	values.put(AppData.KEY, "app_visited_version");
        	values.put(AppData.VALUE, "0.3");
        	getContentResolver().insert(AppData.CONTENT_URI, values);
    	}
    }

    /** Called when the user selects an item in the list.
     *  
     *  Currently, starts the RehearsalPlayback activity.
     *  
     */
    AdapterView.OnItemClickListener mSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
        {
	    	Uri runUri = ContentUris.withAppendedId(getIntent().getData(), id);
	    	startActivity(new Intent(Intent.ACTION_VIEW, runUri));
        }
    };
    
    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
			menu.add(0, MENU_ITEM_PLAYBACK, 0, "playback");
			menu.add(0, MENU_ITEM_RECORD, 1, "record");
			menu.add(0, MENU_ITEM_DELETE, 1, "delete");
		}
    	
    };
    
	public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("Rehearsal Assistant", "bad menuInfo", e);
            return false;
        }

    	Uri runUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

    	switch (item.getItemId()) {

            case MENU_ITEM_PLAYBACK: {
                // Delete the run that the context menu is for
            	startActivity(new Intent(Intent.ACTION_VIEW, runUri));
                return true;
            }
            case MENU_ITEM_RECORD: {
        		startActivity(new Intent(Intent.ACTION_EDIT, runUri));
                return true;
            }
            case MENU_ITEM_DELETE: {
                // Delete the run that the context menu is for
                getContentResolver().delete(runUri, null, null);
                return true;
            }
        }
        return false;
	}

	public void onClick(View v)
	{
		if(v == findViewById(R.id.new_run))
			startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
		else if(v == findViewById(R.id.help))
			Request.notification(this, "Instructions", getResources().getString(R.string.instructions));
	}    
}