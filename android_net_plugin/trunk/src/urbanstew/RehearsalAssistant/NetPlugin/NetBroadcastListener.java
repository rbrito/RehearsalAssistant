package urbanstew.RehearsalAssistant.NetPlugin;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetBroadcastListener extends BroadcastReceiver
{    
    public void onReceive(Context context, Intent intent)
    {
		byte[] address = {(byte) 192, (byte) 168, 1, 101};
		OSCPortOut sender;
		try
		{
			sender = new OSCPortOut(java.net.InetAddress.getByAddress(address), 12345);
		} catch (SocketException e1)
		{
			e1.printStackTrace();
			return;
		} catch (UnknownHostException e1)
		{
			e1.printStackTrace();
			return;
		}
		OSCMessage msg;
		
    	if(intent.getAction().equals("urbanstew.RehearsalAssistant.NetPlugin.startSession"))
	        msg = new OSCMessage("/RehearsalAssistant/sessionStarted", null);
    	else if(intent.getAction().equals("urbanstew.RehearsalAssistant.NetPlugin.stopSession"))
		    msg = new OSCMessage("/RehearsalAssistant/sessionStopped", null);
    	else if(intent.getAction().equals("urbanstew.RehearsalAssistant.NetPlugin.playbackStarted"))
    	{
            Object args[] = new Object[2];
            args[0] = new Float(intent.getFloatExtra("START_TIME", 0));
            args[1] = new Float(intent.getFloatExtra("END_TIME", 0));

	        msg = new OSCMessage("/RehearsalAssistant/playbackStarted", args);
    	}
    	else
    		return;

    	try
    	{
            sender.send(msg);
    	} catch (IOException e)
		{
			e.printStackTrace();
		}
    }
}