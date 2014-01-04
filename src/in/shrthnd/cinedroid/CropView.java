package in.shrthnd.cinedroid;

import android.widget.*;
import android.graphics.*;
import android.content.*;
import android.util.*;

public class CropView extends ImageView
{
	private Paint crpColor;
	private Paint crp43;
	private int vid_h, vid_w, c_h, c_b, o_h, c_o;
	private boolean drawcentercrop, draw; //4:3 box
	public CropView(Context c, AttributeSet attr)
	{
		super(c, attr);
		crpColor = new Paint();
		crpColor.setStyle(Paint.Style.STROKE);
		crpColor.setStrokeWidth(3f);
		crpColor.setColor(Color.argb(185,255,0,0));
		crp43 = new Paint();
		crp43.setStyle(Paint.Style.STROKE);
		crp43.setStrokeWidth(3f);
		crp43.setColor(Color.argb(150, 255,255,255));
		setMinimumWidth(CineMain.myDisplay.right);
		setMinimumHeight(CineMain.myDisplay.bottom);
		
	}
	
	public void setCrop(float ratio)
	{
		o_h = (int)(vid_w / ratio);
		c_h = (int)((float)o_h / (float)vid_w * (float)CineMain.myDisplay.right);
		c_o = (CineMain.myDisplay.bottom - c_h)/2;
	}
	
	public void drawCrop(boolean v)
	{
		draw = v;
	}
	
	public void drawCenter(boolean v)
	{
		drawcentercrop = v;
	}
	
	public void setSize(int w, int h)
	{
		vid_h = h;
		vid_w = w;
	}
	
	public void onDraw(Canvas c)
	{
		if(draw)
		{
			c.drawLine(0, c_o, this.getWidth(), c_o, crpColor);
			c.drawLine(0, c_h+c_o, this.getWidth(), c_h+c_o, crpColor);
		}
		if(drawcentercrop)
		{
			//Draw 4:3 square to display's height
			int c_h = (int)(vid_h * 1.33);
			int crpWidth = (int)((float)c_h / (float)vid_h * (float)CineMain.myDisplay.bottom);
			int crpO = (CineMain.myDisplay.right - crpWidth)/2;
			c.drawLine((CineMain.myDisplay.right - crpWidth)/2, 0, (CineMain.myDisplay.right - crpWidth)/2, CineMain.myDisplay.bottom, crp43);
			c.drawLine(crpWidth+crpO, 0, crpWidth+crpO, CineMain.myDisplay.bottom, crp43);
		}
		invalidate();
		
	}
}
