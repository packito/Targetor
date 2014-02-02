package cz.packito.targetor;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.BlurMaskFilter.Blur;

public abstract class TButton {

	// private float left, right, top, bottom;
	protected final Bitmap bmp;
	protected final Bitmap bmpPressed;
	protected final Rect srcRect;
	protected final Rect dstRect;
	protected String text;

	/**
	 * {@link TButton} can hold some data, for instance integer of the level, or
	 * {@link BluetoothDevice}
	 */
	public final Object data;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	private final int type;
	private final Paint textPaint;
	private final int screenWidth;
	private boolean pressed = false;
	private boolean border;
	private boolean clickable = true;

	public void setClickable(boolean clickable) {
		this.clickable = clickable;
	}

	// button types
	private static final int TYPE_IMAGE = 1;
	private static final int TYPE_TEXT = 2;
	// private float hpos,vpos, size;

	public static final float MIN = 0.0f;
	public static final float CENTER = 0.5f;
	public static final float MAX = 1.0f;

	public static final float SIZE_BUTTON = 0.15f;
	public static final float SIZE_BUTTON_STARTGAME = 0.4f;
	public static final float SIZE_LOGO = 0.6f;

	/**
	 * Constructor for {@link TButton} with image
	 * 
	 * @param sWidth
	 * @param sHeight
	 * @param bmp
	 * @param bmpPressed
	 * @param size
	 * @param hpos
	 * @param vpos
	 */
	public TButton(int sWidth, int sHeight, Bitmap bmp, Bitmap bmpPressed,
			float size, float hpos, float vpos) {
		this(null, sWidth, sHeight, bmp, bmpPressed, size, hpos, vpos);
	}
	public TButton(Object data, int sWidth, int sHeight, Bitmap bmp,
			Bitmap bmpPressed, float size, float hpos, float vpos) {
		this.data = data;
		this.bmp = bmp;
		this.bmpPressed = bmpPressed;
		this.type = TYPE_IMAGE;
		text = null;
		textPaint = null;
		// this.hpos=hpos;
		// this.vpos=vpos;
		// this.size=size;

		srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());

		screenWidth = sWidth;
		float width = size * sWidth;
		float height = width * bmp.getHeight() / bmp.getWidth();
		dstRect = new Rect(Math.round(hpos * (sWidth - width)), Math.round(vpos
				* (sHeight - height)), Math.round(width + hpos
				* (sWidth - width)), Math.round(height + vpos
				* (sHeight - height)));
	}

	/**
	 * constructor for {@link TButton}with text
	 * 
	 * @param sWidth
	 * @param sHeight
	 * @param text
	 * @param hsize
	 * @param vsize
	 * @param hpos
	 * @param vpos
	 */

	public TButton(int sWidth, int sHeight, String text, float hsize,
			float vsize, float hpos, float vpos, boolean border) {
		this(null, sWidth, sHeight, text, hsize, vsize, hpos, vpos, border);
	}
	public TButton(Object data, int sWidth, int sHeight, String text,
			float hsize, float vsize, float hpos, float vpos, boolean border) {
		this.data = data;
		this.bmp = null;
		this.bmpPressed = null;
		this.type = TYPE_TEXT;
		this.text = text;
		this.border = border;

		srcRect = null;
		screenWidth = sWidth;

		textPaint = new Paint();
		textPaint.setTextSize(100);
		textPaint.setTextScaleX(1.0f);
		textPaint.setColor(Color.BLACK);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(TargetorActivity.getTypeface());

		Rect bounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), bounds);

		float width = hsize * sWidth;
		float height = vsize * sHeight;

		// see if text will fill width or height
		float textSize;
		if (width / height >= bounds.width() / (float) bounds.height()) {
			float target = height * 0.8f;
			textSize = (target / bounds.height()) * 100f;
		} else {
			float target = width * 0.8f;
			textSize = (target / bounds.width()) * 100f;
		}
		textPaint.setTextSize(textSize);

		dstRect = new Rect(Math.round(hpos * (sWidth - width)), Math.round(vpos
				* (sHeight - height)), Math.round(width + hpos
				* (sWidth - width)), Math.round(height + vpos
				* (sHeight - height)));
	}


	public void drawOn(Canvas canvas) {
		switch (type) {
		case TYPE_IMAGE:
			if (clickable) { // opaque if clickable
				canvas.drawBitmap(pressed ? bmpPressed : bmp, srcRect, dstRect,
						null);
			} else {
				Paint imgPaint = new Paint();
				imgPaint.setAlpha(50);
				canvas.drawBitmap(pressed ? bmpPressed : bmp, srcRect, dstRect,
						imgPaint);
			}
			break;
		case TYPE_TEXT:
			Paint rectPaint = new Paint();
			rectPaint.setStrokeJoin(Paint.Join.ROUND);
			rectPaint.setStrokeWidth(0.01f * screenWidth);
			rectPaint.setColor(Color.BLACK);
			rectPaint.setStyle(Paint.Style.STROKE);
			if (clickable) {
				textPaint.setAlpha(255);
			} else {
				textPaint.setAlpha(50);
				rectPaint.setAlpha(50);
			}
			Rect bounds = new Rect();
			textPaint.getTextBounds(text, 0, text.length(), bounds);

			if (pressed) {// red glow
				textPaint.setColor(Color.RED);
				rectPaint.setColor(Color.RED);
				textPaint.setMaskFilter(new BlurMaskFilter(
						0.012f * screenWidth, BlurMaskFilter.Blur.NORMAL));
				rectPaint.setMaskFilter(new BlurMaskFilter(
						0.012f * screenWidth, BlurMaskFilter.Blur.NORMAL));

				if (border)
					canvas.drawRoundRect(new RectF(dstRect),
							0.05f * screenWidth, 0.05f * screenWidth, rectPaint);
				canvas.drawText(text, dstRect.exactCenterX(), dstRect.bottom
						- ((dstRect.height() - bounds.height()) / 2), textPaint);

				textPaint.setColor(Color.BLACK);
				rectPaint.setColor(Color.BLACK);
				textPaint.setMaskFilter(null);
				rectPaint.setMaskFilter(null);
			}

			if (border)
				canvas.drawRoundRect(new RectF(dstRect), 0.05f * screenWidth,
						0.05f * screenWidth, rectPaint);
			canvas.drawText(text, dstRect.exactCenterX(), dstRect.bottom
					- ((dstRect.height() - bounds.height()) / 2), textPaint);
			break;
		}
	}

	public void press() {
		if (clickable)
			pressed = true;
	}

	public void release() {
		if (pressed) {
			pressed = false;
			onClick();
		}
	}

	public void cancel() {
		pressed = false;
	}

	public abstract void onClick();

	public boolean contains(float x, float y) {
		return dstRect.contains(Math.round(x), Math.round(y));
	}

}