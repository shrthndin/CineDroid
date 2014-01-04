package in.shrthnd.cinedroid;

import android.hardware.Camera;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.nio.*;
import java.lang.*;
import java.util.*;
import android.app.*;
import android.sax.*;
import android.util.*;
import android.os.*;
import android.media.*;
import android.widget.*;
import java.util.concurrent.*;
import java.nio.channels.*;

public class CineCam implements Camera.PreviewCallback, SurfaceHolder.Callback, Camera.AutoFocusCallback
{
	//Main Variables
	private Camera cam; 
	private SurfaceHolder mPrev;
	private SurfaceView mSurface;
	private Camera.Parameters camParams;
	private int numOfBufs, totalFrmSize, frames, mainfrms, recordYUVFrms, i, valCon, valShp, valSat, valExp;
	private List<Camera.Size> lstSizes;
	private List<Camera.Size> prevSizes;
	private List<Integer> lstFPS = new ArrayList<Integer>();
	private Camera.Size mySize;
	private double startTime, runningFPS, fps; 
	private boolean isAELCK, isAWBLCK, aelck, awblck;
	public static boolean isgoodFocus;
	private FocusSelector myFCS;
	private boolean clampFPS;
	private List<String> isoLst = new ArrayList<String>();
	private boolean camLoaded, yuvRecordReady, yuvRecord;
	private static volatile LinkedBlockingQueue<byte[]> frmList = new LinkedBlockingQueue<byte[]>();
	
	public void surfaceCreated(SurfaceHolder surface)
	{
		
	
	}

	public void surfaceChanged(SurfaceHolder surface, int format, int w, int h)
	{
		if(cam != null){
		cam.stopPreview();
		try{	
			cam.setPreviewCallback(this);
			cam.setPreviewDisplay(mPrev);
			cam.startPreview();
		}
		catch(IOException e)
		{

		}}
	}
	
	public void surfaceDestroyed(SurfaceHolder surface)
	{
		if(cam != null)
		{
			cam.stopPreview();
			cam.release();
		}
	}

	//AutoFocus Callback
	public void onAutoFocus(boolean s, Camera c)
	{
		if(s)
		{
			//Set Selectors color
			if(myFCS != null)
			{
				myFCS.getFocus(getFocus());
				myFCS.setFocusCheck(true);
			}
			
		
		}
	}

	//Interface for Camera Preview Callback (for raw YUV420 frames)
	public void onPreviewFrame(byte data[], Camera c)
	{

		runningFPS = (System.currentTimeMillis() - startTime);

		if(yuvRecord){

		}

		c.addCallbackBuffer(data);
		//add data again
		frames++;

		if(runningFPS > 1000) 
		{
		 	Log.v(CineMain.TAG + " Preview FPS Counter: ", String.format("%.2f",(float)(frames/runningFPS)*1000));
		 	//Log.v(CineMain.TAG + " YUV Frames ", " Total Frames: " + String.valueOf(mainfrms) + " Record YUV Frames: " + String.valueOf(recordYUVFrms));
			startTime = System.currentTimeMillis();
		 	frames=0;
		}
	}
	
	//**Constructor**
	public CineCam(SurfaceView  v)
	{
		
		v.getHolder().addCallback(this);
		v.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);	
		
		mSurface=v;		
		
		mPrev=mSurface.getHolder();
		
		cam = Camera.open(0);
	
		if(cam != null)
		{
			
			camParams = cam.getParameters();
			isAWBLCK = camParams.isAutoWhiteBalanceLockSupported();
			isAELCK = camParams.isAutoExposureLockSupported();
			//Work around for multiple video size listing
			if(camParams.getSupportedVideoSizes() != null)
			{	
				lstSizes = camParams.getSupportedVideoSizes();
				prevSizes = camParams.getSupportedPreviewSizes();
				Camera.Size s = camParams.getPreferredPreviewSizeForVideo();
				if(s!=null)
					camParams.setPreviewSize(s.width, s.height);
	
			}
			else //Preview Size is Video Res Size (Snapdragon devices)
			{
				lstSizes = camParams.getSupportedPreviewSizes();
				prevSizes = camParams.getSupportedPreviewSizes();
				//Check nearest HD Preview Size or default
				Camera.Size c = cam.new Size(CineMain.myDisplay.right, CineMain.myDisplay.bottom);
				if(lstSizes.contains(c))
				{
					camParams.setPreviewSize(c.width, c.height);
				}
				else{
					camParams.setPreviewSize(lstSizes.get(0).width, lstSizes.get(0).height);
				}
			}
			
			
			
			
			for(int i = 0; i < camParams.getSupportedPreviewFrameRates().size(); i++)
			{
				if(camParams.getSupportedPreviewFrameRates().get(i) <= 30)
				{
					lstFPS.add(camParams.getSupportedPreviewFrameRates().get(i));
				}
				else{
					break;
				}
			}
			camParams.setRecordingHint(true); 
			camParams.setFlashMode(camParams.FLASH_MODE_OFF);
			cam.setParameters(camParams);
			cam.setPreviewCallbackWithBuffer(this);
			camLoaded = true;
			
		}
		else
		{
			Log.e(CineMain.TAG + " Camera Initialization: ", "Camera is NULL");
			//error
			CineMain.uiActivity.finish();
		}
	}
	
	//Core Methods
	public boolean isLoaded()
	{
		return camLoaded;
	}
	
	

	public List<Camera.Size> getPreviewResolutions()
	{
		return prevSizes;
	}
	
	// /*
	//DEBUG: Setting (and return new params)
	public String _test(boolean g, String k, String v)
	{
		if(g)
		{
		camParams.set(k,v);
		cam.setParameters(camParams);
		camParams = cam.getParameters();
		return camParams.flatten();
		}
		else
		{
			return camParams.get(k);
		}
	}

	
	//DEBUG: Parameters output
	public String _dump()
	{
		camParams = cam.getParameters();
		return camParams.flatten();		
	}
	
	
	public Camera getCamera()
	{
		return cam;
	}
	
	public SurfaceHolder getPreviewSurface()
	{
		return mPrev;
	}
	
	
	public Camera.Size currentRes()
	{
		return mySize;
	}
	
	public int currentFrameRate()
	{
		return (int)fps;
	}
	
	public List<Camera.Size> getResolutions()
	{
		return lstSizes;
	}
	
	public List<Integer> getZoomRatios()
	{
		if(camParams.isZoomSupported())
		{
			return camParams.getZoomRatios();
		}
		return null;
	}
	
	public void setZoomRatio(int v)
	{
		if(camParams.isZoomSupported())
		{
			camParams.setZoom(v);
			cam.setParameters(camParams);
		}
		
	}
	
	public int getMaxZoom()
	{
		if(camParams.isZoomSupported())
			return camParams.getMaxZoom();
		else
			return 0;
	}
	
	public void setFPSStable(boolean s)
	{
		clampFPS = s;
		try
		{
			camParams.set("preview-frame-rate-mode","frame-rate-fixed");
		}
		catch(RuntimeException e)
		{}
	}
	
	
	//Select focus/metering areas if supported
	public void setFocusSelector(FocusSelector sel)
	{
		myFCS = sel;
		
	}
	
	public boolean hasFocusArea()
	{
		if(camParams.getMaxNumFocusAreas() > 0)
			return true;
			
		return false;
	}
	
	public boolean hasExpArea()
	{
		if(camParams.getMaxNumMeteringAreas() >0)
			return true;
		
		return false;
	}
	
	public boolean setFocusArea(Rect area)
	{
		List<Camera.Area> l = new ArrayList<Camera.Area>();
		l.add(new Camera.Area(CineUtils.viewToCameraArea(area), 1000));
		Log.v(CineMain.TAG + " Focus Selector Area: ", String.valueOf(l.get(0).rect.toString()));
		if(camParams.getMaxNumFocusAreas() > 0)
		{
			try{
				camParams.setFocusAreas(l);
				cam.setParameters(camParams);
				return true;
			}
			catch(Exception e)
			{
				Log.e(CineMain.TAG + "Focus Set Failed", e.toString());
				return false;
			}
		}
		return false;
		
	}
	
	public boolean setExpArea(Rect sel)
	{
		List<Camera.Area> l = new ArrayList<Camera.Area>();
		l.add(new Camera.Area(CineUtils.viewToCameraArea(sel), 1000));
		Log.v(CineMain.TAG + " Exposure Selector Area: ", String.valueOf(l.get(0).rect.toString()));
		if(camParams.getMaxNumMeteringAreas() > 0)
		{
			try{
			camParams.setMeteringAreas(l);
			cam.setParameters(camParams);
			return true;
			}
			catch(Exception e)
			{
				Log.e(CineMain.TAG + "Exposure Set Failed.", e.toString());
				return false;
			}
		}
		return false;
	}
	
	public int getMinExpComp()
	{
		return camParams.getMinExposureCompensation();
	}
	public int getMaxExpComp()
	{
		return camParams.getMaxExposureCompensation();
	}
	public float getExpCompStep()
	{
		return camParams.getExposureCompensationStep();
	}
	
	public void setExpComp(int v)
	{
		if(v >= getMinExpComp() && v <= getMaxExpComp())
			camParams.setExposureCompensation(v);
			cam.setParameters(camParams);
	}
	
	public void setAutoFocus(boolean v)
	{
		if(v && camParams.getSupportedFocusModes().contains(camParams.FOCUS_MODE_CONTINUOUS_VIDEO))
		{
			camParams.setFocusMode(camParams.FOCUS_MODE_CONTINUOUS_VIDEO);
		}
		else if (!v && camParams.getSupportedFocusModes().contains(camParams.FOCUS_MODE_AUTO))
		{
				camParams.setFocusMode(camParams.FOCUS_MODE_AUTO);
		}
		cam.setParameters(camParams);
	}
	
	public boolean setFocusMode(String m)
	{
		try{
			camParams.setFocusMode(m);
			cam.setParameters(camParams);
			return true;
		}
		catch(Exception e)
		{
			Log.e(CineMain.TAG + "Set Focus Mode Failed.", e.toString());
			return false;
		}
	}
	
	public boolean getStablizer()
	{
		return camParams.isVideoStabilizationSupported();
	}
	
	public boolean setStablizer(boolean v)
	{
		if(camParams.isVideoStabilizationSupported())
		{
			camParams.setVideoStabilization(v);
			return true;
		}
		return false;
	}
	
	public List<String> getFocusModes()
	{
		return camParams.getSupportedFocusModes();
	}
	
	public void focus()
	{
		
		if(myFCS != null)
			myFCS.setFocusCheck(false);
			
		//cam.cancelAutoFocus();
		cam.autoFocus(this);
		
	}
	
	
	
	//Locks for Exp, WB - returns true if supported, false if not
	public boolean setAELock(boolean set)
	{
		if(set == true && isAELCK == true)
		{
			camParams.setAutoExposureLock(true);
			cam.setParameters(camParams);
			aelck = set;
			return true;
		}
		else if(isAELCK)
		{
			camParams.setAutoExposureLock(false);
			cam.setParameters(camParams);
			aelck = set;
			return false;
			
		}
		else
			return false;
	}
	
	public List<String> getWB()
	{
		List<String> wb = new ArrayList<String>();
		int kel_i = 0;
		wb.add("AUTO");
		if(camParams.isAutoWhiteBalanceLockSupported())
		{
			wb.add("LOCK");
		}
		while(kel_i < camParams.getSupportedWhiteBalance().size())
		{
			String c = camParams.getSupportedWhiteBalance().get(kel_i);

			if(c.contains(Camera.Parameters.WHITE_BALANCE_INCANDESCENT))
			{
				wb.add("3200");
			}
			if(c.contains(Camera.Parameters.WHITE_BALANCE_FLUORESCENT))
			{
				wb.add("4000");
			}
			if(c.contains(Camera.Parameters.WHITE_BALANCE_DAYLIGHT))
			{
				wb.add("5600");
			}
			if(c.contains(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT))
			{
				wb.add("8000");
			}
			
			kel_i++;
		}
		
		return wb;
	}
	
	public void setWB(String kelvin)
	{
		
		if(kelvin == "AUTO")
		{
			camParams.setWhiteBalance(camParams.WHITE_BALANCE_AUTO);
		}
		else if(kelvin == "LOCK")
		{
			setAWBLock(true);
		}
	    else if(kelvin == "3200")
		{
			camParams.setWhiteBalance(camParams.WHITE_BALANCE_INCANDESCENT);
		}
		else if(kelvin == "4000")
		{
			camParams.setWhiteBalance(camParams.WHITE_BALANCE_FLUORESCENT);
		}
		else if(kelvin == "5600")
		{
			camParams.setWhiteBalance(camParams.WHITE_BALANCE_DAYLIGHT);
		}
		else if(kelvin == "8000")
		{
			camParams.setWhiteBalance(camParams.WHITE_BALANCE_CLOUDY_DAYLIGHT);
		}
		try{
			cam.setParameters(camParams);
		}
		catch(RuntimeException e)
		{
			
		}
	}
	
	public boolean setLight(boolean set)
	{
		if(!set)
		{
			camParams.setFlashMode(camParams.FLASH_MODE_OFF);
			cam.setParameters(camParams);
			return false;
		}
		else
		{
			camParams.setFlashMode(camParams.FLASH_MODE_TORCH);
			cam.setParameters(camParams);
			return true;
		}
		
	}
	public boolean setAWBLock(boolean set)
	{
		if(set == true && isAWBLCK == true)
		{
			camParams.setAutoWhiteBalanceLock(true);
			cam.setParameters(camParams);
			awblck = set;
			return true;
		}
		else if(isAWBLCK)
		{	
			camParams.setAutoWhiteBalanceLock(false);
			cam.setParameters(camParams);
			awblck = set;
			return false;
		}
		else
			return false;
	}
	
	public float[] getFocus()
	{
		float[] a = new float[3];
		camParams = cam.getParameters();
		camParams.getFocusDistances(a);	
		return a;
	}
	
	//Sharpness, Saturation and Contrast for Qualcomm Chipsets for now.
	public int getSaturation()
	{
		if(camParams.get("saturation") != null)
			return camParams.getInt("saturation");
		else
			return -1;
	}
	
	public int getContrast()
	{
		if(camParams.get("contrast") != null)
			return camParams.getInt("contrast");
		else
			return -1;
	}
	
	public int getSharpness()
	{
		if(camParams.get("sharpness") != null)
		{
			return camParams.getInt("sharpness");
		}
		else
			return -1;
	}
	
	public int getMaxSaturation()
	{
		String max = camParams.get("saturation-max");
		if(max != null)
		{
			return Integer.valueOf(max);
		}
		else
		{
			max = camParams.get("max-saturation");
			if(max != null){
				return Integer.valueOf(max);
			}
		}
		return -1;
	}
	

	public void setSaturation(int v)
	{
		valSat = v;
		try{
			camParams.set("saturation", String.valueOf(v));
			cam.setParameters(camParams);
		}
		catch(RuntimeException e){
			return;
		}
		
	}
	
	public void setContrast(int v)
	{
		
		try{
			camParams.set("contrast", String.valueOf(v));
			cam.setParameters(camParams);}
		catch(RuntimeException e){
			return;
		}
	}
	
	public int getMaxContrast()
	{
		String max = camParams.get("contrast-max");
		if(max != null)
		{
			return Integer.valueOf(max);
		}
		else
		{
			max = camParams.get("max-contrast");
			if(max != null)
			{
				return Integer.valueOf(max);
			}
		}
		return -1;	
	}
	
	public void setSharpness(int v)
	{
		try{
			camParams.set("sharpness", String.valueOf(v));
			cam.setParameters(camParams);}
		catch(RuntimeException e)
		{
			return;
		}
	}
	
	public int getMaxSharpness()
	{
		String max = camParams.get("sharpness-max");
		if(max != null)
		{
			return Integer.valueOf(max);
		}
		else
		{
			max = camParams.get("max-sharpness");
			if(max != null)
				return Integer.valueOf(max);
		}
			return -1;
	}
	
	public String[] getISOValues()
	{
		String v[];
		if(camParams.get("iso-values") != null)
		{
				v = camParams.get("iso-values").split(",");
				for(int i = 0; i < v.length; i++)
				{
					isoLst.add(v[i]);
				}
				return v;
		}
		return null;
	}
	
	public boolean setISO(String v)
	{
		if(isoLst.contains(v))
		{
			try{
				camParams.set("iso", v);
				cam.setParameters(camParams);
				return true;
			}
			catch(Exception e)
			{
				Log.e(CineMain.TAG + " ISO Setting Failed", e.toString());
				return false;
			}
		}
		return false;
	}
	
	public List<Integer> getFPSList()
	{
		return lstFPS;
		
	}
	
	public boolean setFPS(double fr)
	{
		fps = fr;
			cam.stopPreview();
			try{
				camParams.setPreviewFpsRange((int)(fps * 1000), (int)(fps*1000));
				cam.setParameters(camParams);
			}
			catch(RuntimeException e)
			{
				try{
					camParams.setPreviewFrameRate((int)fps);
					cam.setParameters(camParams);
				}
				catch(RuntimeException e2)
				{
					cam.startPreview();
					Log.e(CineMain.TAG + "Multiple FPS Setting Methods Failed", e2.toString());
					return false;
				}
			}
			cam.startPreview();
			
			
			return true;
			
	}
	
}
	
