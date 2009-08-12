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
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};
	
	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;
	
	private static final int TIMER_INTERVAL = 125;
	
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
	private int				 framePeriod;
	
	private byte[] 			 buffer;
	
	private int				 payloadSize;
	
	private Timer			 timer;
	
	public State getState()
	{
		return state;
	}
	
	/* 
	 * 
	 * A timer takes care of the recording process, as the 
	 * RecordPositionUpdateListener is somewhat buggy.
	 * 
	 */
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
	
	/** 
	 * 
	 * 
	 * Default constructor
	 * 
	 * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
	 * In case of errors, no exception is thrown, but the state is set to ERROR
	 * 
	 */ 
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

				framePeriod = sampleRate / 8;
				bufferSize = framePeriod * 2 * bSamples / 8 * nChannels;
				if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat))
				{ // Check to make sure buffer size is not smaller than the smallest allowed one 
					bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
				}
				
				aRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
				if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
					throw new Exception("AudioRecord initialization failed");
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
	
	/**
	 * Sets output file path, call after initialization.
	 *  
	 * @param output file path
	 * 
	 */
	public void setOutputFile(String argPath)
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				fPath = argPath;
				if (!rUncompressed)
				{
					mRecorder.setOutputFile(fPath);
				}
			}
		}
		catch (Exception e)
		{
			state = State.ERROR;
		}
	}
	
	/**
	 * 
	 * Gets the largest amplitude sampled since the last call to this method.
	 * 
	 * @return returns the largest amplitude since the last call, or 0 when not in recording state. 
	 * 
	 */
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
	

	/**
	 * 
	* Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
	* the recorder is set to the ERROR state, which makes a reconstruction necessary.
	* In case uncompressed recording is toggled, the header of the wave file is written.
	* In case of an exception, the state is changed to ERROR
	* 	 
	*/
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
						fWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
						fWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
						fWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
						fWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
						fWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						fWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
						fWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
						fWriter.writeBytes("data");
						fWriter.writeInt(0); // Data chunk size not known yet, write 0
						
						buffer = new byte[framePeriod*bSamples/8*nChannels];
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
	
	/**
	 * 
	 * 
	 *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
	 *  
	 */
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
	
	/**
	 * 
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped.
	 * In case of exceptions the class is set to the ERROR state.
	 * 
	 */
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
	
	/**
	 * 
	 * 
	 * Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().
	 * 
	 */
	public void start()
	{
		if (state == State.READY)
		{
			if (rUncompressed)
			{
				payloadSize = 0;
				aRecorder.startRecording();
				timer = new Timer();
				timer.scheduleAtFixedRate(new RecordTask(), TIMER_INTERVAL, TIMER_INTERVAL);
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
	
	/**
	 * 
	 * 
	 *  Stops the recording, and sets the state to STOPPED.
	 * In case of further usage, a reset is needed.
	 * Also finalizes the wave file in case of uncompressed recording.
	 * 
	 */
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
					fWriter.writeInt(Integer.reverseBytes(36+payloadSize));
				
					fWriter.seek(40); // Write size to Subchunk2Size field
					fWriter.writeInt(Integer.reverseBytes(payloadSize));
				
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
	
	/* 
	 * 
	 * Converts a byte[2] to a short, in LITTLE_ENDIAN format
	 * 
	 */
	private short getShort(byte argB1, byte argB2)
	{
		return (short)(argB1 | (argB2 << 8));
	}
}

