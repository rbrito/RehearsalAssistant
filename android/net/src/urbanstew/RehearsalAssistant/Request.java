package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

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
