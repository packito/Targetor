package cz.packito.targetor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Display;
import android.view.View;
import android.widget.Checkable;
import android.widget.Toast;

public class GameActivity extends Activity {

	private static final String TAG = "GameActivity";
	private GameView gameView;
	private View pauseScreen;
	private WakeLock wakeLock;

	public boolean multiplayer;
	public SharedPreferences preferences;
	// public BluetoothSocket btSocket = null;
	// private ConnectedThread connectedThread = null;
	private boolean screenRatioNegotiated = false;

	private int displayWidth, displayHeight;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the views
		setContentView(R.layout.activity_game);
		gameView = (GameView) findViewById(R.id.game_view);
		pauseScreen = findViewById(R.id.pause_screen);

		// load the precerences
		preferences = getPreferences(Context.MODE_PRIVATE);
		Checkable soundToggle = (Checkable) findViewById(R.id.pause_sound_toggle);
		soundToggle.setChecked(preferences.getBoolean("sound", true));

		multiplayer = getIntent().getBooleanExtra(
				TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, false);

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// request the wake lock
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
		wakeLock.acquire();
		// TODO for multiplayer
		// if (multiplayer) {
		// TargetorApplication app = (TargetorApplication) getApplication();
		// btSocket = app.btSocket;
		//
		// if (btSocket == null) {
		// Toast.makeText(this, "Bluetooth socket is null",
		// Toast.LENGTH_LONG).show();
		// finish();
		// }
		// connectedThread = new ConnectedThread(btSocket);
		// connectedThread.start();
		// }
		// if (!screenRatioNegotiated) {// screen ratio not negotiated yet
		// sendScreenSize();
		// }
	}

	@Override
	protected void onPause() {
		if (gameView.isRunning()) {
			pauseGame();
		}
		// TODO for multiplayer
		// if (connectedThread != null) {
		// connectedThread.running = false;
		// connectedThread = null;
		// }
		wakeLock.release();
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if (gameView.isRunning()) {
			pauseGame();
		} else {
			resumeGame(null);
		}
	}

	/** Pauses the game and shows the pause screen. Call only from UI thread */
	public void pauseGame() {
		gameView.stopThread();
		pauseScreen.setVisibility(View.VISIBLE);
		// TODO for multiplayer
		// if (multiplayer) {
		// sendGamePaused();
		// }
	}

	/**
	 * Resumes the game and hides the pause screen. Call only from UI thread
	 * 
	 * @param v
	 *            has no effect
	 */
	public void resumeGame(View v) {
		pauseScreen.setVisibility(View.INVISIBLE);
		gameView.startThread();
		// TODO for multiplayer
		// if (multiplayer) {
		// sendGameResumed();
		// }

	}

	/** Quitting the game by pressing quit button */
	public void quitGame(View v) {
		gameView.music.release();
		// TODO for multiplayer
		// sendGameQuit();
		finish();
	}

	/**
	 * Called by toggling the sound on pause screen. Writes to
	 * SharedPreferences.
	 * 
	 * @param v
	 *            the toggle ({@linkplain Checkable})
	 */

	public void toggleSound(View v) {
		boolean soundOn = ((Checkable) v).isChecked();
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(TargetorApplication.TARGETOR_KEY_SOUND_ON, soundOn);
		editor.commit();
	}

	/**
	 * show a {@linkplain Toast}, safe to call from another thread
	 * 
	 * @param text
	 *            the text to show on Toast
	 */
	public void toastFromAnotherThread(final String text) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(GameActivity.this, text, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	/**
	 * show a {@linkplain Toast}, safe to call from another thread
	 * 
	 * @param resid
	 *            the string resource to show on Toast
	 */

	public void toastFromAnotherThread(final int resid) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(GameActivity.this, resid, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}
}
