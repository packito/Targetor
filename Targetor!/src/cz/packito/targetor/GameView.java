package cz.packito.targetor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The View in which all the game rendering is taking place.
 * 
 * @author packito
 * @see SurfaceView
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	public final Typeface TYPEFACE;

	/** points lost when missing a target */
	private static final int SCORE_MISS = -3;
	public final Bitmap BMP_BG, BMP_TARGET_NORMAL, BMP_TARGET_NORMAL_TEMP,
			BMP_TARGET_DIAMOND, BMP_TARGET_DIAMOND_TEMP, BMP_TARGET_FLOWER,
			BMP_TARGET_FLOWER_TEMP;
	public float MX, MY; // normalized screen resolution; MX=1.0,
							// MY=height/width
	public int idGenerator = 0;
	private final Random rnd = new Random();

	private final SurfaceHolder holder;
	public final List<Target> targets = new ArrayList<Target>();
	public final List<TempTarget> temps = new ArrayList<TempTarget>();
	public final List<TempScore> tempScores = new ArrayList<TempScore>();
	private GameThread thread;
	public GameActivity activity;

	private final Paint scoreFillPaint, scoreStrokePaint;

	public int score = 0;
	public int targetsShot = 0;
	public int misses = 0;
	public int scoreOpponent = 0;
	public int targetsShotOpponent = 0;
	public int missesOpponent = 0;
	public long timeleft;// ms

	private boolean soundOn = true;
	public final SoundPool sounds;
	public final MediaPlayer music;
	public final int SOUND_MISS, SOUND_TARGET_NORMAL, SOUND_TARGET_DIAMOND,
			SOUND_TARGET_FLOWER;

	private int targetScore;
	/** probabilities of a target occurence */
	private double pNormal, pDiamond, pFlower;

	/**
	 * The constructor to be called when layout activity_game.xml is inflated.
	 * 
	 * @param context
	 *            the {@linkplain GameActivity}
	 * @param attrs
	 *            attributes from xml file
	 */

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.activity = (GameActivity) context;
		holder = getHolder();
		holder.addCallback(this);

		// load typeface and set paint
		TYPEFACE = Typeface.createFromAsset(activity.getAssets(),
				"zekton__.ttf");
		scoreFillPaint = new Paint();
		scoreFillPaint.setTypeface(TYPEFACE);
		scoreFillPaint.setColor(Color.WHITE);

		scoreStrokePaint = new Paint(scoreFillPaint);
		scoreStrokePaint.setStyle(Paint.Style.STROKE);
		scoreStrokePaint.setColor(Color.BLACK);

		// load the bitmaps
		BMP_BG = BitmapFactory.decodeResource(getResources(),
				R.drawable.bg_game);
		BMP_TARGET_NORMAL = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_normal);
		BMP_TARGET_NORMAL_TEMP = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_normal_temp);
		BMP_TARGET_DIAMOND = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_diamond);
		BMP_TARGET_DIAMOND_TEMP = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_diamond_temp);
		BMP_TARGET_FLOWER = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_flower);
		BMP_TARGET_FLOWER_TEMP = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_flower_temp);

		// load the sounds
		music = MediaPlayer.create(activity, R.raw.music_game);
		music.setLooping(true);
		sounds = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		SOUND_MISS = sounds.load(activity, R.raw.miss, 1);
		SOUND_TARGET_NORMAL = sounds
				.load(activity, R.raw.target_normal_shot, 1);
		SOUND_TARGET_DIAMOND = sounds.load(activity, R.raw.target_diamond_shot,
				1);
		SOUND_TARGET_FLOWER = sounds
				.load(activity, R.raw.target_flower_shot, 1);

		// setup time left and probabilities
		timeleft = TargetorApplication.calcTime(activity.getLevel());
		targetScore = TargetorApplication.calcScore(activity.getLevel());
		pNormal = TargetorApplication.calcNormal(activity.getLevel());
		pDiamond = TargetorApplication.calcDiamond(activity.getLevel());
		pFlower = TargetorApplication.calcFlower(activity.getLevel());
	}

	/**
	 * Handling of the touch events, checking targets for collision and shooting
	 * them
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			boolean miss = true;

			float normX = event.getX() / getWidth() * MX;
			float normY = event.getY() / getHeight() * MY;
			for (int i = 0; i < targets.size(); i++) {
				if (targets.get(i).isCollision(normX, normY)) {
					int value = targets.get(i).shoot();
					TempScore ts = new TempScore(this, event.getX(),
							event.getY(), value);
					tempScores.add(ts);
					miss = false;
					break;
				}
			}
			if (miss) {
				misses++;
				playSound(SOUND_MISS);
				score += SCORE_MISS;
				TempScore ts = new TempScore(this, event.getX(), event.getY(),
						SCORE_MISS);
				tempScores.add(ts);
				if (activity.isMultiplayer())
					activity.sendScoreUpdate(score);
			}
			break;
		}
		return true;

	}

	/**
	 * Play a sound
	 * 
	 * @param soundID
	 *            pick from constants in {@linkplain GameView}
	 */

	public void playSound(int soundID) {
		if (soundOn)
			sounds.play(soundID, 1, 1, 0, 0, 1);
	}

	/**
	 * called when opponent shoots a target
	 * 
	 * @param targetId
	 *            the id of the {@linkplain Target} that opponent shot
	 */
	public void opponentShot(int targetId) {
		for (int i = 0; i < targets.size(); i++) {
			if (targets.get(i).id == targetId) {
				targets.get(i).shootOpponent();
				break;
			}
		}
	}

	/**
	 * Add a target with given type and random x,y,v,d. Used when target is
	 * added locally
	 */
	public void addTarget(int type) {
		targets.add(new Target(this, type));
	}

	/**
	 * Add a target with given type and given x,y,v,d. Used when target is added
	 * by opponent
	 */
	public void addTarget(int type, int id, float x, float y, float v, double d) {
		targets.add(new Target(this, type, id, x, y, v, d));
	}

	/**
	 * Draw a new frame (each {@linkplain Target} handles his movement by
	 * himself)
	 * 
	 * @param canvas
	 */
	public void redraw(Canvas canvas) {
		// randomly add targets TODO add target according to lvl.
		if (rnd.nextDouble() < pNormal)
			addTarget(Target.TYPE_NORMAL);
		if (rnd.nextDouble() < pDiamond)
			addTarget(Target.TYPE_DIAMOND);
		if (rnd.nextDouble() < pFlower)
			addTarget(Target.TYPE_FLOWER);

		canvas.drawBitmap(BMP_BG, 0, 0, null);// draw bg
		// targets nad temps
		for (int i = targets.size() - 1; i >= 0; i--) {
			targets.get(i).draw(canvas);
		}
		for (int i = temps.size() - 1; i >= 0; i--) {
			temps.get(i).draw(canvas);
		}
		for (int i = tempScores.size() - 1; i >= 0; i--) {
			tempScores.get(i).draw(canvas);
		}
		// end targets

		// scores TODO move to R.string
		String timeleftString = String.format("Time left: %.2f",
				timeleft / 1000.0f);
		float textSize = scoreFillPaint.getTextSize();
		canvas.drawText(timeleftString, 10, textSize * 1.2f, scoreFillPaint);
		canvas.drawText(timeleftString, 10, textSize * 1.2f, scoreStrokePaint);

		String scoreString = String.format("Score %d", score);
		canvas.drawText(scoreString, 10, textSize * 2.4f, scoreFillPaint);
		canvas.drawText(scoreString, 10, textSize * 2.4f, scoreStrokePaint);

		if (activity.isMultiplayer()) {
			// / draw opponent's score in mp
			String oppScoreString = String.format("Opponent score %d",
					scoreOpponent);
			canvas.drawText(oppScoreString, 10, textSize * 3.6f, scoreFillPaint);
			canvas.drawText(oppScoreString, 10, textSize * 3.6f,
					scoreStrokePaint);
		} else {
			// / draw target score in sp
			String targetScoreString = String.format("Target score %d",
					targetScore);
			canvas.drawText(targetScoreString, 10, textSize * 3.6f,
					scoreFillPaint);
			canvas.drawText(targetScoreString, 10, textSize * 3.6f,
					scoreStrokePaint);
		}
		// end scores

	}

	/**
	 * Thread handling redrawing of the frames at constant framerate
	 * {@linkplain #FPS}. Start it using {@linkplain GameView#startThread()}.
	 * Keeps running until {@linkplain #running} turns false.( this is achieved
	 * by calling {@link GameView#stopThread()} )
	 * 
	 * @author packito
	 * 
	 */
	public class GameThread extends Thread {

		private static final long FPS = 25;
		// volatile because of accessing from different threads
		private volatile boolean running = true;

		@Override
		public void run() {
			long ticksPS = 1000 / FPS;
			long startTime, sleepTime, lastTime = 0;

			// start playing music
			if (soundOn)
				music.start();

			while (running) {
				if (!holder.getSurface().isValid())
					continue;// surface is still preparing

				// handle the times
				startTime = System.currentTimeMillis();
				if (lastTime > 0) {// if the game has not been paused
					timeleft -= startTime - lastTime;
					if (timeleft < 0) {
						running = false;
						// TODO game over, time up
						activity.gameOver();
					}
				}
				lastTime = startTime;

				// do the drawing
				Canvas canvas = null;
				synchronized (holder) {
					canvas = holder.lockCanvas();
					try {
						redraw(canvas);
					} catch (NullPointerException e) {
						e.printStackTrace();
						Log.d("GameView", "canvas is null");
					} finally {
						if (canvas != null)
							holder.unlockCanvasAndPost(canvas);
					}
				}

				// handle constant FPS
				sleepTime = ticksPS - (System.currentTimeMillis() - startTime);
				try {
					if (sleepTime > 0)
						Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}// end loop, game is paused

			lastTime = 0;
			// stop playin music
			if (music.isPlaying())
				music.pause();
		}
	}

	/** start the GameThread */
	public void startThread() {
		soundOn = activity.preferences.getBoolean(
				TargetorApplication.TARGETOR_KEY_SOUND_ON, true);

		thread = new GameThread();
		thread.start();
	}

	/** check if {@linkplain GameThread} is running */
	public boolean isRunning() {
		boolean r;
		if (thread == null) {
			r = false;
		} else {
			r = thread.running;
		}
		return r;
	}

	/** stop the GameThread */
	public void stopThread() {
		if (thread != null) {
			thread.running = false;
			thread = null;
		}
	}

	// SurfaceHolder.Callback methods
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// update MX, MX (normalised screen size)
		MX = 1.0f;
		MY = (float) height / (float) width;

		scoreFillPaint.setTextSize(width / 20.0f);
		scoreStrokePaint.setTextSize(width / 20.0f);
		scoreStrokePaint.setStrokeWidth(width / 500.0f);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// ensure we're not drawing after surface is destroyed
		stopThread();
	}

}
