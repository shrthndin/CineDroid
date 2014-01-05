package in.shrthnd.cinedroid;

/*
	CineDroid - Android Pro Video Camera
	Copyright (c) 2013 shrthnd interactive/shrthnd.in@gmail.com
*/
	
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;	
import android.util.*;
import android.content.*;
import java.util.*;
import android.hardware.Camera;
import java.io.*;
import java.text.*;
import android.media.*;
import android.graphics.drawable.*;

	
public class CineMain extends Activity 
{
	//private static final boolean isPro = false; //Compile time flag for pro version code
	private CineCam cam;
	private CineNeg rec;
	private CineNeg.RecParams recParams = new CineNeg.RecParams();
	private CineAudio aud;
	private String STAMP = "";
	private String outDirectory;
	public static ActivityManager myManager;
	private boolean recording, expLCK, lightToggle, isAFOn;
	public static final String TAG = "CineDroid";
	public static Context myContext;
	public static Rect myDisplay = new Rect();
	private List<String> kelvinList = new ArrayList<String>();
	private List<String> resList = new ArrayList<String>();
	private int kel_i=0, res_i=0, fps_i=0, fcs_i=0;
	private List<String> wbList;
	private List<String> fcsList =  new ArrayList<String>();
	private boolean inRackFocus = false, expSet, resetExp = false;
	private boolean runRackFocus = true;
	private boolean rfToOne;
	private TextView btnKelvin;
	private VUMeter meter;
	public static Toast msg;
	private volatile List<Rect> rackAreas = new ArrayList<Rect>();
	private int rackDelay = 2000; //two second default
	private int expCompLevel = 0;
	private AlertDialog.Builder diabuild;
	private AlertDialog dialog;
	private FocusSelector fs;
	private ExposureSelector es;
	private TextView btnRes;
	private List<Integer> zoomFactorList = new ArrayList<Integer>();
	private int zoomFactor;
	private File file;
	public static Activity uiActivity;
	public static int frmCnt;
	public static boolean cntFrms;
	public static int take, shot, w_480, h_480, myres;
	private CropView crpView;
	private ArrayAdapter<CharSequence> isoList;
	private boolean recordCounter;
	private FileOutputStream pstos;
	private boolean hasPst;
	private HtmlAd htmlAd;
	
	
	private class TIMECODE
	{
		public boolean isTC = true;
		public int absFrms;
		public int frms;
		public int secs;
		public int mins;
		public int hours;
		public String counter;
	}
	
	private class PRESETINFO
	{
		public int sat;
		public int shrp;
		public int con;
		public boolean stab;
		public int crop;
		public boolean crop43;
		public int iso;
		public boolean audioprv;
		public int vidBit;
		public int vidRes;
		public int vidFPS;
		public int audSam;
		public int audRate;
		public int focus;
		public int kelvin;
		public int fpsProject;
	}
	
	private TIMECODE tc = new TIMECODE();
	private PRESETINFO pst = new PRESETINFO();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
	
		super.onCreate(savedInstanceState);
	   
		//Static Variables
		myManager = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		myContext = this.getApplicationContext();
		uiActivity = this;
		
		//Main View
		setContentView(R.layout.main);
		
		
		//Load Camera and Recorder
		cam = new CineCam((SurfaceView)findViewById(R.id.prvCam));
		rec = new CineNeg();
		//cam._test(true, "preview-frame-rate-mode", "frame-rate-fixed");
		//Make App directory
		outDirectory = Environment.getExternalStorageDirectory() +"/" + "CineDroid";
		file = new File(outDirectory);
		if(!file.exists())
			file.mkdir();
			
		//Get Camera Parameters
		File f = new File(Environment.getExternalStorageDirectory() + "/CineDroid", "camera.parameters.txt");

		if(!f.exists())
		{
			try{
				f.createNewFile();
				FileOutputStream s = new FileOutputStream(f);
				s.write("CineDroid - Camera Parameters Output \n\n".getBytes());
				s.write("Device Manufacturer: ".getBytes());
				s.write(Build.MANUFACTURER.getBytes());
				s.write("\nDevice Model: ".getBytes());
				s.write(Build.MODEL.getBytes());
				s.write("\nDevice Chipset: ".getBytes());
				s.write(Build.BOARD.getBytes());
				s.write("\n\nCamera Parameters Key List:\n".getBytes());
				String params = new String(cam._dump());
				params = params.replace(";","\n\n");
				s.write(params.getBytes());
				s.close();
				Toast.makeText(myContext, "Output Camera Parameters to " + Environment.getExternalStorageDirectory() + "/CineDroid/camera.parameters.txt", Toast.LENGTH_LONG).show();
			}
			catch(IOException e)
			{

			}
		}
		
		//Get Preset Values
		hasPst = getPreset();
		
		//ISO List
		
		/*String[] isos = cam.getISOValues();
		if(isos != null)
		{
			isoList = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
			for(int i = 0; i < isos.length; i++)
			{
				isoList.add(isos[i]);
			}
			//Read Setting
			//((Spinner)findViewById(R.id.spinISO)).setSelection(pst.iso);
			((Spinner)findViewById(R.id.spinISO)).setAdapter(isoList);
			((Spinner)findViewById(R.id.spinISO)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
				
				public void onItemSelected(AdapterView<?> parent, View v, int pos, long id)
				{
					if(!cam.setISO((String)parent.getItemAtPosition(pos)))
						Toast.makeText(myContext, "Error Setting ISO Value", Toast.LENGTH_SHORT).show();
				}
				
				public void onNothingSelected(AdapterView<?> parent)
				{
					
				}
			});
		}
		else
		{*/
			findViewById(R.id.spinISO).setVisibility(View.GONE);
			findViewById(R.id.txtISO).setVisibility(View.GONE);
		//}
		
		//Crop Guides Drawing
		ArrayAdapter<CharSequence> croplist = ArrayAdapter.createFromResource(this, R.array.Crops, android.R.layout.simple_spinner_item);

		croplist.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		((Spinner)findViewById(R.id.spinCrop)).setAdapter(croplist);
		
		((Spinner)findViewById(R.id.spinCrop)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			
				public void onItemSelected(AdapterView<?> parent, View view, 
										   int pos, long id) {
					// An item was selected. You can retrieve the selected item using
					// parent.getItemAtPosition(pos)
					if(pos == 0)
					{
						crpView.drawCrop(false);
					}
					if(pos == 1)
					{
							crpView.setSize(recParams.videoWidth, recParams.videoHeight);
							crpView.setCrop(1.85f);
							crpView.drawCrop(true);
					}
					if(pos == 2)
					{
						crpView.setSize(recParams.videoWidth, recParams.videoHeight);
						crpView.setCrop(2.35f);
						crpView.drawCrop(true);
					}
				}

				public void onNothingSelected(AdapterView<?> parent) {
					// Another interface callback
				}
			
		});
		
		
		diabuild = new AlertDialog.Builder(this);
		
		diabuild.setIcon(R.drawable.launcher_96);
		diabuild.setTitle("CineDroid v1.0.9");
		diabuild.setMessage("Copyright 2013 shrthnd interactive \n\n *1.0.9 Caught white balance exception that would crash on certain devices. \n\n Reverted ad service to StartApp. \n\n Minor bug fixes. \n\n Please email camera.parameters.txt file if you still have unresolved camera issues!");
		
		dialog = diabuild.create();
	
		
		
		//Set VU Meter to Recorder
		meter = ((VUMeter)findViewById(R.id.vuMtr));
		
		aud = new CineAudio(meter, false, false);
		if(hasPst){
			if(pst.audioprv){
				aud.startPreview();
				((CheckBox)findViewById(R.id.chkPreAudio)).setChecked(true);
			}
			else
			{
				((CheckBox)findViewById(R.id.chkPreAudio)).setChecked(false);
			}
		
		}
		else
		{
			aud.startPreview();
		}
		
		//Window size
		myDisplay.right = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
		myDisplay.bottom = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
		
		
		//Dump Camera Parameter to LogCat
		Log.v(TAG + " Camera Parameters: ", cam._dump());
		
		//Get Zoom Factor List
		if(cam.getZoomRatios() != null)
		{
			zoomFactorList = cam.getZoomRatios();
			((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
		}
		else
			findViewById(R.id.btnZoom).setVisibility(View.INVISIBLE);
			
		
		
		//Get Kelvin List
		kelvinList = cam.getWB();
		
		kel_i=0;

		Log.v(TAG + " Supported Kelvin Temperatures: ", kelvinList.toString());
		//Default White Balance
		btnKelvin = ((TextView)findViewById(R.id.btnKelvin));		btnKelvin.setText("AUTO");
		btnKelvin.setTextColor(Color.rgb(255,255,255));
		cam.setWB("AUTO");
		
		//Get Resolutions
		for(res_i = 0; res_i < cam.getResolutions().size();res_i++)
		{
			Camera.Size c = cam.getResolutions().get(res_i);
			
		    if(c.width == 1280 && c.height == 720)
			{
				resList.add("HD720");
			}
			else if(c.width == 1920 && c.height == 1080)
			{
				resList.add("HD1080");
			}
			else if(c.width == 1920 && c.height == 1088)
			{
				resList.add("HDOVER");
			}
			else if(c.width == 720 && c.height == 480)
			{
				resList.add("SD480");
			}
			else if(c.width == 720 && c.height == 576)
			{
				resList.add("SD576");
			}
		}
		
		//Get Wanted Focus Modes
		for(fcs_i = 0; fcs_i < cam.getFocusModes().size(); fcs_i++){
			int l = fcs_i;
		if(cam.getFocusModes().get(l).contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
		{
			fcsList.add(cam.getFocusModes().get(l));
		}
		else if(cam.getFocusModes().get(l).contains(Camera.Parameters.FOCUS_MODE_AUTO))
		{
			fcsList.add(cam.getFocusModes().get(l));
		}
		else if(cam.getFocusModes().get(l).contains(Camera.Parameters.FOCUS_MODE_MACRO))
		{
			fcsList.add(cam.getFocusModes().get(l));
		}
		else if(cam.getFocusModes().get(l).contains(Camera.Parameters.FOCUS_MODE_INFINITY))
		{
			fcsList.add(cam.getFocusModes().get(l));
		}
		else if(cam.getFocusModes().get(l).contains(Camera.Parameters.FOCUS_MODE_FIXED))
		{
			fcsList.add(cam.getFocusModes().get(l));
		}
		}
		((TextView)findViewById(R.id.btnFCS)).setText("    TF  ");
		findViewById(R.id.fcsSel).setVisibility(View.VISIBLE);
		fcs_i = fcsList.indexOf(Camera.Parameters.FOCUS_MODE_AUTO);
		cam.setFocusMode(fcsList.get(fcs_i));
		isAFOn = false;
		fs = (FocusSelector)findViewById(R.id.fcsSel);
		cam.setFocusSelector(fs);

		
		btnRes = (TextView)findViewById(R.id.btnRES);
		if(resList.contains("HD1080"))
		{
			btnRes.setText("HD1080");
			recParams.videoWidth=1920;
			recParams.videoHeight=1080;
			res_i = resList.indexOf("HD1080");
		}
		else if(resList.contains("HDOVER"))
		{
			btnRes.setText("HD1080");
			recParams.videoWidth=1920;
			recParams.videoHeight=1088;
			res_i = resList.indexOf("HDOVER");
		}
		else if(resList.contains("HD720") && (!resList.contains("HD1080") || !resList.contains("HDOVER")))
		{
			btnRes.setText("HD720");
			recParams.videoWidth=1280;
			recParams.videoHeight=720;
			res_i = resList.indexOf("HD720");	
		}
		else if(resList.contains("SD480")) 
		{
			btnRes.setText("SD480");
			recParams.videoWidth=720;
			recParams.videoHeight=480;
			res_i = resList.indexOf("SD480"); 
			
		}
	
	
		Log.v(TAG + " Supported Resolutions: ", resList.toString());
		
		Log.v(TAG + " Supported Frame Rates: ", cam.getFPSList().toString());
		
		TextView fpsBtn = (TextView)findViewById(R.id.btnFPS);
		cam.setFPSStable(true);
		
		//Set Default Frame Rate
		if(cam.getFPSList().contains(24))
		{	
			fpsBtn.setText("24FPS");
			if(cam.setFPS(24))
				fps_i = cam.getFPSList().indexOf(24);
			else{
				Toast.makeText(myContext, "Error Setting Frame Rate", Toast.LENGTH_SHORT).show();
				fpsBtn.setText("XFPS");
				fpsBtn.setTextColor(Color.RED);
			}
		}
		else if(!cam.getFPSList().contains(24) && cam.getFPSList().contains(30))
		{	
			fpsBtn.setText("30FPS");
			if(cam.setFPS(30))
				fps_i = cam.getFPSList().indexOf(30);
			else{
				Toast.makeText(myContext, "Error Setting Frame Rate", Toast.LENGTH_SHORT).show();
				fpsBtn.setText("XFPS");
				fpsBtn.setTextColor(Color.RED);
			}
		}
		else
		{
			if(cam.setFPS(cam.getFPSList().get(0)))
				fpsBtn.setText(String.valueOf(cam.getFPSList().get(0)) + "FPS");
			else
			{
				Toast.makeText(myContext, "Error Setting Frame Rate", Toast.LENGTH_SHORT).show();
				fpsBtn.setText("XFPS");
				fpsBtn.setTextColor(Color.RED);
			}
			fps_i = 0;
		}
		
		//Force 24FPS Checkbox
		((CheckBox)findViewById(R.id.chk24fps)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			
			public void onCheckedChanged(CompoundButton me, boolean v)
			{
				if(v)
				{
					if(cam.setFPS(24))
						((TextView)findViewById(R.id.btnFPS)).setText("24FPS");
					else
						Toast.makeText(myContext, "Error Forcing Frame Rate.", Toast.LENGTH_SHORT).show();
				}
				else
				{
					((TextView)findViewById(R.id.btnFPS)).setText(String.valueOf(cam.getFPSList().get(0)) + "FPS");
					if(!cam.setFPS(cam.getFPSList().get(0)))
					{	
						((TextView)findViewById(R.id.btnFPS)).setText("XFPS");
						((TextView)findViewById(R.id.btnFPS)).setTextColor(Color.RED);
					}
				}
			}
		});
		
		
		
		//Set Focus Long Click Handler
		fs.setOnLongClickListener(new View.OnLongClickListener(){
			public boolean onLongClick(View v)
			{
				focusLongClick(v);
				return true;
			}
			
		});
		
		//Set Exposure Long Click Handler
	    es = (ExposureSelector)findViewById(R.id.expSel);
		es.setOnLongClickListener(new View.OnLongClickListener(){
			public boolean onLongClick(View v)
			{
				expLongClick(v);
				return true;
			}
		});
		
		//Set Long Click to Reset Zoom and Exposure
		((TextView)findViewById(R.id.btnEXP)).setOnLongClickListener(new View.OnLongClickListener(){
			public boolean onLongClick(View v)
			{
				((TextView)findViewById(R.id.btnEXP)).setText("0.0+/-");
				expCompLevel = 0;
				cam.setExpComp(0);
				return true;
			}
		});
		
		((TextView)findViewById(R.id.btnZoom)).setOnLongClickListener(new View.OnLongClickListener(){
		
			public boolean onLongClick(View v)
			{
				((TextView)findViewById(R.id.btnZoom)).setText("Z1.00");
				cam.setZoomRatio(0);
				return true;
			}
		});
		
		crpView =  (CropView)findViewById(R.id.cropGuides);
		//Log.v("Crop Guides Size", String.valueOf(crpView.getWidth()));
		
		//Set Camera Image Settings
		
		//Check for all three first!
		if(cam.getMaxContrast() != -1 && cam.getMaxSaturation() != -1 && cam.getMaxSharpness() != -1)
		{
			int c,sa,srp;
			if(hasPst)
			{
				c=pst.con;
				sa=pst.sat;
			    srp=pst.shrp;
			}
			else
			{
				c=cam.getContrast();
				sa=cam.getSaturation();
				srp=cam.getSharpness();
			}
			((SeekBar)findViewById(R.id.skbSat)).setMax(cam.getMaxSaturation());
			((SeekBar)findViewById(R.id.skbSat)).setProgress(sa);
			((SeekBar)findViewById(R.id.skbCon)).setMax(cam.getMaxContrast());
			((SeekBar)findViewById(R.id.skbCon)).setProgress(c);
			((SeekBar)findViewById(R.id.skbShr)).setMax(cam.getMaxSharpness());
			((SeekBar)findViewById(R.id.skbShr)).setProgress(srp);
			//Set preset values
			cam.setSaturation(sa);
			cam.setContrast(c);
			cam.setSharpness(srp);
			((SeekBar)findViewById(R.id.skbSat)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			public void onStopTrackingTouch(SeekBar s)
			{
				
			}
			public void onStartTrackingTouch(SeekBar s)
			{
				
			}
			public void onProgressChanged(SeekBar s, int v, boolean b)
			{
				if(cam.getMaxSaturation() > 0)
					cam.setSaturation(v);
			}
			});
		
			((SeekBar)findViewById(R.id.skbCon)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
				public void onStopTrackingTouch(SeekBar s)
				{

				}
				public void onStartTrackingTouch(SeekBar s)
				{

				}
				public void onProgressChanged(SeekBar s, int v, boolean b)
				{
					if(cam.getMaxContrast() > 0)
						cam.setContrast(v);
				}
			});
			
			((SeekBar)findViewById(R.id.skbShr)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
				public void onStopTrackingTouch(SeekBar s)
				{

				}
				public void onStartTrackingTouch(SeekBar s)
				{

				}
				public void onProgressChanged(SeekBar s, int v, boolean b)
				{
					if(cam.getMaxSharpness() > 0)
						cam.setSharpness(v);
				}
			});
			}
			else //Hide Settings bars
			{
				findViewById(R.id.txtCon).setVisibility(View.GONE);
				findViewById(R.id.skbCon).setVisibility(View.GONE);
				findViewById(R.id.txtSat).setVisibility(View.GONE);
				findViewById(R.id.skbSat).setVisibility(View.GONE);
				findViewById(R.id.txtShr).setVisibility(View.GONE);
				findViewById(R.id.skbShr).setVisibility(View.GONE);
			}
			
			if(cam.getStablizer())
			{
				if(hasPst){
				if(pst.stab){
					((CheckBox)findViewById(R.id.chkStable)).setChecked(true);
					cam.setStablizer(true);
				}}
			((CheckBox)findViewById(R.id.chkStable)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton me, boolean v)
				{
					if(v)
					{
						if(cam.getStablizer())
						{
							cam.setStablizer(v);
						}
					}
					else
					{
						if(cam.getStablizer())
						{
							cam.setStablizer(false);
						}
					}
				}
			});
			}
			else //Hide Stablizer
			{
				((CheckBox)findViewById(R.id.chkStable)).setVisibility(View.GONE);
			}
			
			//Preview Audio Checked
			((CheckBox)findViewById(R.id.chkPreAudio)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				
				public void onCheckedChanged(CompoundButton b, boolean v)
				{
					if(v)
					{
						while(aud.stillRunning() && !recording)
						{}
							aud.startPreview();
					}
					else
					{
						if(aud.stillRunning() && !recording)
							aud.stopPreview();
							Toast.makeText(myContext, "Audio Preview Stopped", Toast.LENGTH_SHORT).show();
							
					}
				}
			});
			
			//AGC Checked
			((CheckBox)findViewById(R.id.chkAGC)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton b, boolean v)
				{
					if(v)
					{
						aud.stopPreview();
						while(aud.stillRunning())
						{}
	
						aud = new CineAudio(meter, true, false);
						aud.startPreview();
					}
					else
					{
						aud.stopPreview();
						while(aud.stillRunning()){}
						
							
						aud = new CineAudio(meter, false, false);
						aud.startPreview();
					}
				}
				
			});
			
			((CheckBox)findViewById(R.id.chk43box)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				
				public void onCheckedChanged(CompoundButton b, boolean v)
				{
					if(v)
					{
						//Draw 4:3 Center Crop
						crpView.setSize(recParams.videoWidth, recParams.videoHeight);
						crpView.drawCenter(true);
					}
					else
					{
						crpView.drawCenter(false);
					}
				}
			});
			
			//Other Presets
			if(hasPst){
			setRes(pst.vidRes);
			setFps(pst.vidFPS);
			setKelvin(pst.kelvin);
			fcsMode(pst.focus);
			recParams.videoBitRate = pst.vidBit;
			recParams.audioBitRate = pst.audRate;
			recParams.audioSampleRate = pst.audSam;
			recParams.videoFPS = pst.fpsProject;
			//Set Text
			((EditText)findViewById(R.id.editABit)).setText(String.valueOf(recParams.audioBitRate));
			((EditText)findViewById(R.id.editSample)).setText(String.valueOf(recParams.audioSampleRate));
			((EditText)findViewById(R.id.editVBit)).setText(String.valueOf(recParams.videoBitRate));
			((EditText)findViewById(R.id.editFPS)).setText(String.valueOf(recParams.videoFPS));
			
			}
			else
			{
				pst.vidFPS = fps_i;
				pst.vidRes = res_i;
				pst.kelvin = kel_i;
				pst.focus = fcs_i;
			}
		
			
		//Entry AD Section
		AndroidSDKProvider.initSDK(this);
		AdPreferences adPreferences =
			new AdPreferences("104735826", "204655453", AdPreferences.TYPE_INAPP_EXIT);
		htmlAd = new HtmlAd(this); htmlAd.load(adPreferences, null);
	}
	
	protected void onStop()
	{
		//Save Preset
		savePreset();
		//End With Ad
		
		super.onStop();
		
		//End APP. Save Instance State before finish!
		this.finish();
	}
	
	protected void onDestroy()
	{
		
		if(aud != null)
		{
			aud.stopPreview();
			while(aud.stillRunning()){} //block
			aud = null;
		}

		android.os.Debug.stopMethodTracing();
		super.onDestroy();
	}
	
	//Save Instance State
	/*protected void onSavedInstanceState(Bundle in)
	{
		
	}*/
	//**GUI Events**
	
	public boolean getPreset()
	{
		try{
			if(new File(myContext.getCacheDir() + "/settings").exists()){
			FileInputStream pstis = new FileInputStream(myContext.getCacheDir() + "/settings");
			DataInputStream is = new DataInputStream(pstis);
			pst.audioprv  = is.readBoolean();
			pst.audRate = is.readInt();
			pst.audSam = is.readInt();
			pst.con = is.readInt();
			pst.sat = is.readInt();
			pst.shrp = is.readInt();
			pst.crop = is.readInt();
			pst.crop43 = is.readBoolean();
			pst.focus = is.readInt();
			pst.kelvin = is.readInt();
			pst.vidFPS = is.readInt();
			pst.vidRes = is.readInt();
			pst.vidBit = is.readInt();
			pst.fpsProject = is.readInt();
			pst.iso = is.readInt();
			pst.stab = is.readBoolean();
			is.close();
			pstis.close();
			return true;
			}
			else
			{
				return false;
			}
		}
		catch(IOException e)
		{
			return false;
		}
	}
	public void savePreset()
	{
		//Save GUI Presets
		pst.audioprv = ((CheckBox)findViewById(R.id.chkPreAudio)).isChecked();
		pst.audRate = Integer.valueOf(((EditText)findViewById(R.id.editABit)).getText().toString());
		pst.audSam = Integer.valueOf(((EditText)findViewById(R.id.editSample)).getText().toString());
		pst.vidBit = Integer.valueOf(((EditText)findViewById(R.id.editVBit)).getText().toString());
		pst.iso = ((Spinner)findViewById(R.id.spinISO)).getSelectedItemPosition();
		pst.crop = ((Spinner)findViewById(R.id.spinCrop)).getSelectedItemPosition();
		pst.crop43 = ((CheckBox)findViewById(R.id.chk43box)).isChecked();
		pst.sat = ((SeekBar)findViewById(R.id.skbSat)).getProgress();
		pst.con = ((SeekBar)findViewById(R.id.skbCon)).getProgress();
		pst.shrp = ((SeekBar)findViewById(R.id.skbShr)).getProgress();
		pst.stab = ((CheckBox)findViewById(R.id.chkStable)).isChecked();
		pst.fpsProject = Integer.valueOf(((TextView)findViewById(R.id.editFPS)).getText().toString());
		//Save Preset
		try{
			pstos =  new FileOutputStream(myContext.getCacheDir() + "/settings");
			DataOutputStream os = new DataOutputStream(pstos);
			os.writeBoolean(pst.audioprv);
			os.writeInt(pst.audRate);
			os.writeInt(pst.audSam);
			os.writeInt(pst.con);
			os.writeInt(pst.sat);
			os.writeInt(pst.shrp);
			os.writeInt(pst.crop);
			os.writeBoolean(pst.crop43);
			os.writeInt(pst.focus);
			os.writeInt(pst.kelvin);
			os.writeInt(pst.vidFPS);
			os.writeInt(pst.vidRes);
			os.writeInt(pst.vidBit);
			os.writeInt(pst.fpsProject);
			os.writeInt(pst.iso);
			os.writeBoolean(pst.stab);
			os.flush();
			os.close();
			pstos.flush();
			pstos.close();
		}
		catch(IOException e)
		{
			
		}
	}
	public void recordClick(final View v)
	{
		if(recording)
		{
			
			recordCounter = false;
			meter.stop();
			rec.stop();
			Toast.makeText(myContext, "Captured to output: " + recParams.outFileNme,Toast.LENGTH_LONG).show();
			
			meter.UseRecorder(false);
			aud.stopPreview();
			while(aud.stillRunning()){}
			aud.startPreview();
			
			v.setBackgroundResource(R.drawable.run);
			recording = false;
		}
		else
		{
			STAMP =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			recParams.isAudioAGC = ((CheckBox)findViewById(R.id.chkAGC)).isChecked();
			recParams.outFileNme = outDirectory + "/" + "FOOTAGE_"+STAMP+".mp4";
			recParams.videoBitRate = Integer.valueOf(((TextView)findViewById(R.id.editVBit)).getText().toString()) * 1000000;
			recParams.audioSampleRate = Integer.valueOf(((TextView)findViewById(R.id.editSample)).getText().toString());
			recParams.audioBitRate = Integer.valueOf(((TextView)findViewById(R.id.editABit)).getText().toString()) * 1000;
			recParams.isAudioStereo = ((CheckBox)findViewById(R.id.chkStereo)).isChecked();
			recParams.videoFPS = Integer.valueOf(((TextView)findViewById(R.id.editFPS)).getText().toString());
			rec.setRecordParams(recParams);
			if(rec.initCameraRecorder(cam))
			{
				meter.setExtRecorder(rec.getRecorderObj());
				aud.stopPreview();
				while(aud.stillRunning())
				{}
				meter.UseRecorder(true);
				if(rec.start()){
				meter.stop();
				meter.start();
				recording = true;
				recordCounter = true;
				rec.getRecorderObj().setOnErrorListener(new MediaRecorder.OnErrorListener(){
					public void onError(MediaRecorder r, int a, int b)
					{
						recording = false;
						recordCounter = false;
						meter.stop();
						meter.UseRecorder(false);
						aud.stopPreview();
						while(aud.stillRunning())
						{}
						aud.startPreview();
						Toast.makeText(myContext, "Recording failed. Try adjusting output settings.", Toast.LENGTH_SHORT).show();
						v.setBackgroundResource(R.drawable.run);
					}
				});
				//Set Rack Focus Run
				if(((CheckBox)findViewById(R.id.chkfcs)).isChecked())
				{
					rfToOne = false;
					new RackFocusTask().execute();
				}
					new FrameCounter().start();
				}
				else
				{
					Toast.makeText(myContext, "Recorder Failed to Start, Try Adjusting Output Settings.", Toast.LENGTH_LONG).show();
				}
			}
			
			v.setBackgroundResource(R.drawable.run_pressed);
			
		}
	}
	
	public void frmCntClick(View v)
	{
		if(tc.isTC){
			tc.isTC=false;
			tc.counter = String.format("%02d:%02d:%02d.%02d",tc.hours, tc.mins,tc.secs,tc.frms);
		}
		else{
			tc.isTC = true;
			tc.counter = String.valueOf(tc.absFrms)+"@"+String.valueOf(cam.currentFrameRate());
		}
		((TextView)findViewById(R.id.txtFrms)).setText(tc.counter);
	}
	
	public class FrameCounter extends Thread
	{
		public void run()
		{
			int fps = recParams.videoFPS;
			tc.absFrms = 0;
			while(recordCounter)
			{
				//Govern frame rate counter
					try{
						Thread.sleep(1000/fps);
						tc.absFrms++;
					}
					catch(Exception e){}
				//Calculate TC based on absolute frames
				//Seconds
				tc.frms = tc.absFrms % fps;
				tc.secs = tc.absFrms / fps % 60;
				tc.mins = tc.absFrms / (60*fps) % 60;
				tc.hours = tc.absFrms / (3600 * fps);
				//Format
				if(tc.isTC)
					tc.counter = String.format("%02d:%02d:%02d.%02d",tc.hours, tc.mins,tc.secs,tc.frms);
				else
					tc.counter = String.valueOf(tc.absFrms)+"@"+String.valueOf(fps);
					
				showProgress(tc.counter);
				
			}
			
			return;
		}
		
		protected void showProgress(final String count)
		{
			uiActivity.runOnUiThread(new Runnable(){
			
			public void run(){
			 ((TextView)findViewById(R.id.txtFrms)).setText(count);
			}
			});
		}
		
	}
	
	public void setKelvin(int i)
	{
		kel_i = i;
		View v = findViewById(R.id.btnKelvin);
		if(kel_i < kelvinList.size())
		{
			((TextView)v).setText(kelvinList.get(kel_i) + "K");
			cam.setWB(kelvinList.get(kel_i));
			//Set Color
			if(kelvinList.get(kel_i) == "AUTO")
			{	
				((TextView)v).setText("AUTO");
				((TextView)v).setTextColor(Color.WHITE);
			}
			else if(kelvinList.get(kel_i) == "LOCK")
			{
				((TextView)v).setText("LOCK");
				((TextView)v).setTextColor(Color.RED);
			}
			else if(kelvinList.get(kel_i) == "3200")
			{
				((TextView)v).setTextColor(Color.rgb(255,155,15));
			}
			else if(kelvinList.get(kel_i) == "4000")
			{
				((TextView)v).setTextColor(Color.parseColor("#c4ffe5"));
			}
			else if(kelvinList.get(kel_i) == "5600")
			{
				((TextView)v).setTextColor(Color.parseColor("#00c2ff"));
			}
			else if(kelvinList.get(kel_i) == "8000")
			{
				((TextView)v).setTextColor(Color.parseColor("#0013ff"));
			}
		}
	}
	
	public void kelvinClick(View v)
	{
			pst.kelvin = kel_i;
			setKelvin(kel_i);
			kel_i++;
			if(kel_i == kelvinList.size()) kel_i = 0;
	}
	
	public void lightClick(View v)
	{
		
		if(!lightToggle)
		{
			v.setBackgroundResource(R.drawable.bulb_on);
			cam.setLight(true);
			lightToggle = true;
		}
		else
		{
			v.setBackgroundResource(R.drawable.bulb_off);
			cam.setLight(false);
			lightToggle=false;
		}
	
	}
	
	public void setFps(int i)
	{
		fps_i = i;
		if(fps_i < cam.getFPSList().size())
		{
			cam.setFPS(cam.getFPSList().get(fps_i));
			((TextView)findViewById(R.id.btnFPS)).setText(
				String.valueOf(cam.getFPSList().get(fps_i)) + "FPS");
		}
	}
	
	public void fpsClick(View v)
	{
		pst.vidFPS = fps_i;
		setFps(fps_i);
		fps_i++;
		if(fps_i == cam.getFPSList().size()) fps_i =0;
	}
	
	public void setRes(int i)
	{
		int r = i;
		if(r < resList.size())
		{
			if(resList.get(r).contains("HD1080"))
			{
				btnRes.setText(resList.get(r));
				recParams.videoWidth=1920;
				recParams.videoHeight=1080;
	
			}
			else if(resList.get(r).contains("HDOVER"))
			{
				btnRes.setText("HD1080");
				recParams.videoWidth=1920;
				recParams.videoHeight=1088;
			}
			else if(resList.get(r).contains("HD720"))
			{
				btnRes.setText(resList.get(r));
				recParams.videoWidth=1280;
				recParams.videoHeight=720;
			}
			else if(resList.get(r).contains("SD480"))
			{
				btnRes.setText(resList.get(r));
				recParams.videoWidth=720;
				recParams.videoHeight=480;
			}
			else if(resList.get(r).contains("SD576"))
			{
				btnRes.setText(resList.get(r));
				recParams.videoWidth=720;
				recParams.videoHeight=576;
			}
			
		}
	}
	public void resClick(View v)
	{
		pst.vidRes = res_i;
		setRes(res_i);
		res_i++;
		if(res_i == resList.size()) res_i = 0;	
	}
	
	public void expLock(View v)
	{
		if(expLCK)
		{
			es.selected(false);
			cam.setAELock(false);
			expLCK = false;
		}
		else
		{
			if(cam.setAELock(true))
			{
				es.selected(true);
				expLCK = true;
			}
		}
	}
	
	public void fcsMode(int i)
	{
		int f = i;
		if(f < fcsList.size()){
		if(fcsList.get(f).contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
		{
			isAFOn = true;
			fs.setVisibility(View.INVISIBLE);
			cam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			cam.setFocusArea(new Rect(0,0,myDisplay.right,myDisplay.bottom));
			((TextView)findViewById(R.id.btnFCS)).setText("    AF  ");
		}
		else if(fcsList.get(f).contains(Camera.Parameters.FOCUS_MODE_AUTO))
		{
			isAFOn = false;
			fs.setVisibility(View.VISIBLE);
			cam.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			((TextView)findViewById(R.id.btnFCS)).setText("    TF  ");
		}
		else if(fcsList.get(f).contains(Camera.Parameters.FOCUS_MODE_MACRO))
		{
			isAFOn = false;
			fs.setVisibility(View.VISIBLE);
			cam.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
			((TextView)findViewById(R.id.btnFCS)).setText("MACRO");
		}
		else if(fcsList.get(f).contains(Camera.Parameters.FOCUS_MODE_INFINITY))
		{
			isAFOn = true;
			fs.setVisibility(View.INVISIBLE);
			cam.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			((TextView)findViewById(R.id.btnFCS)).setText("    \u221E  ");
		}
		else if(fcsList.get(f).contains(Camera.Parameters.FOCUS_MODE_FIXED))
		{
			isAFOn = true;
			fs.setVisibility(View.INVISIBLE);
			cam.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
			((TextView)findViewById(R.id.btnFCS)).setText("    LF  ");
		}}
	}
	
	public void focusModeClick(View v)
	{
		pst.focus =  fcs_i;
		fcsMode(fcs_i);	
		fcs_i++;
		if(fcs_i == fcsList.size()) fcs_i = 0;
	}
	
	
	//Key Events for Zoom
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		if(recording)
		{
        switch (keyCode) {
			
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == KeyEvent.ACTION_DOWN) {
					if(zoomFactor <= cam.getMaxZoom())
					{
						cam.setZoomRatio(zoomFactor);
						((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
						zoomFactor++;
					}
					else
					{
						zoomFactor = cam.getMaxZoom();
						cam.setZoomRatio(zoomFactor);
						((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
					}
					
					Log.v(TAG + "Current Zoom Factor",String.valueOf(zoomFactor));
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_DOWN) {
					if(zoomFactor > 0)
					{
						cam.setZoomRatio(zoomFactor);
						((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
						zoomFactor--;
					}
					else
					{
						zoomFactor = 0;
						cam.setZoomRatio(zoomFactor);
						((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
					}

					Log.v(TAG + "Current Zoom Factor",String.valueOf(zoomFactor));
				}
				return true;
			default:
				return super.dispatchKeyEvent(event);
        }
		}
		else
		{
			switch(keyCode){
				case KeyEvent.KEYCODE_MENU:
				if(action == KeyEvent.ACTION_UP){
					if(findViewById(R.id.menuScroll).getVisibility() == View.VISIBLE)
					{
						findViewById(R.id.menuScroll).setVisibility(View.INVISIBLE);
					}
					else
					{
						findViewById(R.id.menuScroll).setVisibility(View.VISIBLE);
					}
					break;
				}
				break;
				case KeyEvent.KEYCODE_BACK:
				if(action == KeyEvent.ACTION_UP && event.getRepeatCount() == 0)
				{
					if(findViewById(R.id.menuScroll).getVisibility() == View.VISIBLE)
					{
						findViewById(R.id.menuScroll).setVisibility(View.INVISIBLE);
						return false;
					}
					else
					{
						if(htmlAd != null) 
						{
							htmlAd.show();
							Toast.makeText(myContext, "Exiting...", Toast.LENGTH_SHORT).show();
							super.onBackPressed();
						}
						return false;
					}
				}
				/*else
				{
					
					this.finish();
					return true;
				}*/
			}
		}
		return super.dispatchKeyEvent(event);
    }
	
	//Touch Events for focus and exposure
	
	public boolean onTouchEvent(MotionEvent touch)
	{
		
		//Set Focus Point
		if(!isAFOn && !expSet)
		{
			fs.setX(touch.getX() - (fs.getWidth() / 2));
			fs.setY(touch.getY() - (fs.getHeight() /2));

			//Clamp Touch
				if(fs.getX() <= 0) fs.setX(0);
				if(fs.getX() >= myDisplay.right - 125) fs.setX(myDisplay.right - (fs.getWidth() + 5));
				if(fs.getY() <= 0) fs.setY(0);
				if(fs.getY() >= myDisplay.bottom - 125) fs.setY(myDisplay.bottom - (fs.getHeight() + 5));
		
			 Rect r = new Rect((int)fs.getX(), (int)fs.getY(), (int)fs.getX() + fs.getWidth(), (int)fs.getY() + fs.getHeight());
		
			if(inRackFocus)
			{	
			    cam.setFocusArea(r);
				cam.focus();
				switch(touch.getAction())
				{
					case touch.ACTION_UP:
					{	
						//Hold For Focus
						if(!rackAreas.isEmpty())
							rackAreas.add(1, r);
						else
						{
								msg.cancel();
								msg = Toast.makeText(myContext, "Please Reset Rack Focus Again", Toast.LENGTH_LONG);
								msg.show();
								inRackFocus = false;
								return true;
						}
						//Set Delay On Run Dialog
						msg.cancel();
						msg = Toast.makeText(myContext, "Second Focal Point Set. Going Back To First", Toast.LENGTH_LONG);
						msg.show();
						inRackFocus = false;
						rfToOne = true;
						rackDelay = (int)(Float.valueOf(((EditText)findViewById(R.id.editRack)).getText().toString()) * 1000);
						new RackFocusTask().execute();

						Log.v(TAG + " Rack Focus Areas: ", rackAreas.toString());					
						
						break;
						
					}
				}
				
			}
			else
			{
				cam.setFocusArea(r);
				cam.focus();
			}
				
			
		}
		
		
		if(expSet)
		{
			//Set Exposure Area
			es.setX(touch.getX() - (es.getWidth() / 2));
		    es.setY(touch.getY() - (es.getHeight() /2));
	
			//Clamp Touch
			if(es.getX() <= 0) es.setX(0);
			if(es.getX() >= myDisplay.right - 125) es.setX(myDisplay.right - (es.getWidth() + 5));
			if(es.getY() <= 0) es.setY(0);
			if(es.getY() >= myDisplay.bottom - 125) es.setY(myDisplay.bottom - (es.getHeight() + 5));

			Rect r = new Rect((int)es.getX(), (int)es.getY(), (int)es.getX() + es.getWidth(), (int)es.getY() + es.getHeight());

			cam.setExpArea(r);
			switch (touch.getAction())
			{
				case touch.ACTION_UP:
					msg.cancel();
					msg = Toast.makeText(myContext, "Exposure Area Set", Toast.LENGTH_SHORT);
					msg.show();
					expSet = false;
					break;
			}
		}
		return true;
	}
	
	private class RackFocusTask extends AsyncTask<Void, Void, Void>{
		
		public Void doInBackground(Void... v)
		{
			try{
				Thread.sleep(rackDelay);
			}
			catch(Exception e)
			{}
			
			return null;
		}
		
		public void onPostExecute(Void v)
		{
			int i = 0;
			
			if(!rfToOne)
			{
				i = 1;
			}
			if(rackAreas.size() > 0){
				fs.setX(rackAreas.get(i).left);
				fs.setY(rackAreas.get(i).top);
				cam.setFocusArea(rackAreas.get(i));
				cam.focus();
			}
			else
				Toast.makeText(myContext, "Please set rack focus areas first. Long click on focus bracket.", Toast.LENGTH_LONG).show();
		}
	}
	
	//onClick focus
	public void focusClick(View v)
	{	
		cam.focus();		
	}
	
	public void focusLongClick(View v)
	{
		
		//Show Rack Focus Dialog
		if(!runRackFocus)
		{
			rackAreas.add(0, new Rect((int)fs.getX(), (int)fs.getY(), (int)fs.getX() + fs.getWidth(), (int)fs.getY() + fs.getHeight()));
			runRackFocus = true;
			inRackFocus = true;
			msg = Toast.makeText(myContext, "Set Second Rack Focal Point", Toast.LENGTH_LONG);
			msg.show();
		}
		else
		{
			rackAreas.clear();
			msg = Toast.makeText(myContext, "Previous Rack Focus Settings Are Reset", Toast.LENGTH_SHORT);
			msg.show();
			runRackFocus = false;
		
		}
	}
	
	public void expLongClick(View v)
	{
		//Show Toast to set Exposure position
		//1 Click: Set Area | 2 Click: Reset Area to Whole Screen
		if(cam.hasExpArea())
		{	
			if(!resetExp)
			{
				expSet = true;
				msg = Toast.makeText(myContext, "Tap Area for Exposure Control", Toast.LENGTH_SHORT);
				msg.show();
				resetExp = true;
			
			}
			else
			{
				msg = Toast.makeText(myContext, "Exposure has been reset to normal metering", Toast.LENGTH_LONG);
				msg.show();
				cam.setExpArea(new Rect(0,0,myDisplay.right, myDisplay.bottom));
				resetExp = false;
			}
			
		}
		else
			msg = Toast.makeText(myContext, "Your Device Doesn't Support Custom Exposure Areas", Toast.LENGTH_LONG);
			msg.show();
		}
		
		public void expCompClick(View v)
		{
			if(expCompLevel <= cam.getMaxExpComp())
			{
			
				cam.setExpComp(expCompLevel);
				((TextView)findViewById(R.id.btnEXP)).setText(String.format("%.1f+/-",(expCompLevel * cam.getExpCompStep())));
				expCompLevel++;
			}
			else
			{
				expCompLevel = cam.getMinExpComp();
				cam.setExpComp(expCompLevel);
				((TextView)findViewById(R.id.btnEXP)).setText(String.format("%.1f+/-",(expCompLevel * cam.getExpCompStep())));
				expCompLevel++;
			}
			
		}
		
	public void runLongClick(View v)
	{
		//Show Dialog for Run Button
	}
	
	public void zoomClick(View v)
	{
		if(zoomFactor <= cam.getMaxZoom())
		{
			cam.setZoomRatio(zoomFactor);
			((TextView)findViewById(R.id.btnZoom)).setText(String.format("Z%.2f",(float)zoomFactorList.get(zoomFactor) / 100));
			zoomFactor++;
		}
		else
		{
			zoomFactor = 0;
			cam.setZoomRatio(0);
			zoomFactor++;
		}
	}
	
	public void settingsClick(View v)
	{
		if(findViewById(R.id.menuScroll).getVisibility() == View.INVISIBLE)
		{
			findViewById(R.id.menuScroll).setVisibility(View.VISIBLE);
		}
		else
			findViewById(R.id.menuScroll).setVisibility(View.INVISIBLE);
	}
	
	public void aboutClick(View v)
	{
		dialog.show();
	}

}
