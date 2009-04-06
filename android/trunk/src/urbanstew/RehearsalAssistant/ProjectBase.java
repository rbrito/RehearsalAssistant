package urbanstew.RehearsalAssistant;

import urbanstew.RehearsalAssistant.Rehearsal.Projects;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class ProjectBase extends Activity
{
	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      	((Button)findViewById(R.id.switch_mode)).setOnClickListener(switchClickListener);
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
      	menu.add(R.string.help).setIcon(android.R.drawable.ic_dialog_info);
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
}
