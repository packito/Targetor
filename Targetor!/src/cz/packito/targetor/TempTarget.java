package cz.packito.targetor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class TempTarget {

	private TargetorView gameView;
	private final Rect dstRect;
	private final Bitmap bmp;
	private final int bmpHeight;
	private Rect srcRect;
	/** frame of the animation */
	private int frame = 0;

	/**
	 * 
	 * @param gameView
	 * @param dstRect
	 *            physical coordinates
	 * @param tempBmp
	 *            The bitmap. The animation will go by squares left to right
	 */
	public TempTarget(TargetorView gameView, Rect dstRect, Bitmap tempBmp) {
		this.gameView = gameView;
		this.dstRect = dstRect;
		this.bmp = tempBmp;

		bmpHeight = tempBmp.getHeight();
	}

	/** draw to the canvas, handles animation and removing */
	public void draw(Canvas canvas) {
		if (bmpHeight * frame >= bmp.getWidth()) {
			gameView.temps.remove(this);
			return;
		}

		srcRect = new Rect(frame * bmpHeight, 0, frame * bmpHeight + bmpHeight,
				bmpHeight);
		canvas.drawBitmap(bmp, srcRect, dstRect, null);
		frame++;
	}

}
