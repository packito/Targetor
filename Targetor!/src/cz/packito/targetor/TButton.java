package cz.packito.targetor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class TButton {

	//private float left, right, top, bottom;
	protected final Bitmap bmp;
	protected final Bitmap bmpPressed;
	protected final Rect srcRect;
	protected final Rect dstRect;
	private boolean pressed = false;

//	private float hpos,vpos, size;
	
	public static final float MIN = 0.0f;
	public static final float CENTER = 0.5f;
	public static final float MAX = 1.0f;

	public static final float SIZE_BUTTON = 0.15f;
	public static final float SIZE_BUTTON_STARTGAME = 0.4f;
	public static final float SIZE_LOGO = 0.6f;

	public TButton(int sWidth, int sHeight, Bitmap bmp, Bitmap bmpPressed, float size,
			float hpos, float vpos) {
		this.bmp=bmp;
		this.bmpPressed=bmpPressed;
		
//		this.hpos=hpos;
//		this.vpos=vpos;
//		this.size=size;
		
		srcRect= new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
		
		float width=size*sWidth;
		float height=width*bmp.getHeight()/bmp.getWidth();
		dstRect=new Rect(Math.round(hpos*(sWidth-width)), Math.round(vpos*(sHeight-height)), Math.round(width+hpos*(sWidth-width)), Math.round(height+vpos*(sHeight-height)));
	}
	
	
	public void drawOn(Canvas canvas) {
		canvas.drawBitmap(pressed?bmpPressed:bmp, srcRect, dstRect, null);
	}
	
	public void press(){
		pressed=true;
	}
	
	public void release(){
		if(pressed){
			pressed=false;
			onClick();
		}
	}
	
	public void cancel() {
		pressed=false;
	}
	
	public abstract void onClick();


	public boolean contains(float x, float y) {
return dstRect.contains(Math.round(x), Math.round(y));
	}


}