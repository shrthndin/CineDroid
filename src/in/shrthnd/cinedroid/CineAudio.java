package in.shrthnd.cinedroid;

import android.media.*;
import android.os.Process;
import android.util.*;
import java.util.*;
import android.widget.*;

public class CineAudio
{
	private int s, agc, bs, read;
	private short rb[], wb[];
	private int getAmp;
	private volatile boolean preview, audthread;
	private VUMeter m;
	private boolean playFlag;
	private AudioManager am;
	
	public CineAudio(VUMeter meter, boolean isAGCChn, boolean stereo)
	{
		
	    am = (AudioManager)CineMain.myContext.getSystemService(CineMain.myContext.AUDIO_SERVICE);
	
		if(stereo)
			s = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
		else	
			 s = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		
		if(isAGCChn)
			agc = MediaRecorder.AudioSource.VOICE_RECOGNITION;
		else
			agc = MediaRecorder.AudioSource.MIC;
		m = meter;
	}
	
	
	
	public void startPreview()
	{
		
		preview = true;
		
		if(!am.isWiredHeadsetOn())
		{
			playFlag = false;
			Toast.makeText(CineMain.myContext, "Please Plug In Headphones To Monitor Sound", Toast.LENGTH_LONG).show();	
		}
		else
			playFlag = true;
			
		if(!audthread)
			new Thread(new Runnable(){
					public void run()
					{
						audthread = true;
						Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
						bs = AudioRecord.getMinBufferSize(44100, s, AudioFormat.ENCODING_PCM_16BIT);
						m.UseRecorder(false);
						m.start();
						//bsW = AudioTrack.getMinBufferSize(44100, s, AudioFormat.ENCODING_PCM_16BIT)*4;
						rb = new short[bs];
						wb = new short[bs];
						AudioRecord audIn = new AudioRecord(agc, 44100, s, AudioFormat.ENCODING_PCM_16BIT, bs);

						AudioTrack audOut = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, s, AudioFormat.ENCODING_PCM_16BIT, bs, AudioTrack.MODE_STREAM);
						//audOut.setPlaybackRate(44100);
						//Log.v("audio size: ", String.valueOf(bs));
						
						try{
							
						audIn.startRecording();
						audOut.play();
						
						while(preview)
						{
							read = audIn.read(rb, 0, bs);
							if(read != AudioRecord.ERROR_INVALID_OPERATION)	
							{
								wb = new short[read];
								for(int i = 0; i < read; i++)
								{
									//j += rb[i] * rb[i];
									wb[i] = rb[i];
									
									if(playFlag)
										audOut.write(rb, i, 1);
								}
								//If PCM Record dump write buffer
								
								//j /= read;
								//m.setMeasure(Math.sqrt(j)); //For RMS (sqrt(sum(smpl^2) / numsmpls))  
								Arrays.sort(wb);
								m.setMeasure(wb[wb.length - 1]);
								//Poll for headphones change
								if(!am.isWiredHeadsetOn())
								{
									if(playFlag){
									CineMain.uiActivity.runOnUiThread(new Runnable(){
										public void run(){
											Toast.makeText(CineMain.myContext, "Please Plug In Headphones To Monitor Sound", Toast.LENGTH_LONG).show();
										}
									});}
									playFlag = false;
								}
								else
								{
									playFlag = true;
									
								}
							}
						}
						}
						catch(Exception e)
						{
							//Drop out of thread
							CineMain.uiActivity.runOnUiThread(new Runnable(){
								public void run()
								{
									Toast.makeText(CineMain.myContext, "Audio Preview Cannot Be Initialized.", Toast.LENGTH_SHORT).show();
								}
							});
						}
						audOut.stop();
						audIn.stop();
						audOut.release();
						audIn.release();
						audthread = false;
					}
			}).start();
		
	
	}
	
	public void stopPreview()
	{
		preview = false;
		
		
	}
	
	public boolean stillRunning()
	{
		return audthread;
	}
	
}
