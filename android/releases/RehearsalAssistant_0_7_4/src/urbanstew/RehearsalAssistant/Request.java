package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class Request
{
	public static void confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		new Dialog(context, title, content, confirmation);
	}
	public static void notification(Context context, String title, String content)
	{
		new Dialog(context, title, content, null);
	}
	public static void contribution(final Context context)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle("Contribute")
    	.setMessage("Thank you for using Reherasal Assistant!  If you are finding it useful, please consider contributing to the project.")
    	.setPositiveButton
    	(
    		"Find out how",
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://urbanstew.org/contribute.html")));
    		    }
    		}
    	)
    	.setNegativeButton
    	(
    		"Not right now",
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    	
    		    }
    		}
    	);
    			
        dialog.show();
		
	}
	public static void recordWidget(final Context context)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle("Sound Recorder Widget")
    	.setMessage("Have you tried the new Sound Recorder Widget?  It allows you to start and stop recording right from your phone's home screen.")
    	.setPositiveButton
    	(
    		"Download it!",
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    				try
    				{
    					context.startActivity
    			        (
    			        	new Intent
    			        	(
    			        		Intent.ACTION_VIEW,
    			        		Uri.parse("market://search?q=pname:org.urbanstew.RehearsalAssistant.RecordWidget")
    			        	).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    			        );
    				} catch (ActivityNotFoundException e)
    				{
        		    	Toast.makeText(context, "Could not start the Market app to download.", Toast.LENGTH_LONG).show();
    				}
    		    }
    		}
    	)
    	.setNegativeButton
    	(
    		"Don't download",
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    }
    		}
    	);
    			
        dialog.show();
	}
}

class Dialog
{
	public Dialog(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle(title)
    	.setMessage(content)
    	.setPositiveButton
    	(
    		"OK",
    		confirmation == null ?
	    		new DialogInterface.OnClickListener()
	    		{
	    		    public void onClick(DialogInterface dialog, int whichButton)
	    		    {
	    		    }
	    		}
    			: confirmation
    	);
    			
        dialog.show();
	}
}
