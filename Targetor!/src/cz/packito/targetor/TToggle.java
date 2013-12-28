package cz.packito.targetor;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public abstract class TToggle extends TButton{

	private boolean toggled;

	
	public TToggle(boolean toggled, int sWidth, int sHeight,Bitmap bmp, Bitmap bmpPressed, float size,
			float hpos, float vpos){
		super(sWidth, sHeight, bmp, bmpPressed, size, hpos, vpos);
		this.toggled=toggled;
	}
	
	@Override
	public void drawOn(Canvas canvas) {
		canvas.drawBitmap(toggled?bmpPressed:bmp, srcRect, dstRect, null);
	}
	
	/**
	 * DO NOT CALL, INNER PURPOSES ONLY
	 */
	@Override
	public void onClick() {
		toggled=!toggled;
		onToggle(toggled);
	}
	
	public abstract void onToggle(boolean newValue);
}
