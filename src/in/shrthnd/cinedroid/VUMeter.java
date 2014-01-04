package in.shrthnd.cinedroid;

import android.graphics.*;
import android.widget.*;
import android.content.*;
import android.util.*;
import android.media.*;
import java.math.*;
import java.util.*;

public class VUMeter extends ImageView
{
	private Paint greenDB = new Paint();
	private Paint yellowDB = new Paint();
	private Paint orangeDB = new Paint();
	private Paint redDB = new Paint();
	private Paint offDB = new Paint();
	private String[] dbList = new String[]{"60", "48" ,"24", "12", "6" ,"3", "0dB"};
	private MediaRecorder intRec, tmpRec;
	private boolean startMeter, fromRec, rms;
	private int myDB;
	private double val, mVal;
	
	public VUMeter(Context c, AttributeSet attr)
	{
		super(c,attr);

		greenDB.setColor(Color.GREEN);
		greenDB.setTextSize(20);
		yellowDB.setColor(Color.YELLOW);
		yellowDB.setTextSize(20);
		orangeDB.setColor(Color.rgb(255,135,0));
		orangeDB.setTextSize(20);
		redDB.setColor(Color.RED);
		redDB.setTextSize(20);
		offDB.setColor(Color.DKGRAY);
		offDB.setTextSize(20);
		setMinimumWidth(230);
		setMinimumHeight(32);
		/*intRec = new MediaRecorder();
		intRec.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
		intRec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		intRec.setOutputFile("/dev/null");
		tmpRec = intRec;
		//intRec.prepare();*/
		
	}
	
	
	private double calcDB()
	{
	
		double mVal = tmpRec.getMaxAmplitude();
		return 20 * Math.log(mVal / 2700.0);
			
	}
	
	private double calcDBfromValue(double v)
	{

			return 20 * Math.log(v / 2700.0);

	}
	
	
	public void UseRecorder(boolean v)
	{
		if(v)
		{
			fromRec = true;
		}
		else
		{
			fromRec = false;
		}
	}
	
	public void setMeasure(double v)
	{
		val = v;
	}
	
	public void setExtRecorder(MediaRecorder ext)
	{
		tmpRec = ext;
		
	}
	
	public void start()
	{
		
			startMeter = true;
	}
	
	public void stop()
	{
		startMeter = false;
	}
	
	public void onDraw(Canvas c)
	{
		//myDB = calcDB();
		int xItr = 32;
		int startX = 0;
		
		//Draw Off Main DB Line
		for(int i = 0; i < dbList.length; i++)
		{
			c.drawText(dbList[i], startX + (xItr * i), 20, offDB);
		}
		
		if(startMeter)
		{
			if(fromRec)
			{	
				myDB = (int)Math.abs(calcDB());
			}
			else
			{
				myDB = (int)Math.abs(calcDBfromValue(val));
			}
			if(myDB >= Integer.MAX_VALUE) myDB = 60; //Normalize?
			
			//Log.v("Audio LEVEL ", String.valueOf(myDB));
			if(myDB > 60 || myDB <= 60 && myDB < 48)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
			}
			if(myDB <= 48 && myDB > 24)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
			}
			if(myDB <= 24 && myDB > 12)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
				c.drawText(dbList[2], startX + (xItr * 2), 20, greenDB);
			}
			if(myDB <= 12 && myDB > 6)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
				c.drawText(dbList[2], startX + (xItr * 2), 20, greenDB);
				c.drawText(dbList[3], startX + (xItr * 3), 20, greenDB);
				
			}
			if(myDB <= 6 && myDB > 3)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
				c.drawText(dbList[2], startX + (xItr * 2), 20, greenDB);
				c.drawText(dbList[3], startX + (xItr * 3), 20, greenDB);
				c.drawText(dbList[4], startX + (xItr * 4), 20, yellowDB);
			}
			if(myDB <= 3 && myDB > 0)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
				c.drawText(dbList[2], startX + (xItr * 2), 20, greenDB);
				c.drawText(dbList[3], startX + (xItr * 3), 20, greenDB);
				c.drawText(dbList[4], startX + (xItr * 4), 20, yellowDB);
				c.drawText(dbList[5], startX + (xItr * 5), 20, orangeDB);
			}
			if(myDB == 0)
			{
				c.drawText(dbList[0], startX + (xItr * 0), 20, greenDB);	
				c.drawText(dbList[1], startX + (xItr * 1), 20, greenDB);
				c.drawText(dbList[2], startX + (xItr * 2), 20, greenDB);
				c.drawText(dbList[3], startX + (xItr * 3), 20, greenDB);
				c.drawText(dbList[4], startX + (xItr * 4), 20, yellowDB);
				c.drawText(dbList[5], startX + (xItr * 5), 20, orangeDB);
				c.drawText(dbList[6], startX + (xItr * 6), 20, redDB);
			}
			
		}
		invalidate();
	}

}
