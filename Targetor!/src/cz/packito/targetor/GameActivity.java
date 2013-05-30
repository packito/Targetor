package cz.packito.targetor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class GameActivity extends Activity implements OnCheckedChangeListener {

	private static final String TAG = "GameActivity";
	private GameView gameView;
	private View pauseScreen;
	private ToggleButton soundToggle;
	private View resume;
	private WakeLock wakeLock;

	private boolean multiplayer;

	public boolean isMultiplayer() {
		return multiplayer;
	}

	public SharedPreferences preferences;
	public BluetoothSocket btSocket = null;
	private ConnectedThread connectedThread = null;
	private boolean screenRatioNegotiated = false;

	private int displayWidth, displayHeight;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the views
		setContentView(R.layout.activity_game);
		gameView = (GameView) findViewById(R.id.game_view);
		pauseScreen = findViewById(R.id.pause_screen);
		soundToggle = (ToggleButton) findViewById(R.id.pause_sound);
		resume = findViewById(R.id.resume);

		// load the precerences
		preferences = getSharedPreferences(
				TargetorApplication.SHARED_PREFERENCES, MODE_PRIVATE);
		soundToggle.setChecked(preferences.getBoolean(
				TargetorApplication.TARGETOR_KEY_SOUND_ON, true));
		soundToggle.setOnCheckedChangeListener(this);

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
		if (isMultiplayer()) {
			TargetorApplication app = (TargetorApplication) getApplication();
			btSocket = app.btSocket;

			if (btSocket == null) {
				Toast.makeText(this, "Bluetooth socket is null",
						Toast.LENGTH_LONG).show();
				finish();
			}
			connectedThread = new ConnectedThread(btSocket);
			connectedThread.start();
			sendAppResumed();
			if (!screenRatioNegotiated) {// screen ratio not negotiated yet
				sendScreenSize();
			}
		}
	}

	@Override
	protected void onPause() {
		if (gameView.isRunning()) {
			pauseGame();
		}
		if (isMultiplayer()) {
			sendAppPaused();
			if (connectedThread != null) {
				connectedThread.running = false;
				connectedThread = null;
			}
		}
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
		if (isMultiplayer()) {
			sendGamePaused();
		}
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
		if (isMultiplayer()) {
			sendGameResumed();
		}

	}

	/** Quitting the game by pressing quit button */
	public void quitGame(View v) {
		gameView.music.release();
		if (isMultiplayer()) {
			connectedThread.cancel();
			sendGameQuit();
		}
		finish();
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

	/**
	 * Called by toggling the sound on pause screen. Writes to
	 * SharedPreferences.
	 * 
	 */

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.pause_sound:
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(TargetorApplication.TARGETOR_KEY_SOUND_ON,
					isChecked);
			editor.commit();
			break;
		}
	}

	/** Thread that handles sending and receiving data via Bluetooth */

	private class ConnectedThread extends Thread {

		/** int width, int height */
		public static final byte SCREEN_SIZE = 100;
		/** int id */
		public static final byte TARGET_SHOT = 101;
		/**
		 * int type, int id, float x, float y, float v, double d (see
		 * {@link Target})
		 */
		public static final byte NEW_TARGET = 102;
		/** int score */
		public static final byte SCORE_UPDATE = 103;
		/** no data */
		public static final byte GAME_PAUSED = 104;
		/** no data */
		public static final byte GAME_RESUMED = 105;
		/** no data */
		public static final byte GAME_QUIT = 106;
		/** no data */
		public static final byte APP_PAUSED = 107;
		/** no data */
		public static final byte APP_RESUMED = 108;

		private final BluetoothSocket socket;
		private InputStream inStream;
		private OutputStream outStream;

		public boolean running;

		public ConnectedThread(BluetoothSocket socket) {
			this.socket = socket;
			try {
				inStream = socket.getInputStream();
				outStream = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // number of bytes returned from read()

			running = true;
			// Keep listening to the InputStream until an exception occurs
			loop: while (running) {
				try {
					// Read from the InputStream
					bytes = inStream.read(buffer);
					Log.d("bluetoothTargetor", "Read " + bytes + " bytes");

					if (bytes > 0) {// handle received data
						ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0,
								bytes);

						// see the first byte of received packet
						switch (byteBuffer.get()) {
						case GAME_PAUSED:
							if (gameView.isRunning()) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										pauseGame();
									}
								});
							}
							break;
						case GAME_RESUMED:
							if (!gameView.isRunning()) {

								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										resumeGame(null);
									}
								});
							}
							break;
						case GAME_QUIT:
							toastFromAnotherThread(R.string.opponent_left);
							connectedThread.cancel();
							// TODO handle this better
							finish();
							break loop;
						case APP_PAUSED:
							// opponent left the app, prevent from resuming
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									resume.setEnabled(false);
									resume.setClickable(false);
								}
							});
							break;
						case APP_RESUMED:
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									resume.setEnabled(true);
									resume.setClickable(true);
								}
							});
							break;
						case NEW_TARGET:
							int type = byteBuffer.getInt();
							int id = byteBuffer.getInt();
							float x = byteBuffer.getFloat();
							float y = byteBuffer.getFloat();
							float v = byteBuffer.getFloat();
							double d = byteBuffer.getDouble();

							gameView.addTarget(type, id, x, y, v, d);
							break;
						case TARGET_SHOT:
							int targetId = byteBuffer.getInt();
							gameView.opponentShot(targetId);
							break;
						case SCORE_UPDATE:
							int score = byteBuffer.getInt();
							gameView.scoreOpponent = score;
							break;
						case SCREEN_SIZE:
							// negotiate screen ratio
							// will choose the narrower screen
							int remoteWidth = byteBuffer.getInt();
							int remoteHeight = byteBuffer.getInt();
							final float remoteRatio = (float) remoteHeight
									/ (float) remoteWidth;

							float localRatio = (float) displayHeight
									/ (float) displayWidth;
							if (remoteRatio > localRatio) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
												(int) (displayHeight / remoteRatio),
												displayHeight);
										layoutParams
												.addRule(RelativeLayout.CENTER_IN_PARENT);
										gameView.setLayoutParams(layoutParams);
									}
								});
							}
							screenRatioNegotiated = true;
							break;
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
					toastFromAnotherThread(R.string.connection_lost);
					finish();
					break;
				}
			}
		}

		/**
		 * Call this from the main activity to send data to the remote device.
		 * First byte is the type of meassage (choose from constants in
		 * {@link ConnectedThread}). Other bytes depend upon choosen type of
		 * message.
		 */
		public void write(byte[] bytes) {
			try {
				outStream.write(bytes);
				outStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/** Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	public void sendGamePaused() {
		byte[] bytes = new byte[] { ConnectedThread.GAME_PAUSED };
		connectedThread.write(bytes);
	}

	public void sendGameResumed() {
		byte[] bytes = new byte[] { ConnectedThread.GAME_RESUMED };
		connectedThread.write(bytes);
	}

	public void sendGameQuit() {
		byte[] bytes = new byte[] { ConnectedThread.GAME_QUIT };
		connectedThread.write(bytes);
	}

	public void sendAppPaused() {
		byte[] bytes = new byte[] { ConnectedThread.APP_PAUSED };
		connectedThread.write(bytes);
	}

	public void sendAppResumed() {
		byte[] bytes = new byte[] { ConnectedThread.APP_RESUMED };
		connectedThread.write(bytes);
	}

	public void sendNewTarget(int type, int id, float x, float y, float v,
			double d) {

		// 2*int,byte,3*float,double=29Bytes
		ByteBuffer byteBuffer = ByteBuffer.allocate(29);
		byteBuffer.put(ConnectedThread.NEW_TARGET).putInt(type).putInt(id)
				.putFloat(x).putFloat(y).putFloat(v).putDouble(d);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);
	}

	public void sendTargetShot(int id) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(5);
		byteBuffer.put(ConnectedThread.TARGET_SHOT).putInt(id);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);
	}

	public void sendScoreUpdate(int score) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(5);
		byteBuffer.put(ConnectedThread.SCORE_UPDATE).putInt(score);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);

	}

	public void sendScreenSize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(9);
		byteBuffer.put(ConnectedThread.SCREEN_SIZE).putInt(displayWidth)
				.putInt(displayHeight);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);
	}
}
