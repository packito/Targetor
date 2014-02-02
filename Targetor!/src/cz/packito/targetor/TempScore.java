package cz.packito.targetor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;

public class TempScore {

	private final TargetorView gameView;
	private final String scoreString;
	private final Paint fillPaint, strokePaint;

	/** frame of the animation */
	private int frame = 0;

	private float x, y;
	private final float step;

	private static final int MAX_FRAMES = 5;

	/**
	 * 
	 * @param gameView
	 * @param x
	 *            coordinates at the center of the text.
	 * @param y
	 *            see x.
	 * @param score
	 *            The score to be drawn
	 */
	public TempScore(TargetorView gameView, float x, float y, int score) {
		this.gameView = gameView;
		this.x = x;
		this.y = y;
		scoreString = String.format("%+d", score);

		step=gameView.getHeight()/150.0f;
		
		fillPaint = new Paint();
		fillPaint.setTypeface(TargetorActivity.getTypeface());
		fillPaint.setTextSize(gameView.getWidth() / 16.0f);
		fillPaint.setColor(score < 0 ? Color.RED : Color.BLUE);
		
		strokePaint=new Paint(fillPaint);
		strokePaint.setStyle(Style.STROKE);
		strokePaint.setStrokeWidth(gameView.getWidth()/500.0f);
		strokePaint.setColor(Color.GRAY);

	}

	/** draw to the canvas, handles animation and removing */
	public void draw(Canvas canvas) {
		if (frame >= MAX_FRAMES) {
			gameView.tempScores.remove(this);
			return;
		}
		y-=step;

		canvas.drawText(scoreString, x, y, fillPaint);
		canvas.drawText(scoreString, x, y, strokePaint);
		frame++;
	}
}
