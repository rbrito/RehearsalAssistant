package urbanstew.RehearsalAssistant;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class RehearsalAudioRecorder
{
	/**
	 * INITIALIZING : recorder is initializing;
	 * READY : recorder has been initialized, recorder not yet started
	 * RECORDING : recording
	 * ERROR : reconstruction needed
	 * STOPPED: reset needed
	 */
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED}
	enum ByteOrder { LSB_FIRST, MSB_FIRST };
	
	private final int PERIOD_IN_FRAMES = 1000;
	
	private boolean 		 rUncompressed;
	private AudioRecord 	 aRecorder = null;
	private MediaRecorder	 mRecorder = null;
	private int				 cAmplitude= 0;
	private String			 fPath = null;
	private State			 state;
	private RandomAccessFile fWriter;
	private short 			 nChannels;
	private int				 sRate;
	private short			 bSamples;
	private int				 bufferSize;
	private int				 aSource;
	private int				 aFormat;
	
	private byte[] 			 buffer;
	
	private int				 payloadSize;
	
	private Timer			 timer;
	
	public State getState()
	{
		return state;
	}
	
	private class RecordTask extends TimerTask
	{
		public void run()
		{
			aRecorder.read(buffer, 0, buffer.length);
			try
			{
				fWriter.write(buffer);
				payloadSize += buffer.length;
				if (bSamples == 16)
				{
					for (int i=0; i<buffer.length/2; i++)
					{ // 16bit sample size
						short curSample = getShort(buffer[i*2], buffer[i*2+1]);
						if (curSample > cAmplitude)
						{
							cAmplitude = curSample;
						}
					}
				}
				else
				{ // 8bit sample size
					for (int i=0; i<buffer.length; i++)
					{
						if (buffer[i] > cAmplitude)
						{
							cAmplitude = buffer[i];
						}
					}
				}
			}
			catch (IOException e)
			{
				stop();
			}
		}
	}
	
	public RehearsalAudioRecorder(boolean uncompressed, int audioSource, int sampleRate, int channelConfig,
			int audioFormat)
	{
		try
		{
			rUncompressed = uncompressed;
			if (rUncompressed)
			{
				if (audioFormat == AudioFormat.ENCODING_PCM_16BIT)
				{
					bSamples = 16;
				}
				else
				{
					bSamples = 8;
				}
				
				if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO)
				{
					nChannels = 1;
				}
				else
				{
					nChannels = 2;
				}
				
				aSource = audioSource;
				sRate   = sampleRate;
				aFormat = audioFormat;

				bufferSize = PERIOD_IN_FRAMES * 2 * bSamples / 8 * nChannels;
				aRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
			} else
			{
				mRecorder = new MediaRecorder();
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			}
			cAmplitude = 0;
			fPath = null;
			state = State.INITIALIZING;
		} catch (Exception e)
		{
			state = State.ERROR;
		}
	}
	
	public void setOutputFile(String argPath)
	{
		try
		{
			fPath = argPath;
			if (rUncompressed)
			{
				if (state != State.INITIALIZING)
					state = State.ERROR;
			}
			else
			{
				mRecorder.setOutputFile(fPath);
			}
		}
		catch (Exception e)
		{
			state = State.ERROR;
		}
	}
	
	public int getMaxAmplitude()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				int result = cAmplitude;
				cAmplitude = 0;
				return result;
			}
			else
			{
				try
				{
					return mRecorder.getMaxAmplitude();
				}
				catch (IllegalStateException e)
				{
					return 0;
				}
			}
		}
		else
		{
			return 0;
		}
	}
	
	public void prepare()
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				if (rUncompressed)
				{
					if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (fPath != null))
					{
						// write file header

						fWriter = new RandomAccessFile(fPath, "rw");
						
						fWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
						fWriter.writeBytes("RIFF");
						fWriter.writeInt(0); // Final file size not known yet, write 0 
						fWriter.writeBytes("WAVE");
						fWriter.writeBytes("fmt ");
						fWriter.write(getBytes(16, ByteOrder.LSB_FIRST)); // Sub-chunk size, 16 for PCM
						fWriter.write(getBytes((short)1, ByteOrder.LSB_FIRST)); // AudioFormat, 1 for PCM
						fWriter.write(getBytes(nChannels, ByteOrder.LSB_FIRST));// Number of channels, 1 for mono, 2 for stereo
						fWriter.write(getBytes(sRate, ByteOrder.LSB_FIRST)); // Sample rate
						fWriter.write(getBytes(sRate*bSamples*nChannels/8, ByteOrder.LSB_FIRST)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						fWriter.write(getBytes((short)(nChannels*bSamples/8),ByteOrder.LSB_FIRST)); // Block align, NumberOfChannels*BitsPerSample/8
						fWriter.write(getBytes(bSamples, ByteOrder.LSB_FIRST)); // Bits per sample
						fWriter.writeBytes("data");
						fWriter.writeInt(0); // Data chunk size not known yet, write 0
						
						buffer = new byte[PERIOD_IN_FRAMES*bSamples/8*nChannels];
						state = State.READY;
					}
					else
					{
						state = State.ERROR;
					}
				}
				else
				{
					mRecorder.prepare();
					state = State.READY;
				}
			}
			else
			{
				state = State.ERROR;
			}
		}
		catch(Exception e)
		{
			state = State.ERROR;
		}
	}
	
	public void release()
	{
		if (state == State.RECORDING)
		{
			stop();
		}
		else
		{
			if ((state == State.READY) & (rUncompressed))
			{
				try
				{
					fWriter.close(); // Remove prepared file
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				(new File(fPath)).delete();
			}
		}
		
		if (rUncompressed)
		{
			aRecorder.release();
		}
		else
		{
			mRecorder.release();
		}
	}
	
	public void reset()
	{
		try
		{
			if (state != State.ERROR)
			{
				release();
				fPath = null; // Reset file path
				cAmplitude = 0; // Reset amplitude
				if (rUncompressed)
				{
					aRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize);
				}
				else
				{
					mRecorder = new MediaRecorder();
					mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
				state = State.INITIALIZING;
			}
		}
		catch (Exception e)
		{
			state = State.ERROR;
		}
	}
	
	public void start()
	{
		if (state == State.READY)
		{
			if (rUncompressed)
			{
				payloadSize = 0;
				aRecorder.startRecording();
				timer = new Timer();
				timer.scheduleAtFixedRate(new RecordTask(), (long)(1000/(((double)sRate)/1000)), (long)(1000/(((double)sRate)/1000)));
			}
			else
			{
				mRecorder.start();
			}
			state = State.RECORDING;
		}
		else
		{
			state = State.ERROR;
		}
	}
	
	public void stop()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				aRecorder.stop();
				
				try
				{
				timer.cancel();

				fWriter.seek(4); // Write size to RIFF header
				fWriter.write(getBytes(36+payloadSize, ByteOrder.LSB_FIRST));
				
				fWriter.seek(40); // Write size to Subchunk2Size field
				fWriter.write(getBytes(payloadSize, ByteOrder.LSB_FIRST));
				
				fWriter.close();
				}
				catch(IOException e)
				{
					state = State.ERROR;
				}
			}
			else
			{
				mRecorder.stop();
			}
			state = State.STOPPED;
		}
		else
		{
			state = State.ERROR;
		}
	}
	
	private short getShort(byte argB1, byte argB2)
	{
		return (short)(argB1 | (argB2 << 8));
	}
	
	private byte[] getBytes(int argInt, ByteOrder byteOrder)
	{
		byte[] bytes = new byte[4];
		if (byteOrder == ByteOrder.LSB_FIRST)
		{
			bytes[0] = (byte) (argInt & 0x00FF);
			bytes[1] = (byte) ((argInt >> 8) & 0x000000FF);
			bytes[2] = (byte) ((argInt >> 16) & 0x000000FF);
			bytes[3] = (byte) ((argInt >> 24) & 0x000000FF);
			return bytes;
		}
		else
		{
			bytes[0] = (byte) ((argInt >> 24) & 0x000000FF);
			bytes[1] = (byte) ((argInt >> 16) & 0x000000FF);
			bytes[2] = (byte) ((argInt >> 8) & 0x000000FF);
			bytes[3] = (byte) (argInt & 0x00FF);
			return bytes;
		}
	}
	
	private byte[] getBytes(short argInt, ByteOrder byteOrder)
	{
		byte[] bytes = new byte[2];
		if (byteOrder == ByteOrder.LSB_FIRST)
		{
			bytes[0] = (byte) argInt;
			bytes[1] = (byte) ((argInt >> 8) & 0x000000FF);
			return bytes;
		}
		else
		{
			bytes[0] = (byte) ((argInt >> 8) & 0x000000FF);
			bytes[1] = (byte) argInt;
			return bytes;
		}
	}
}

