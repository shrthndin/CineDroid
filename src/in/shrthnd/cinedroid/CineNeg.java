package in.shrthnd.cinedroid;

import android.hardware.*;
import android.media.*;
import java.io.*;
import android.util.*;
import android.view.*;
import android.os.*;
import java.text.*;
import java.util.*;

public class CineNeg
{
	public static class CineCodec
	{
		
		public static int H264 = 0;
		//For Later
		/*public static int PRORES = 1;
		public static int DNXHD = 2;*/
	}
	
	public static class RecParams{
		public int videoCodec = CineCodec.H264;
		public int videoFPS;
		public int videoWidth;
		public int videoHeight;
		public int videoBitRate;
		public int audioSampleRate;
		public int audioBitRate;
		public int recordLength;
		public String outFileNme;
		public boolean isAudioAGC = false;
		public boolean isAudioStereo = false;
		public boolean withAudio = true;
		public boolean isAudioCompressed = true;
		public boolean isRAW; //for YUV
		public boolean isRAWSplit;
		public boolean isFFmpeg; //for FFmpeg pipe encoding
	}
	
	private MediaRecorder rec;
	private Camera cam;
	private boolean isMR, isFFmpeg, isRAW;
	private static RecParams params;
	
	
	public void setRecordParams(RecParams p)
	{
		params = p;
	}
	
	public boolean initCameraRecorder(CineCam c)
	{
		rec = new MediaRecorder();
		if(params.videoCodec == CineCodec.H264)
		{
			//TIMESTAMP = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			//File f = new File(Environment.DIRECTORY_DCIM, "FOOTAGE_"+TIMESTAMP+".mp4");
			cam = c.getCamera();
			cam.stopPreview();
			cam.unlock();
			rec.setCamera(cam);
			
			
			
			//Setup Audio First!
			if(params.withAudio && params.isAudioCompressed)
			{
				if(!params.isAudioAGC)
					rec.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
				else
					rec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
				
				
			
				if(params.isAudioStereo)
					rec.setAudioChannels(2);
				else
					rec.setAudioChannels(1);
				
				rec.setAudioEncodingBitRate(params.audioBitRate);
				rec.setAudioSamplingRate(params.audioSampleRate);
			}	
			else if(params.withAudio && !params.isAudioCompressed)
			{
				//Setup PCM Audio Recorder
			
			}

			//Setup Video Recorder
			rec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			rec.setVideoEncodingBitRate(params.videoBitRate);
			rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			rec.setVideoSize(params.videoWidth, params.videoHeight);
			
			rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
			//rec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
			rec.setCaptureRate(c.currentFrameRate());
			rec.setVideoFrameRate(params.videoFPS);
			
			//rec.setMaxDuration(30000);
			rec.setOutputFile(params.outFileNme);
			rec.setPreviewDisplay(c.getPreviewSurface().getSurface());
			try{
				rec.prepare();
			}
			catch(Exception e)
			{
				Log.e(CineMain.TAG + " Camera .H264 Record Error", e.getMessage());
				return false;
			}
			return true;	
		}
		return false;
	}
	
	public MediaRecorder getRecorderObj()
	{
		return rec;
	}
	
	public boolean start()
	{
		try{
		rec.start();
		return true;
		}
		catch(Exception e)
		{
			return false;
		}
		
	}
	
	public void stop()
	{
		rec.stop();
		rec.reset();
		rec.release();
		
		try{
			cam.reconnect();
		}
		catch(Exception e)
		{
			Log.e(CineMain.TAG + " Could Not Reconnect Camera After .H264 Record", e.getStackTrace().toString());
			}
		cam.lock();
	}
	
	
	
}
