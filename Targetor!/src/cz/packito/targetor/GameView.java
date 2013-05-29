package cz.packito.targetor;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
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

	public final Bitmap BMP_BG, BMP_TARGET_NORMAL, BMP_TARGET_NORMAL_TEMP;
	public float MX, MY; // normalized screen resolution; MX=1.0,
							// MY=height/width
	public int idGenerator = 0;

	private final SurfaceHolder holder;
	public List<Target> targets = new ArrayList<Target>();
	public List<TempTarget> temps = new ArrayList<TempTarget>();
	private GameThread thread;
	public GameActivity activity;
	public int score = 0;
	public int scoreOpponent = 0;
	public long timeleft = 600000;// ms

	private boolean soundOn = true;
	public final SoundPool sounds;
	public final MediaPlayer music;
	public final int SOUND_MISS, SOUND_TARGET_NORMAL;

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

		// load the bitmaps
		BMP_BG = BitmapFactory.decodeResource(getResources(),
				R.drawable.bg_game);
		BMP_TARGET_NORMAL = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_normal);
		BMP_TARGET_NORMAL_TEMP = BitmapFactory.decodeResource(getResources(),
				R.drawable.target_normal_temp);

		// load the sounds
		music = MediaPlayer.create(activity, R.raw.music_game);
		music.setLooping(true);
		sounds = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		SOUND_MISS = sounds.load(activity, R.raw.miss, 1);
		SOUND_TARGET_NORMAL = sounds
				.load(activity, R.raw.target_normal_shot, 1);

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
					targets.get(i).shoot();
					miss = false;
					break;
				}
			}
			if (miss) {
				playSound(SOUND_MISS);
				score--;
				// TODO multiplayer
				// activity.sendScoreUpdate(score);
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
		// TODO
		// for (int i = 0; i < targets.size(); i++) {
		// if (targets.get(i).id == targetId) {
		// targets.get(i).shootOpponent();
		// break;
		// }
		// }
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
		// randomly add targets
		if (Math.random() < 0.03) {
			addTarget(Target.TYPE_NORMAL);
		}

		canvas.drawBitmap(BMP_BG, 0, 0, null);// draw bg
		// scores
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTypeface(Typeface.MONOSPACE);
		paint.setTextSize(30);
		canvas.drawText("Time left: " + (float) timeleft / 1000 + "s", 50, 50,
				paint);
		canvas.drawText("Score " + score, 50, 100, paint);
		if (activity.multiplayer)
			canvas.drawText("Opponent score " + scoreOpponent, 50, 150, paint);
		// end scores

		// targets nad temps
		for (int i = targets.size() - 1; i >= 0; i--) {
			targets.get(i).draw(canvas);
		}
		for (int i = temps.size() - 1; i >= 0; i--) {
			temps.get(i).draw(canvas);
		}
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
						activity.toastFromAnotherThread("Game over! " + score
								+ " points");
						// end TODO
						activity.finish();
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
					Log.d("GameView", "sleep time: " + sleepTime + "ms");
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
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// ensure we're not drawing after surface is destroyed
		stopThread();
	}

}
