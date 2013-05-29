package cz.packito.targetor;

import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

/**
 * Class that represents a target on the game screen
 * @author packito
 *
 */
public class Target {

	// for adding other target types
	public static final int TYPE_NORMAL = 1;

	private final GameView gameView;
	private final Bitmap bmp, tempBmp;
	private final int SOUND_ID;
	private final Rect srcRect;
	/** points gained by shooting this target */
	private final int value;
	/** radius */
	private final float r;
	/** target id */
	public final int id;
	/** normalised coordinates from range (0,MX) resp. (0,MY) */
	private float x, y;
	/** velocity */
	private float v;
	/** direction in radians */
	private double d;

	/**
	 * create a target at the given coordinates with supplied id. Used when
	 * receiving NEW_TARGET from opponent via bluetooth
	 * 
	 * @param gameView
	 * @param type
	 * @param id
	 * @param x
	 * @param y
	 * @param v
	 * @param d
	 */

	public Target(GameView gameView, int type, int id, float x, float y,
			float v, double d) {
		this.gameView = gameView;
		this.x = x;
		this.y = y;
		this.v = v;
		this.d = d;
		this.id = id;

		switch (type) {
		case TYPE_NORMAL:
			bmp = gameView.BMP_TARGET_NORMAL;
			tempBmp = gameView.BMP_TARGET_NORMAL_TEMP;
			srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			value = 10;
			SOUND_ID = gameView.SOUND_TARGET_NORMAL;
			r = 0.1f;
			break;
		default:
			bmp = gameView.BMP_TARGET_NORMAL;
			tempBmp = gameView.BMP_TARGET_NORMAL_TEMP;
			srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			value = 10;
			SOUND_ID = gameView.SOUND_TARGET_NORMAL;
			r = 0.1f;
			Log.d("Target", "unknown target type: " + type);
			break;
		}

	}

	/**
	 * Create a target at random coordinates with random id. Used when locally
	 * creating a new target.
	 * 
	 * @param gameView
	 * @param type
	 */
	public Target(GameView gameView, int type) {
		this(gameView, type,
				gameView.idGenerator += (int) (Math.random() * 1000000) + 1, 0,
				0, 0, 0);

		// change starting position to random
		Random rnd = new Random();
		v = rnd.nextFloat() * 0.015f + 0.008f;

		float start = rnd.nextFloat() * 2 * (gameView.MX + gameView.MY);

		if (start < gameView.MX) {// top
			x = start;
			y = -r;
			d = rnd.nextDouble() * Math.PI / 2 + Math.PI / 4;
		} else if (start < gameView.MX + gameView.MY) {// left
			x = -r;
			y = start - gameView.MX;
			d = rnd.nextDouble() * Math.PI / 2 - Math.PI / 4;
		} else if (start < 2 * gameView.MX + gameView.MY) {// bottom
			x = start - gameView.MX - gameView.MY;
			y = gameView.MY + r;
			d = rnd.nextDouble() * Math.PI / 2 + Math.PI * 5 / 4;
		} else {// right
			x = gameView.MX + r;
			y = start - 2 * gameView.MX - gameView.MY;
			d = rnd.nextDouble() * Math.PI / 2 + Math.PI * 3 / 4;
		}

		if (gameView.activity.multiplayer) {
			// TODO multiplayer
			// gameView.activity.sendNewTarget(type, id, x, y, v, d);
		}
	}

	/** Move the target */
	public void update() {
		x += Math.cos(d) * v;
		y += Math.sin(d) * v;
	}

	/** update the position and draw to canvas */
	public void draw(Canvas canvas) {
		update();
		canvas.drawBitmap(bmp, srcRect, dstRect(), null);
	}

	/**
	 * returns physical coordinates of the target. Used when creating a
	 * {@linkplain TempTarget}
	 */
	private Rect dstRect() {
		int left = (int) ((x - r) * gameView.getWidth());
		int top = (int) ((y - r) * gameView.getWidth());
		int right = (int) ((x + r) * gameView.getWidth());
		int bottom = (int) ((y + r) * gameView.getWidth());
		return new Rect(left, top, right, bottom);
	}

	/**
	 * check for collision
	 * 
	 * @param normX
	 *            normalised x coordinate
	 * @param normY
	 *            normalised y coordinate
	 * @return true if [normX,normY] is inside the target, false otherwise
	 */
	public boolean isCollision(float normX, float normY) {
		return Math.sqrt((x - normX) * (x - normX) + (y - normY) * (y - normY)) <= r;
	}

	/**
	 * handles the shooting(removing,adding points) of this target by local
	 * player
	 */
	public void shoot() {
		gameView.score += value;
		gameView.playSound(SOUND_ID);
		gameView.targets.remove(this);
		TempTarget temp = new TempTarget(gameView, dstRect(), tempBmp);
		gameView.temps.add(temp);
		// TODO multiplayee
		// gameView.activity.sendTargetShot(id);
	}

	/**
	 * handles the shooting(removing,adding points) of this target by opponent
	 */
	public void shootOpponent() {
		gameView.scoreOpponent += value;
		gameView.targets.remove(this);
		TempTarget temp = new TempTarget(gameView, dstRect(), tempBmp);
		gameView.temps.add(temp);
	}
}
