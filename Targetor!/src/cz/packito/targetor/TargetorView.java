package cz.packito.targetor;

import java.util.ArrayList;

import android.R.string;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class TargetorView extends SurfaceView implements SurfaceHolder.Callback {
	
	private static final String TAG= "TargetorView";

	// Content
	private ArrayList<TButton> buttonsMenu;

	// States
	public static final int STATE_LOADING = 0;
	public static final int STATE_MENU = 1;
	public static final int STATE_BTMENU = 2;
	public static final int STATE_GAME = 3;
	public static final int STATE_GAME_PAUSED = 4;
	
	private int state = STATE_LOADING;

	// Android stuff
	private final SurfaceHolder holder;
	private final TargetorActivity activity;
	private final Resources res;

	// Multimedia
	private Bitmap menuBg;
	private MediaPlayer musicMenu, musicGame;

	// preferences
	boolean soundOn = true;
	

	public TargetorView(TargetorActivity act) {
		super(act);
		this.activity = act;
		res = getResources();
		holder = getHolder();
		holder.addCallback(this);
		

		setState(STATE_LOADING);
		
		//get screen size
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		final int width = metrics.widthPixels;
		final int height = metrics.heightPixels;
		
		Thread loadingThread = new Thread() {

			@Override
			public void run() {
				// Load Menu Buttons
				Bitmap logoBmp=BitmapFactory.decodeResource(res, R.drawable.menu_logo);
				Bitmap quitBmp = BitmapFactory.decodeResource(res,
						R.drawable.quit);
				Bitmap exitBmp = BitmapFactory.decodeResource(res,
						R.drawable.exit_game);
				Bitmap shareBmp = BitmapFactory.decodeResource(res,
						R.drawable.share);
				Bitmap infoBmp = BitmapFactory.decodeResource(res,
						R.drawable.info);
				Bitmap soundOnBmp = BitmapFactory.decodeResource(res,
						R.drawable.sound_on);
				Bitmap soundOffBmp = BitmapFactory.decodeResource(res,
						R.drawable.sound_off);

				menuBg = BitmapFactory.decodeResource(getResources(),
						R.drawable.bg_menu);

				buttonsMenu = new ArrayList<TButton>();

				// Menu logo
				buttonsMenu.add(new TButton(width, height, logoBmp, logoBmp,
						TButton.SIZE_LOGO, TButton.CENTER, TButton.MIN) {
					@Override
					public void onClick() {
						// Does nothing, just image
					}
				});
				
				// Quit button
				buttonsMenu.add(new TButton(width, height, quitBmp, exitBmp,
						TButton.SIZE_BUTTON, TButton.MIN, TButton.MIN) {
					@Override
					public void onClick() {
						activity.finish();
					}
				});

				// Share button
				buttonsMenu.add(new TButton(width, height, shareBmp, shareBmp,
						TButton.SIZE_BUTTON, TButton.MAX, TButton.MAX) {
					@Override
					public void onClick() {
						// TODO 
					}
				});

				// Info button
				buttonsMenu.add(new TButton(width, height, infoBmp, infoBmp,
						TButton.SIZE_BUTTON, TButton.MIN, TButton.MAX) {
					@Override
					public void onClick() {
						// TODO Auto-generated method stub

					}
				});

				// load sound prefs
				soundOn = activity.preferences.getBoolean(
						TargetorApplication.TARGETOR_KEY_SOUND_ON, true);
				
				// Sound toggle
				buttonsMenu.add(new TToggle(soundOn, width, height,
						soundOffBmp, soundOnBmp, TButton.SIZE_BUTTON,
						TButton.MAX, TButton.MIN) {
					@Override
					public void onToggle(boolean newValue) {
						soundOn = newValue;
						if (soundOn)
							startMusic();
						else
							stopMusic();
						SharedPreferences.Editor editor= activity.preferences.edit();
						editor.putBoolean(TargetorActivity.TARGETOR_KEY_SOUND_ON, soundOn);
						editor.commit();
					}
				});
				// End menu buttons


				setState(STATE_MENU);
				if (soundOn) {
					startMusic();
				}
				redraw();
			}
		};
		loadingThread.start();
	}

	public void redraw() {

		while(!holder.getSurface().isValid());
		
		Canvas canvas = holder.lockCanvas();

		switch (state) {
		case STATE_LOADING:
			canvas.drawBitmap(
					BitmapFactory.decodeResource(res, R.drawable.bg_loading),
					0, 0, null);
			break;
		case STATE_MENU:
			canvas.drawBitmap(menuBg, 0, 0, null);
			for (TButton button : buttonsMenu) {
				button.drawOn(canvas);
			}
			break;
		case STATE_GAME:

			break;
		}

		holder.unlockCanvasAndPost(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (state) {
		case STATE_MENU:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				for (TButton button : buttonsMenu) {
					if (button.contains(event.getX(), event.getY())) {
						button.press();
					}
				}
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_CANCEL:
				for (TButton button : buttonsMenu) {
					if (!button.contains(event.getX(), event.getY())) {
						button.cancel();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				for (TButton button : buttonsMenu) {
					if (button.contains(event.getX(), event.getY())) {
						button.release();
					}
				}
				break;
			}
			redraw();
			break;
		case STATE_GAME:
			// TODO
			break;
		}
		return true;
	}

	// SurfaceHolder.Callback methods

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format,
			final int width, final int height) {
		Log.d(TAG, "surfaceChanged");
		redraw();
	}

	public void startMusic() {
		switch (state) {
		case STATE_MENU:
		case STATE_BTMENU:
			musicMenu = MediaPlayer.create(activity, R.raw.music_menu);
			musicMenu.setLooping(true);
			musicMenu.start();
			break;
		case STATE_GAME:
			musicGame = MediaPlayer.create(activity, R.raw.music_game);
			musicGame.setLooping(true);
			musicGame.start();
			break;
		}
	}

	public void stopMusic() {
		if (musicGame != null) {
			musicGame.release();
			musicGame = null;
		}
		if (musicMenu != null) {
			musicMenu.release();
			musicMenu = null;
		}
	}

	public void setState(int state) {
		this.state = state;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
	}

}
