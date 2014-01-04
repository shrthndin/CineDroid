package in.shrthnd.cinedroid;

//Misc Functions
import android.hardware.Camera;
import android.view.*;
import android.graphics.*;
import android.util.*;
public class CineUtils
{

	/*static{
		System.loadLibrary("cineutils");
	}
	
	public static native int makeFIFO(String name, int mode);
	*/
	public static Rect viewToCameraArea(Rect v)
	{
		//Converter 0,0 x w,h to -1000,-1000 x 1000,1000
		Rect rect = new Rect();
		
		rect.left = v.left * 2000 /CineMain.myDisplay.right  - 1000;
		rect.top = v.top * 2000/CineMain.myDisplay.bottom - 1000;
		rect.right = v.right * 2000/CineMain.myDisplay.right -1000;
		rect.bottom = v.bottom * 2000/CineMain.myDisplay.bottom -1000;
		Log.v(CineMain.TAG + " Camera.Area Conversion: ", rect.toString());
		return rect;
	}
	
	public static int getDpiPixel(int px)
	{
		return (int)(px / CineMain.myContext.getResources().getDisplayMetrics().density);
	}
	
	public static int YUVFrameByteSize(int wid, int hei)
	{
		int yStride  = (int) Math.ceil(wid / 16.0) * 16;
		int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
		int ySize  = yStride * hei;
		int uvSize = uvStride * hei / 2;
		return ySize + uvSize * 2;
	}
}
