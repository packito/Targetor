package cz.packito.targetor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import cz.packito.targetor.R.string;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TargetorView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "TargetorView";

	// Constants
	public static final int LEVELS = 20;

	// Content
	private ArrayList<TButton> buttonsMenu;
	private ArrayList<TButton> buttonsGamePaused;
	private ArrayList<TButton> buttonsPickLevel;
	private ArrayList<TButton> buttonsFinish;
	private ArrayList<TButton> buttonsBtMenu;
	private ArrayList<TText> textsFinish;
	private TButton discoverableButton, searchButton, helpButton,
			bluetoothLogo;

	private TButton nextButton;

	// States
	public static final int STATE_LOADING = 0;
	public static final int STATE_MENU = 1;
	public static final int STATE_BTMENU = 2;
	public static final int STATE_GAME = 3;
	public static final int STATE_GAME_PAUSED = 4;
	public static final int STATE_LEVEL_PICKER = 5;
	public static final int STATE_FINISH = 6;

	private int state = STATE_LOADING;

	// Android stuff
	private final SurfaceHolder holder;
	private final TargetorActivity activity;
	private final Resources res;
	private ProgressDialog dialog;
	/** Screen dimensions */
	volatile int width, height;

	// Bluetooth stuff
	public static final UUID MY_UUID = UUID
			.fromString("c3f8407d-f3b7-45d8-a1a2-3965a58305e7");
	public static final String NAME = "Targetor";

	private AcceptThread acceptThread;
	private ConnectedThread connectedThread;
	private BluetoothAdapter bluetoothAdapter;
	private BTReceiver btReceiver;
	private BluetoothSocket btSocket;
	private Set<BluetoothDevice> newDevices, bondedDevices;
	private boolean btWasOn = false;

	private volatile boolean opponentReady = false;
	private boolean opponentPausedOneSecondAgoDamnThisIsALongVariablename = false;
	private boolean screenRatioNegotiated = false;

	// Multimedia
	private Bitmap MENU_BG_BMP, BLUETOOTH_BMP;
	private MediaPlayer musicMenu, musicGame;
	private int musicMenuSeek = 0, musicGameSeek = 0;
	/** Game bitmaps */
	public Bitmap BMP_BG, BMP_TARGET_NORMAL, BMP_TARGET_NORMAL_TEMP,
			BMP_TARGET_DIAMOND, BMP_TARGET_DIAMOND_TEMP, BMP_TARGET_FLOWER,
			BMP_TARGET_FLOWER_TEMP;
	public SoundPool sounds;
	/** Game sounds */
	public int SOUND_MISS, SOUND_TARGET_NORMAL, SOUND_TARGET_DIAMOND,
			SOUND_TARGET_FLOWER;
	/** game paints */
	private Paint scoreFillPaint, scoreStrokePaint;

	// Game stuff ///////////////////////////////////////////
	public final List<Target> targets = new ArrayList<Target>();
	public final List<TempTarget> temps = new ArrayList<TempTarget>();
	public final List<TempScore> tempScores = new ArrayList<TempScore>();
	private GameThread thread;

	boolean multiplayer;
	public volatile float MX = 0.0f, MY = 0.0f; // normalized screen resolution;
												// MX=1.0,
	// MY=height/width

	public int idGenerator = 0;
	private final Random rnd = new Random();

	/** points lost when missing a target */
	private static final int SCORE_MISS = -3;

	private static final int LEVEL_MULTIPLAYER = 0;

	private final Paint blackPaint;
	public int score = 0;
	public int targetsShot = 0;
	public int misses = 0;
	public int scoreOpponent = 0;
	public int targetsShotOpponent = 0;
	public int missesOpponent = 0;
	private long countdown;
	public long timeleft;// ms
	private int level;

	private int targetScore;
	/** probabilities of a target occurence */
	private double pNormal, pDiamond, pFlower;

	// //////////////////////////////////////////////////////

	// preferences
	boolean soundOn = true;

	public TargetorView(TargetorActivity act, int screenWidth, int screenHeight) {
		super(act);
		this.activity = act;
		res = getResources();
		holder = getHolder();
		holder.addCallback(this);

		blackPaint = new Paint();
		blackPaint.setColor(Color.BLACK);

		setState(STATE_LOADING);

		// screen size
		width = screenWidth;
		height = screenHeight;

		Thread loadingThread = new Thread() {

			@Override
			public void run() {

				scoreFillPaint = new Paint();
				scoreFillPaint.setTypeface(TargetorActivity.getTypeface());
				scoreFillPaint.setColor(Color.WHITE);

				scoreStrokePaint = new Paint(scoreFillPaint);
				scoreStrokePaint.setStyle(Paint.Style.STROKE);
				scoreStrokePaint.setColor(Color.BLACK);

				loadMenuButtons();

				loadGameMultimedia();

				setState(STATE_MENU);
				redraw();
			}

		};
		loadingThread.start();
	}

	/**
	 * Load the menu bitmaps and add menu buttons to {@link #buttonsMenu}
	 * 
	 * @param width
	 *            screen width
	 * @param height
	 *            screen height
	 */
	private void loadMenuButtons() {
		// Load Menu Button bitmaps
		Bitmap logoBmp = BitmapFactory
				.decodeResource(res, R.drawable.menu_logo);
		Bitmap quitBmp = BitmapFactory.decodeResource(res, R.drawable.quit);
		Bitmap shareBmp = BitmapFactory.decodeResource(res, R.drawable.share);
		Bitmap infoBmp = BitmapFactory.decodeResource(res, R.drawable.info);
		Bitmap soundOnBmp = BitmapFactory.decodeResource(res,
				R.drawable.sound_on);
		Bitmap soundOffBmp = BitmapFactory.decodeResource(res,
				R.drawable.sound_off);
		Bitmap helpBmp = BitmapFactory.decodeResource(res, R.drawable.help);

		MENU_BG_BMP = BitmapFactory.decodeResource(getResources(),
				R.drawable.bg_menu);
		BLUETOOTH_BMP = BitmapFactory.decodeResource(res,
				R.drawable.menu_bluetooth_bg);

		TButton menuLogo = new TText(width, height, logoBmp, logoBmp,
				TButton.SIZE_LOGO, TButton.CENTER, TButton.MIN);

		TButton quitButton = new TButton(width, height, quitBmp, quitBmp,
				TButton.SIZE_BUTTON, TButton.MIN, TButton.MIN) {
			@Override
			public void onClick() { // quit button
				switch (state) {
				case STATE_GAME_PAUSED:
					setState(STATE_MENU);
					disconnect();
					width = getWidth();
					height = getHeight();
					redraw();
					break;
				case STATE_MENU:
					activity.finish();
					break;
				}
			}
		};

		TButton shareButton = new TButton(width, height, shareBmp, shareBmp,
				TButton.SIZE_BUTTON, TButton.MAX, TButton.MAX) {
			@Override
			public void onClick() {
				// TODO
			}
		};

		TButton infoButton = new TButton(width, height, infoBmp, infoBmp,
				TButton.SIZE_BUTTON, TButton.MIN, TButton.MAX) {
			@Override
			public void onClick() {
				// TODO Auto-generated method stub

			}
		};

		// load sound prefs
		soundOn = activity.preferences.getBoolean(
				TargetorApplication.TARGETOR_KEY_SOUND_ON, true);

		TToggle soundToggle = new TToggle(soundOn, width, height, soundOffBmp,
				soundOnBmp, TButton.SIZE_BUTTON, TButton.MAX, TButton.MIN) {
			@Override
			public void onToggle(boolean newValue) {
				soundOn = newValue;
				if (soundOn)
					startMusic();
				else
					stopMusic();
				SharedPreferences.Editor editor = activity.preferences.edit();
				editor.putBoolean(TargetorActivity.TARGETOR_KEY_SOUND_ON,
						soundOn);
				editor.commit();
			}
		};

		TButton startSingleplayerButton = new TButton(width, height,
				res.getString(R.string.singleplayer), 0.4f, 0.25f,
				TButton.CENTER, 0.4f, true) {

			@Override
			public void onClick() {
				// Pick level;
				setState(STATE_LEVEL_PICKER);
				TargetorDatabase db = new TargetorDatabase(activity);
				db.open();
				int currentLevel = db.getLevel();
				db.close();

				buttonsPickLevel = new ArrayList<TButton>();

				TText levelPickTitle = new TText(width, height,
						res.getString(R.string.level_choose), TText.TITLE_W,
						TText.TITLE_H, TButton.CENTER, TButton.MIN, false);

				buttonsPickLevel.add(levelPickTitle);
				float vpos = 0.3f;
				for (int i = 1; i <= LEVELS; i++) {

					TButton levelButton = new TButton(i, width, height, "" + i,
							0.15f, 0.15f, 0.1f + ((i - 1) % 5) * 0.2f, vpos,
							true) {
						@Override
						public void onClick() {
							startGame((Integer) data);
						}
					};
					levelButton.setClickable(i <= currentLevel);

					buttonsPickLevel.add(levelButton);
					if (i % 5 == 0)
						vpos += 0.22f;
				}

				redraw();
			}
		};

		TButton startMultiplayerButton = new TButton(width, height,
				res.getString(R.string.multiplayer), 0.4f, 0.25f,
				TButton.CENTER, 0.8f, true) {

			@Override
			public void onClick() {
				setState(STATE_BTMENU);
				btReceiver = new BTReceiver();
				IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
				filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
				filter.addAction(BluetoothDevice.ACTION_FOUND);
				filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
				filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

				activity.registerReceiver(btReceiver, filter);
				bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				if (bluetoothAdapter == null) {
					Toast.makeText(activity, R.string.bluetooth_not_supported,
							Toast.LENGTH_LONG).show();
					setState(STATE_MENU);
				} else {// Bluetooth supported
					if (btWasOn = bluetoothAdapter.isEnabled())
						bluetoothIsOn();
					else
						bluetoothAdapter.enable();
				}

				redraw();
			}
		};

		buttonsMenu = new ArrayList<TButton>();
		buttonsMenu.add(quitButton);
		buttonsMenu.add(soundToggle);
		buttonsMenu.add(infoButton);
		buttonsMenu.add(shareButton);
		buttonsMenu.add(menuLogo);
		buttonsMenu.add(startSingleplayerButton);
		buttonsMenu.add(startMultiplayerButton);

		TButton resumeGameButton = new TButton(width, height,
				res.getString(R.string.touch_to_resume), TButton.MAX, 0.7f,
				TButton.CENTER, TButton.MAX, false) {

			@Override
			public void onClick() {
				resumeGame(true);
			}
		};
		buttonsGamePaused = new ArrayList<TButton>();
		buttonsGamePaused.add(resumeGameButton);
		buttonsGamePaused.add(soundToggle);
		buttonsGamePaused.add(quitButton);

		Bitmap exitBmp = BitmapFactory
				.decodeResource(res, R.drawable.exit_game);
		Bitmap nextBmp = BitmapFactory.decodeResource(res, R.drawable.next);
		Bitmap againBmp = BitmapFactory.decodeResource(res, R.drawable.again);

		TButton exitButton = new TButton(width, height, exitBmp, exitBmp,
				TButton.SIZE_BUTTON, 0.3f, TButton.MAX) {

			@Override
			public void onClick() {
				// TODO show prompt
				if (connectedThread != null)
					sendGameQuit();
				disconnect();
				setState(STATE_MENU);
				redraw();
			}
		};

		TButton againButton = new TButton(width, height, againBmp, againBmp,
				TButton.SIZE_BUTTON, TButton.CENTER, TButton.MAX) {

			@Override
			public void onClick() {
				startGame(level);
			}
		};
		nextButton = new TButton(width, height, nextBmp, nextBmp,
				TButton.SIZE_BUTTON, 0.7f, TButton.MAX) {
			@Override
			public void onClick() {
				startGame(++level);
			}
		};

		buttonsFinish = new ArrayList<TButton>();
		buttonsFinish.add(exitButton);
		buttonsFinish.add(againButton);
		buttonsFinish.add(nextButton);

		discoverableButton = new TButton(width, height,
				res.getString(R.string.make_discoverable), 0.4f, 0.18f, 0.3f,
				0.1f, true) {
			@Override
			public void onClick() { // request discoverability
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				activity.startActivity(discoverableIntent);
			}
		};
		searchButton = new TButton(width, height,
				res.getString(R.string.search_devices), 0.4f, 0.18f, 0.3f,
				0.35f, true) {
			@Override
			public void onClick() {// search for devices
				newDevices = new HashSet<BluetoothDevice>();
				bluetoothAdapter.startDiscovery();
			}
		};
		helpButton = new TButton(width, height, helpBmp, helpBmp,
				TButton.SIZE_BUTTON, TButton.MIN, TButton.MAX) {
			@Override
			public void onClick() { // show the help dialog
				AlertDialog.Builder helpDialog = new AlertDialog.Builder(
						activity);
				helpDialog.setIcon(android.R.drawable.ic_menu_help);
				helpDialog.setTitle(R.string.bt_help_title);
				String btHelpMsg = String.format(
						res.getString(R.string.bt_help_message),
						bluetoothAdapter.getName());
				helpDialog.setMessage(btHelpMsg);
				helpDialog.show();
			}
		};
		bluetoothLogo = new TText(width, height, BLUETOOTH_BMP, BLUETOOTH_BMP,
				0.18f, TButton.MIN, TButton.MIN);

		buttonsBtMenu = new ArrayList<TButton>();
		buttonsBtMenu.add(discoverableButton);
		buttonsBtMenu.add(searchButton);
		buttonsBtMenu.add(helpButton);
		buttonsBtMenu.add(bluetoothLogo);
		buttonsBtMenu.add(new TText(width, height, res
				.getString(R.string.loading), 0.4f, 0.15f, 0.98f, 0.1f, false));
	}

	/**
	 * show a {@linkplain Toast}, safe to call from another thread
	 * 
	 * @param text
	 *            the text to show on Toast
	 */
	public void toastFromAnotherThread(final String text) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
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
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(activity, resid, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// BLUETOOTH MEHTODS AND CLASSES ///////////////////////////////////////////
	private class BTReceiver extends BroadcastReceiver {
		float vpos = 0.5f;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
				switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
						BluetoothAdapter.SCAN_MODE_NONE)) {
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
					discoverableButton.setText(res
							.getString(R.string.device_discoverable));
					discoverableButton.setClickable(false);
					redraw();
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
				case BluetoothAdapter.SCAN_MODE_NONE:
					discoverableButton.setText(res
							.getString(R.string.make_discoverable));
					discoverableButton.setClickable(true);
					redraw();
					break;
				}

			} else if (intent.getAction().equals(
					BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				searchButton.setText(res.getString(R.string.searching));
				searchButton.setClickable(false);
				redraw();
			} else if (intent.getAction().equals(
					BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				searchButton.setText(res.getString(R.string.search_devices));
				searchButton.setClickable(true);
				redraw();
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				newDevices.add(device);

				buttonsBtMenu.add(new TButton(device, width, height, device
						.getName(), 0.4f, 0.15f, 0.3f, vpos, true) {
					@Override
					public void onClick() {
						new ConnectThread((BluetoothDevice) data).start();
					}
				});
				vpos += 18;
				redraw();

			} else if (intent.getAction().equals(
					BluetoothAdapter.ACTION_STATE_CHANGED)) {
				switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.STATE_OFF)) {
				case BluetoothAdapter.STATE_ON:
					bluetoothIsOn();
					break;
				}
			}

		}

	}

	/**
	 * shows buttons for paired devices and starts an {@linkplain AcceptThread}
	 * to deal with incoming connections
	 */
	private void bluetoothIsOn() {
		// pairedDevicesAdapter = new MySimpleAdapter(this);
		// newDevicesAdapter = new MySimpleAdapter(this);

		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();

		buttonsBtMenu = new ArrayList<TButton>();
		buttonsBtMenu.add(discoverableButton);
		buttonsBtMenu.add(searchButton);
		buttonsBtMenu.add(helpButton);
		buttonsBtMenu.add(bluetoothLogo);

		float vpos = 0.1f;
		for (BluetoothDevice device : pairedDevices) {

			buttonsBtMenu.add(new TButton(device, width, height, device
					.getName(), 0.4f, 0.15f, 0.98f, vpos, true) {

				@Override
				public void onClick() {
					new ConnectThread((BluetoothDevice) data).start();
				}
			});
			vpos += 0.18f;
		}
		redraw();

		acceptThread = new AcceptThread();
		acceptThread.start();
	}

	/**
	 * Called when a connection was successful. Starts multiplayer game.
	 * 
	 * @param socket
	 *            The socket
	 * */
	public void manageConnectedSocket(BluetoothSocket socket) {
		final String deviceName = socket.getRemoteDevice().getName();
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
				String connectedTo = getResources().getString(
						R.string.connected_to);
				Toast.makeText(activity, connectedTo + " " + deviceName,
						Toast.LENGTH_SHORT).show();
			}
		});
		// save the socket
		btSocket = socket;

		startGame(LEVEL_MULTIPLAYER);
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
		/** int appVersion */
		public static final byte APP_VERSION = 110;

		private final BluetoothSocket socket;
		private InputStream inStream;
		private OutputStream outStream;

		/**
		 * The length of each message in bytes. Shorter messages will have
		 * zeroes added at the end to match the length
		 */
		public static final int MESSAGE_LENGTH = 60;

		private volatile boolean running;
		private boolean gameIsOver = false;

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
							if (isRunning()) {
								pauseGame();
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
							if (!isRunning()) {
								resumeGame(false);
							}
							break;
						case GAME_QUIT:
							toastFromAnotherThread(R.string.opponent_left);
							connectedThread.cancel();
							// TODO handle this better
							setState(STATE_BTMENU);
							redraw();
							break loop;
						case GAME_OVER:
							scoreOpponent = byteBuffer.getInt();
							targetsShotOpponent = byteBuffer.getInt();
							missesOpponent = byteBuffer.getInt();
							if (connectedThread != null)
								sendGameOver(score, targetsShot, misses);
							gameIsOver = true;
							gameOver(LEVEL_MULTIPLAYER);
							// empty the inStream
							// prevents from reading leftovers when restarting
							// game
							while (inStream.available() > 0)
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

							addTarget(type, id, x, y, v, d);
							break;
						case TARGET_SHOT:
							int targetId = byteBuffer.getInt();
							opponentShot(targetId);
							break;
						case SCORE_UPDATE:
							int score = byteBuffer.getInt();
							scoreOpponent = score;
							break;
						case SCREEN_SIZE:
							// negotiate screen ratio
							// will choose the narrower screen
							int remoteWidth = byteBuffer.getInt();
							int remoteHeight = byteBuffer.getInt();
							final float remoteRatio = (float) remoteHeight
									/ (float) remoteWidth;

							float localRatio = (float) height / (float) width;
							if (remoteRatio > localRatio) {
								// set surfaceview correct resolition
								width = (int) (height / remoteRatio);
							}

							MX = 1.0f;
							MY = (float) height / (float) width;
							screenRatioNegotiated = true;
							break;
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
					toastFromAnotherThread(R.string.connection_lost);
					setState(STATE_BTMENU);
					redraw();
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

	/** Thread used to connect to a remote device */
	private class ConnectThread extends Thread {
		private BluetoothSocket socket;

		public ConnectThread(BluetoothDevice device) {
			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				socket = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		public void run() {
			final String connectingInfo = getResources().getString(
					R.string.connecting_to)
					+ " " + socket.getRemoteDevice().getName() + "…";
			final String address = socket.getRemoteDevice().getAddress();

			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					dialog = new ProgressDialog(activity);
					dialog.setCancelable(false);
					dialog.setCanceledOnTouchOutside(false);
					dialog.setTitle(connectingInfo);
					dialog.setMessage(address);
					dialog.setIcon(android.R.drawable.stat_sys_data_bluetooth);
					dialog.show();
				}
			});

			// Cancel discovery because it will slow down the connection
			bluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				socket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				final String connectFailInfo = getResources().getString(
						R.string.cant_connect)
						+ " " + socket.getRemoteDevice().getName();
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(activity, connectFailInfo,
								Toast.LENGTH_SHORT).show();
						if (dialog != null && dialog.isShowing())
							dialog.dismiss();
					}
				});
				try {
					socket.close();
				} catch (IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			manageConnectedSocket(socket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * A thread that keeps listtening for incoming connections. Once connected,
	 * calls {@link BTFindActivity#manageConnectedSocket(BluetoothSocket)}
	 * 
	 * @author packito
	 * 
	 */
	private class AcceptThread extends Thread {
		private BluetoothServerSocket serverSocket;

		public AcceptThread() {
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				serverSocket = bluetoothAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					manageConnectedSocket(socket);
					try {
						serverSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		private void cancel() {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
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

	public void sendAppVersion() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(5);
		int versionCode = 0;
		try {
			versionCode = activity.getPackageManager().getPackageInfo(
					activity.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		byteBuffer.put(ConnectedThread.APP_VERSION).putInt(versionCode);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);

	}

	public void sendScreenSize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(9);
		byteBuffer.put(ConnectedThread.SCREEN_SIZE).putInt(width)
				.putInt(height);

		byte[] array = byteBuffer.array();
		connectedThread.write(array);
	}

	// END BLUETOOTH ///////////////////////////////////////////////////////

	/**
	 * Load bitmap and sound resources for game
	 */
	private void loadGameMultimedia() {
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

		sounds = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		SOUND_MISS = sounds.load(activity, R.raw.miss, 1);
		SOUND_TARGET_NORMAL = sounds
				.load(activity, R.raw.target_normal_shot, 1);
		SOUND_TARGET_DIAMOND = sounds.load(activity, R.raw.target_diamond_shot,
				1);
		SOUND_TARGET_FLOWER = sounds
				.load(activity, R.raw.target_flower_shot, 1);
	}

	private void drawButtons(Canvas canvas, ArrayList<? extends TButton> buttons) {
		for (TButton button : buttons) {
			button.drawOn(canvas);
		}
	}

	public void redraw() {
		while (!holder.getSurface().isValid())
			;// wait until surface is ready

		Canvas canvas = holder.lockCanvas();

		switch (state) {
		case STATE_LOADING:
			Bitmap loadingBmp = BitmapFactory.decodeResource(res,
					R.drawable.bg_loading);
			canvas.drawBitmap(loadingBmp, new Rect(0, 0, loadingBmp.getWidth(),
					loadingBmp.getHeight()), new Rect(0, 0, width, height),
					null);
			Paint loadingPaint = new Paint();
			loadingPaint.setColor(Color.WHITE);
			loadingPaint.setTypeface(TargetorActivity.getTypeface());
			loadingPaint.setTextSize(height * 0.1f);
			canvas.drawText(res.getString(R.string.loading), width * 0.05f,
					height * 0.9f, loadingPaint);
			break;
		case STATE_MENU:
			canvas.drawBitmap(
					MENU_BG_BMP,
					new Rect(0, 0, MENU_BG_BMP.getWidth(), MENU_BG_BMP
							.getHeight()), new Rect(0, 0, width, height), null);
			drawButtons(canvas, buttonsMenu);
			break;
		case STATE_BTMENU:
			canvas.drawBitmap(MENU_BG_BMP, 0, 0, null);
			drawButtons(canvas, buttonsBtMenu);
			break;
		case STATE_LEVEL_PICKER:
			canvas.drawBitmap(MENU_BG_BMP, 0, 0, null);
			drawButtons(canvas, buttonsPickLevel);
			break;
		case STATE_GAME_PAUSED:
			canvas.drawBitmap(MENU_BG_BMP, 0, 0, null);
			drawButtons(canvas, buttonsGamePaused);
			break;
		case STATE_FINISH:
			canvas.drawBitmap(MENU_BG_BMP, 0, 0, null);
			drawButtons(canvas, buttonsFinish);
			drawButtons(canvas, textsFinish);
			break;
		/**
		 * Draw a new frame (each {@linkplain Target} handles his movement by
		 * himself)
		 */
		case STATE_GAME:
			if (countdown > 0) {// still in countdown mode
				canvas.drawColor(Color.BLACK);
				canvas.drawBitmap(
						MENU_BG_BMP,
						new Rect(0, 0, MENU_BG_BMP.getWidth(), MENU_BG_BMP
								.getHeight()), new Rect(0, 0, width, height),
						null);
				Paint countdownPaint = new Paint();
				countdownPaint.setColor(Color.BLACK);
				countdownPaint.setTextSize(height * 0.6f);
				countdownPaint.setTextAlign(Paint.Align.CENTER);
				canvas.drawText("" + (int) Math.ceil(countdown / 1000.0),
						width / 2, height * 0.8f, countdownPaint);
			} else {// game is running
				// randomly add targets
				if (rnd.nextDouble() < pNormal)
					addTarget(Target.TYPE_NORMAL);
				if (rnd.nextDouble() < pDiamond)
					addTarget(Target.TYPE_DIAMOND);
				if (rnd.nextDouble() < pFlower)
					addTarget(Target.TYPE_FLOWER);

				canvas.drawBitmap(BMP_BG, 0, 0, null);// draw bg
				// targets and temps
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

				// Draw black rectangle to compensate screen sizes
				if (width < getWidth()) {
					canvas.drawRect(new Rect(width + 1, 0, getWidth(), height),
							blackPaint);
				}

				// scores
				String timeleftString = String.format(
						res.getString(R.string.timeleft), timeleft / 1000.0f);
				float textSize = scoreFillPaint.getTextSize();
				canvas.drawText(timeleftString, 10, textSize * 1.2f,
						scoreFillPaint);
				canvas.drawText(timeleftString, 10, textSize * 1.2f,
						scoreStrokePaint);

				String scoreString = String.format("Score %d", score);
				canvas.drawText(scoreString, 10, textSize * 2.4f,
						scoreFillPaint);
				canvas.drawText(scoreString, 10, textSize * 2.4f,
						scoreStrokePaint);

				if (multiplayer) {
					// draw opponent's score in mp
					String oppScoreString = String.format("Opponent score %d",
							scoreOpponent);
					canvas.drawText(oppScoreString, 10, textSize * 3.6f,
							scoreFillPaint);
					canvas.drawText(oppScoreString, 10, textSize * 3.6f,
							scoreStrokePaint);
				} else {
					// draw target score in sp
					String targetScoreString = String.format("Target score %d",
							targetScore);
					canvas.drawText(targetScoreString, 10, textSize * 3.6f,
							scoreFillPaint);
					canvas.drawText(targetScoreString, 10, textSize * 3.6f,
							scoreStrokePaint);
				}
				// end scores
			}// end game drawing
			break;
		}

		holder.unlockCanvasAndPost(canvas);
	}

	/**
	 * handles button click events
	 * 
	 * @param buttons
	 * @param event
	 */
	private void handleButtons(ArrayList<TButton> buttons, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			for (TButton button : buttons) {
				if (button.contains(event.getX(), event.getY())) {
					button.press();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_CANCEL:
			for (TButton button : buttons) {
				if (!button.contains(event.getX(), event.getY())) {
					button.cancel();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			for (TButton button : buttons) {
				if (button.contains(event.getX(), event.getY())) {
					button.release();
				}
			}
		}
		redraw();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (state) {
		case STATE_MENU:
			handleButtons(buttonsMenu, event);
			break;
		case STATE_BTMENU:
			handleButtons(buttonsBtMenu, event);
			break;
		case STATE_GAME_PAUSED:
			handleButtons(buttonsGamePaused, event);
			break;
		case STATE_LEVEL_PICKER:
			handleButtons(buttonsPickLevel, event);
			break;
		case STATE_FINISH:
			handleButtons(buttonsFinish, event);
			break;
		/**
		 * Handling of the touch events, checking targets for collision and
		 * shooting them
		 */
		case STATE_GAME:
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (event.getX() > width || event.getY() > height)
					break;// screen size control
				boolean miss = true;

				float normX = event.getX() / width * MX;
				float normY = event.getY() / height * MY;
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
					TempScore ts = new TempScore(this, event.getX(),
							event.getY(), SCORE_MISS);
					tempScores.add(ts);
					if (multiplayer)
						sendScoreUpdate(score);
				}
				break;
			}
			break;
		}
		return true;
	}

	public void startMusic() {
		switch (state) {
		case STATE_MENU:
		case STATE_BTMENU:
			musicMenu = MediaPlayer.create(activity, R.raw.music_menu);
			musicMenu.setLooping(true);
			musicMenu.seekTo(musicMenuSeek);
			musicMenu.start();
			break;
		case STATE_GAME:
			musicGame = MediaPlayer.create(activity, R.raw.music_game);
			musicGame.setLooping(true);
			musicGame.seekTo(musicGameSeek);
			musicGame.start();
			break;
		}
	}

	/**
	 * Stops playing any music. Performs null checks, safe to call whenever we
	 * want
	 */
	public void stopMusic() {
		if (musicGame != null) {
			// save current playback position for resuming
			musicGameSeek = musicGame.getCurrentPosition();
			musicGame.release();
			musicGame = null;
		}
		if (musicMenu != null) {
			musicMenuSeek = musicMenu.getCurrentPosition();
			musicMenu.release();
			musicMenu = null;
		}
	}

	/**
	 * Changes view state. Handles music playback. TODO change so the p[layback
	 * can continue in mulitple menu screens
	 * 
	 * @param state
	 */
	public void setState(int state) {
		int oldState= this.state;
		this.state = state;
		
		if (!((oldState== STATE_MENU && state == STATE_BTMENU) || oldState == STATE_BTMENU
				&& state == STATE_MENU)){ // dont stop music between menus
			stopMusic();
		if (soundOn)
			startMusic();
		}

	}

	/**
	 * called by {@link TargetorActivity#onBackPressed()}
	 */
	public void onBackPressed() {
		switch (state) {
		case STATE_GAME:
			pauseGame();
			break;
		case STATE_BTMENU:
			if (!btWasOn)
				bluetoothAdapter.disable();
		case STATE_LEVEL_PICKER:
			setState(STATE_MENU);
			redraw();
			break;
		case STATE_MENU:
		case STATE_GAME_PAUSED:
			activity.finish();
			break;
		}
	}

	/**
	 * called by {@link TargetorActivity#onStop()}
	 */
	public void onStop() {
		if (btReceiver != null) {
			activity.unregisterReceiver(btReceiver);
			btReceiver = null;
		}
	}

	/**
	 * called by {@link TargetorActivity#onPause()}
	 */
	public void onPause() {
		stopMusic();
		switch (state) {
		case STATE_GAME:
			pauseGame();
			if (multiplayer && connectedThread != null) {
				sendAppPaused();
				connectedThread.running = false;
				connectedThread = null;
			}
			break;
		}
	}

	/**
	 * called by {@link TargetorActivity#onResume()}
	 */
	public void onResume() {
		if(soundOn)
			startMusic();
		if (multiplayer) {

			if (btSocket == null) {
				Toast.makeText(activity, "Bluetooth socket is null",
						Toast.LENGTH_LONG).show();
				activity.finish();
			}
			connectedThread = new ConnectedThread(btSocket);
			connectedThread.start();
			sendAppResumed();
		}
	}

	// GAME METHODS /////////////////////////////////////////////////////////

	/**
	 * Play a sound
	 * 
	 * @param soundID
	 *            pick from constants in {@linkplain TargetorView}
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
	 * Thread handling redrawing of the frames at constant framerate
	 * {@linkplain #FPS}. Start it using {@linkplain TargetorView#startThread()}
	 * . Keeps running until {@linkplain #running} turns false.( this is
	 * achieved by calling {@link TargetorView#stopThread()} )
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

			while (running) {
				if (!holder.getSurface().isValid())
					continue;// surface is still preparing

				// handle the times
				startTime = System.currentTimeMillis();
				if (lastTime > 0 && countdown > 0) { // we are in countdown mode
					countdown -= startTime - lastTime;
				} else if (lastTime > 0) {// if the game has not been paused
					timeleft -= startTime - lastTime;
					if (timeleft < 0) {
						running = false;
						gameOver(level);
					}
				}
				lastTime = startTime;

				// do the drawing
				synchronized (holder) {
					try {
						redraw();
					} catch (NullPointerException e) {
						e.printStackTrace();
						Log.d("GameView", "canvas is null");
					} finally {
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
			stopMusic();
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

	/**
	 * The game has ended
	 */
	private void gameOver(int level) {
		setState(STATE_FINISH);

		// just in case width was different because of the opponent
		width = getWidth();

		if (multiplayer) {
			stopThread();

			// multipaleyr finish screen
			int titleResid = score > scoreOpponent ? R.string.win
					: (score == scoreOpponent ? R.string.tie : R.string.lose);
			TText finishTitle = new TText(width, height,
					res.getString(titleResid), TText.TITLE_W, TText.TITLE_H,
					TButton.CENTER, TButton.MIN, false);

			float vpos = 0.25f;
			TText meText = new TText(width, height, res.getString(R.string.me),
					TText.TEXT_W, TText.TEXT_H, TButton.CENTER, vpos, false);
			TText opponentText = new TText(width, height, btSocket
					.getRemoteDevice().getName(), TText.TEXT_W, TText.TEXT_H,
					TButton.MAX, vpos, false);

			vpos = 0.48f;
			TText scoreText = new TText(width, height,
					res.getString(R.string.score), TText.TEXT_W, TText.TEXT_H,
					TButton.MIN, vpos, false);
			TText scoreMe = new TText(width, height, "" + score, TText.TEXT_W,
					TText.TEXT_H, TButton.CENTER, vpos, false);
			TText scoreOpp = new TText(width, height, "" + scoreOpponent,
					TText.TEXT_W, TText.TEXT_H, TButton.MAX, vpos, false);

			vpos = 0.72f;
			TText accText = new TText(width, height,
					res.getString(R.string.accuracy), TText.TEXT_W,
					TText.TEXT_H, TButton.MIN, vpos, false);
			TText accMe = new TText(width, height, ""
					+ getAccString(targetsShot, misses), TText.TEXT_W,
					TText.TEXT_H, TButton.CENTER, vpos, false);
			TText accOpp = new TText(width, height, ""
					+ getAccString(targetsShotOpponent, missesOpponent),
					TText.TEXT_W, TText.TEXT_H, TButton.MAX, vpos, false);

			buttonsFinish.remove(nextButton);

			textsFinish = new ArrayList<TText>();
			textsFinish.add(finishTitle);
			textsFinish.add(meText);
			textsFinish.add(opponentText);
			textsFinish.add(scoreText);
			textsFinish.add(scoreMe);
			textsFinish.add(scoreOpp);
			textsFinish.add(accText);
			textsFinish.add(accMe);
			textsFinish.add(accOpp);

			if (connectedThread != null) {
				sendGameOver(score, targetsShot, misses);
				disconnect();
			}
			redraw();

			TargetorDatabase db = new TargetorDatabase(activity);
			db.open();
			db.insertHistoryMultiplayer(score, targetsShot, misses,
					scoreOpponent, btSocket.getRemoteDevice().getAddress());
			db.close();

		} else {// singleplayer finish screen
			String titleString;
			if (score < calcScore(level)) {// failure
				titleString = res.getString(R.string.title_failure);
				buttonsFinish.remove(nextButton);
			} else {// success
				titleString = getResources().getString(R.string.title_success);
				if (!buttonsFinish.contains(nextButton))
					buttonsFinish.add(nextButton);
			}

			TText finishTitle = new TText(width, height, titleString + " "
					+ level, TText.TITLE_W, TText.TITLE_H, TButton.CENTER,
					TButton.MIN, false);
			TText scoreText = new TText(width, height,
					res.getText(R.string.score) + " " + score + "/"
							+ targetScore, TButton.MAX, TText.TEXT_H,
					TButton.CENTER, 0.4f, false);

			String acc = getAccString(targetsShot, misses);

			TText accText = new TText(width, height,
					res.getText(R.string.accuracy) + " " + acc, TButton.MAX,
					TText.TEXT_H, TButton.CENTER, 0.55f, false);
			textsFinish = new ArrayList<TText>();
			textsFinish.add(finishTitle);
			textsFinish.add(scoreText);
			textsFinish.add(accText);

			redraw(); // end singleplayer finish screen

			TargetorDatabase db = new TargetorDatabase(activity);

			db.open();
			db.insertHistorySingleplayer(score, targetsShot, misses, level);
			db.close();
		}
		redraw();
	}

	private String getAccString(int targetsShot, int misses) {
		double accuracy = (100.0 * targetsShot) / (targetsShot + misses);
		if (accuracy == Double.NaN)
			accuracy = 0.0;
		String acc = String.format("%.1f%%", accuracy);
		return acc;
	}

	/**
	 * stop the current {@link ConnectedThread}
	 * 
	 * @return true if the thread existed, false otherwise
	 */
	private boolean disconnect() {
		boolean ritrnValju = false;
		if (connectedThread != null) {
			ritrnValju = true;
			connectedThread.running = false;
			connectedThread = null;
		}
		return ritrnValju;
	}

	/**
	 * start new game
	 * 
	 * @param level
	 *            level number, 0 for multiplayer
	 */
	public void startGame(int level) {
		MX = 1.0f;
		MY = (float) height / (float) width;

		musicGameSeek = 0;
		musicMenuSeek = 0;

		score = 0;
		targets.clear();
		temps.clear();
		tempScores.clear();

		// 3,2,1, start
		countdown = 3000;

		timeleft = calcTime(level);
		targetScore = calcScore(level);
		pNormal = calcNormal(level);
		pDiamond = calcDiamond(level);
		pFlower = calcFlower(level);
		this.level = level;

		multiplayer = level == LEVEL_MULTIPLAYER;

		if (multiplayer) {
			// TODO countdown
			opponentReady = true;

			connectedThread = new ConnectedThread(btSocket);
			connectedThread.start();

			sendAppVersion();
			sendScreenSize();
		}

		resumeGame(false);
	}

	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return the time of game for the current level, in milliseconds
	 */
	public static int calcTime(int lvl) {
		if (lvl == 0)
			return 60000;
		else
			return 5000 * (lvl + 4);
	}

	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return the target for the current level, has no effect in multiplayer
	 */
	public static int calcScore(int lvl) {
		if (lvl == 0)
			return 0;
		else
			return 25 * (lvl + 4) + (int) Math.exp(lvl / 3.0);
	}

	/**
	 * 
	 * @param lvl
	 *            the current game level (0 for multiplayer)
	 * @return probability that a Normal target is created each frame
	 */
	public static double calcNormal(int lvl) {
		if (lvl == 0)
			return 0.02;
		else
			return 1.0 / 12.0 + lvl * lvl / 2500.0;
	}

	/** @see #calcNormal(int) */
	public static double calcDiamond(int lvl) {
		if (lvl == 0)
			return 0.002;
		else
			return (lvl - 3.0) / 200.0;
	}

	/** @see #calcNormal(int) */
	public static double calcFlower(int lvl) {
		if (lvl == 0)
			return 0.005;
		else
			return Math.pow(lvl, 1.5) / 600.0;
	}

	/**
	 * Resume the game
	 * 
	 * @param notify
	 *            indicates whether or not to {@link #sendAppResumed()}. Has no
	 *            effect in singleplayer
	 */
	public void resumeGame(boolean notify) {
		// resume only if opponent is ready in multiplayer
		if (activity.isOnTop() && (opponentReady || !multiplayer)) {
			// block from resuming if opponent just paused
			if (!opponentPausedOneSecondAgoDamnThisIsALongVariablename) {
				if (multiplayer && notify) {
					sendGameResumed();
				}
				setState(STATE_GAME);
				startThread();
			}
		} else {
			toastFromAnotherThread(R.string.opponent_not_ready);
		}

	}

	public void pauseGame() {
		stopThread();
		if (multiplayer && connectedThread != null) {
			sendGamePaused();
		}
		setState(STATE_GAME_PAUSED);
		redraw();
	}

	// SurfaceHolder.Callback methods //////////////////////////////////////

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format,
			final int width, final int height) {
		Log.d(TAG, "surfaceChanged");
		if (MX == 0.0f && MY == 0.0f) { // call only first time
			MX = 1.0f;
			MY = (float) height / (float) width;
		}
		scoreFillPaint.setTextSize(width / 20.0f);
		scoreStrokePaint.setTextSize(width / 20.0f);
		scoreStrokePaint.setStrokeWidth(width / 500.0f);
		redraw();
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
