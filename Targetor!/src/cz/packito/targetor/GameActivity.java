package cz.packito.targetor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import cz.packito.targetor.GameView.GameThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.InputFilter.LengthFilter;
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
	private TextView resume;
	private WakeLock wakeLock;

	private boolean multiplayer;
	/** flag if the activity is currently resumed */
	private boolean onTop;
	private boolean opponentReady = false;
	private boolean opponentPausedOneSecondAgoDamnThisIsALongVariablename = false;

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
		resume = (TextView) findViewById(R.id.resume);

		TargetorApplication.changeTypeface(this, R.id.resume);

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
		onTop = true;
	}

	@Override
	protected void onPause() {
		onTop = false;
		if (gameView.isRunning()) {
			pauseGame();
		}
		if (isMultiplayer()) {
			if (connectedThread != null) {
				sendAppPaused();
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
			quitGame(null);
		}
	}

	/** Pauses the game and shows the pause screen. Call only from UI thread */
	public void pauseGame() {
		gameView.stopThread();
		pauseScreen.setVisibility(View.VISIBLE);
		if (isMultiplayer() && connectedThread != null) {
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
		// resume only if opponent is ready in multiplayer
		if (onTop && (opponentReady || !multiplayer)) {
			// block from resuming if opponent just paused
			if (!opponentPausedOneSecondAgoDamnThisIsALongVariablename) {
				// resume the game
				pauseScreen.setVisibility(View.INVISIBLE);
				resume.setText(R.string.touch_to_resume);
				gameView.startThread();
				if (isMultiplayer()) {
					sendGameResumed();
				}
			}
		} else {
			toastFromAnotherThread(R.string.opponent_not_ready);
		}
	}

	/** Shows a prompt to quit the game */
	public void quitGame(View v) {
		AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
		quitDialog
				.setTitle(R.string.are_you_sure)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								gameView.music.release();
								if (isMultiplayer()) {
									sendGameQuit();
									connectedThread.cancel();
								}
								finish();
							}
						});
		quitDialog.show();
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

	/** stops the current {@link ConnectedThread} */
	public void disconnect() {
		if (connectedThread != null) {
			connectedThread.running = false;
			connectedThread = null;
		}
	}

	public void gameOver() {
		gameView.stopThread();
		// TODO finish
		if (isMultiplayer() && connectedThread != null) {
			sendGameOver(gameView.score, gameView.targetsShot, gameView.misses);
		} else {
			startFinishActivity();
		}
	}

	private void startFinishActivity() {
		Intent finishIntent = new Intent(this, FinishActivity.class);
		finishIntent
				.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER,
						isMultiplayer())
				.putExtra(TargetorApplication.TARGETOR_EXTRA_SCORE,
						gameView.score)
				.putExtra(TargetorApplication.TARGETOR_EXTRA_TARGETS_SHOT,
						gameView.targetsShot)
				.putExtra(TargetorApplication.TARGETOR_EXTRA_MISSES,
						gameView.misses);
		if (isMultiplayer()) {
			finishIntent
					.putExtra(
							TargetorApplication.TARGETOR_EXTRA_OPPONENT_MISSES,
							gameView.missesOpponent)
					.putExtra(
							TargetorApplication.TARGETOR_EXTRA_OPPONENT_SCORE,
							gameView.scoreOpponent)
					.putExtra(
							TargetorApplication.TARGETOR_EXTRA_OPPONENT_TARGETS_SHOT,
							gameView.targetsShotOpponent);
		}
		startActivity(finishIntent);
		finish();
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
		/** int score, int targetsShot, int misses */
		public static final byte GAME_OVER = 109;

		private final BluetoothSocket socket;
		private InputStream inStream;
		private OutputStream outStream;

		/**
		 * The length of each message in bytes. Shorter messages will have
		 * zeroes added at the end to match the length
		 */
		public static final int MESSAGE_LENGTH = 60;

		public boolean running;
		private boolean gameIsOver=false;

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
			byte[] buffer = new byte[MESSAGE_LENGTH]; // buffer store for the
														// stream
			int bytes; // number of bytes returned from read()

			running = true;
			// Keep listening to the InputStream until an exception occurs
			loop: while (running) {
				try {
					// Read from the InputStream
					bytes = inStream.read(buffer);
					Log.d(TAG, "Read " + bytes + " bytes");

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
							opponentPausedOneSecondAgoDamnThisIsALongVariablename = true;
							new Thread() {
								public void run() {
									try {
										Thread.sleep(1000);
										opponentPausedOneSecondAgoDamnThisIsALongVariablename = false;
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								};
							}.start();
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
						case GAME_OVER:
							gameView.scoreOpponent = byteBuffer.getInt();
							gameView.targetsShotOpponent = byteBuffer.getInt();
							gameView.missesOpponent = byteBuffer.getInt();
							if (!gameIsOver)
								sendGameOver(gameView.score,
										gameView.targetsShot, gameView.misses);
							gameIsOver = true;
							disconnect();
							startFinishActivity();
							
							//empty the inStream
							// prevents from reading leftovers when restarting game
							while(inStream.available()>0)
								inStream.read();
							break loop;
						case APP_PAUSED:
							// opponent left the app, prevent from resuming
							opponentReady = false;
							break;
						case APP_RESUMED:
							if (!opponentReady)
								sendAppResumed();
							opponentReady = true;
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
		 * message and filled to match the {@linkplain #MESSAGE_LENGTH}.
		 */
		public void write(byte[] bytes) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_LENGTH);
			try {
				byteBuffer.put(bytes);
			} catch (BufferOverflowException e) {
				e.printStackTrace();
			}
			while (byteBuffer.hasRemaining()) {// fill to match length
				byteBuffer.put((byte) 0);
			}
			try {
				outStream.write(byteBuffer.array());
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

	public void sendGameOver(int score, int targetsShot, int misses) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(13);
		byteBuffer.put(ConnectedThread.GAME_OVER).putInt(score)
				.putInt(targetsShot).putInt(misses);

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
