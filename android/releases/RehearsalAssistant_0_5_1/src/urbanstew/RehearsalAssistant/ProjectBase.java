package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class ProjectBase extends RehearsalActivity
{	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      	
        ((Button)findViewById(R.id.switch_mode)).setOnClickListener(switchClickListener);
      	
        String projectId = getIntent().getData().getPathSegments().get(1);
        mProjectId = Long.valueOf(projectId);
        
        setTitle(getResources().getString(R.string.about));
        
        mAppData = new AppDataAccess(getContentResolver());

        // Display license if this is the first time running this version.
        mVisitedVersion = mAppData.getVisitedVersion();
        if (mVisitedVersion == null || !mVisitedVersion.equals("0.5"))
        {
    		Request.notification
    		(
				this,
				"Warning",
				getString(R.string.beta_warning)
			);
    		Request.confirmation
    		(
				this,
				"License",
				getString(R.string.license),
				new DialogInterface.OnClickListener()
	    		{
	    		    public void onClick(DialogInterface dialog, int whichButton)
	    		    {
	    		    	if(mVisitedVersion == null)
	    		    		mAppData.addVisitedVersion("0.5");
	    		    	else
	    		    		mAppData.setVisitedVersion("0.5");
	    		    }
	    		}
	    	);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        mHelpMenuItem = menu.add(R.string.help).setIcon(android.R.drawable.ic_dialog_info);
        return true;
    }
    
    View.OnClickListener switchClickListener = new View.OnClickListener()
    {
		public void onClick(View v)
		{
			if(v == findViewById(R.id.switch_mode))
			{
				long project_id = new AppDataAccess(getContentResolver()).switchCurrentProject();
		        
				startActivity
		        (
		        	new Intent
		        	(
		        		Intent.ACTION_VIEW,
		        		ContentUris.withAppendedId(Projects.CONTENT_URI, project_id)
		        	)
		        );
		        
		        finish();
			}
		}
    };
    
    protected long projectId()
    {
    	return mProjectId;
    }
    
    long mProjectId;
    
    protected MenuItem mHelpMenuItem; 
    
    protected static final int SESSIONS_ID = 0;
    protected static final int SESSIONS_TITLE = 1;
    
    protected static final String[] sessionsProjection = new String[]
	{
	      Sessions._ID, // 0
	      Sessions.TITLE // 1
	};
    
    String mVisitedVersion;
    AppDataAccess mAppData;
}
