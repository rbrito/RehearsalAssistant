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


import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/** The RehearsalPlayback Activity provides playback access for
 * 	annotations in a particular project.
 */
public class RehearsalPlayback extends RehearsalActivity
{
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.playback);
        
        mSessionPlayback = new SessionPlayback(savedInstanceState, this, getIntent().getData());

        if(mSessionPlayback.annotationsCursor().getCount() == 0)
        {
        	TextView instructions = (TextView)findViewById(R.id.no_annotations);
        	instructions.setText(R.string.no_annotations);
        }
    }

    public void onDestroy()
    {
    	mSessionPlayback.onDestroy();
    	super.onDestroy();
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	mSessionPlayback.onRestoreInstanceState(savedInstanceState);
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	mSessionPlayback.onSaveInstanceState(outState);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mSessionPlayback.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	return mSessionPlayback.onOptionsItemSelected(item);
    }
    
	public boolean onContextItemSelected(MenuItem item)
	{
		return mSessionPlayback.onContextItemSelected(item);
	}

    SessionPlayback mSessionPlayback;
}