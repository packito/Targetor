package cz.packito.targetor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BTFindActivity extends Activity implements OnItemClickListener {

	private static final int REQUEST_ENABLE_BT = 13333337;
	public static final UUID MY_UUID = UUID
			.fromString("c3f8407d-f3b7-45d8-a1a2-3965a58305e7");
	public static final String NAME = "Targetor";

	private MySimpleAdapter pairedDevicesAdapter, newDevicesAdapter;
	private ListView pairedDevicesList, newDevicesList;
	private BluetoothAdapter bluetoothAdapter;
	private Button discoverableButton;
	private MyReceiver receiver;
	private AcceptThread acceptThread;

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
				switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
						BluetoothAdapter.SCAN_MODE_NONE)) {
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
					discoverableButton.setText(R.string.device_discoverable);
					discoverableButton.setEnabled(false);
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
					discoverableButton.setText(R.string.make_discoverable);
					discoverableButton.setEnabled(true);
					break;
				case BluetoothAdapter.SCAN_MODE_NONE:
					discoverableButton.setText(R.string.make_discoverable);
					discoverableButton.setEnabled(true);
					Toast.makeText(BTFindActivity.this, "scan mode is none",
							Toast.LENGTH_SHORT).show();
					break;
				}

			} else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				// a new bluetooth device has been found, add it to list
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				newDevicesAdapter.add(device.getName(), device.getAddress());
				newDevicesList.setAdapter(newDevicesAdapter);
			}
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btfind);
		TargetorApplication.changeTypeface(this, R.id.button_discoverable,
				R.id.button_search, R.id.tv_paired_devices);

		pairedDevicesList = (ListView) findViewById(R.id.list_paired_devices);
		newDevicesList = (ListView) findViewById(R.id.list_new_devices);

		pairedDevicesList.setOnItemClickListener(this);
		newDevicesList.setOnItemClickListener(this);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(this, R.string.bluetooth_not_supported,
					Toast.LENGTH_LONG).show();
			finish();
		}

		// register reciever for discoverability changes
		receiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter);
		discoverableButton = (Button) findViewById(R.id.button_discoverable);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// request turning on Bluetooth
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			bluetoothIsOn();
		}
	}

	@Override
	protected void onPause() {
		acceptThread.cancel();
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == RESULT_OK) {
				bluetoothIsOn();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.cant_play_no_bt,
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	/**
	 * adds {@link MySimpleAdapter}s to {@link ListView}s. The
	 * {@link #pairedDevicesList} is filled with paired devices and starts an
	 * {@linkplain AcceptThread} to deal with incoming connections
	 */
	private void bluetoothIsOn() {
		pairedDevicesAdapter = new MySimpleAdapter(this);
		newDevicesAdapter = new MySimpleAdapter(this);

		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();

		for (BluetoothDevice device : pairedDevices) {
			pairedDevicesAdapter.add(device.getName(), device.getAddress());
		}

		pairedDevicesList.setAdapter(pairedDevicesAdapter);
		newDevicesList.setAdapter(newDevicesAdapter);

		acceptThread = new AcceptThread();
		acceptThread.start();
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

	// private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
	//
	// private ProgressDialog dialog;
	// private BluetoothSocket socket;
	// private BluetoothDevice remoteDevice;
	//
	// public ConnectTask(BluetoothDevice remoteDevice) {
	// this.remoteDevice = remoteDevice;
	// }
	//
	// @Override
	// protected void onPreExecute() {
	// dialog = new ProgressDialog(BTFindActivity.this);
	// dialog.setCancelable(false);
	// dialog.setCanceledOnTouchOutside(false);
	// String connectingTo = getResources().getString(
	// R.string.connecting_to);
	// dialog.setTitle(connectingTo + " " + remoteDevice.getName() + "…");
	// dialog.show();
	// }
	//
	// /**
	// * attempt connection to {@link #remoteDevice}.
	// *
	// * @param params
	// * has no effect
	// * @return true if the connaction was successful, false otherwise
	// */
	// @Override
	// protected Boolean doInBackground(Void... params) {
	// try {
	// socket = remoteDevice
	// .createRfcommSocketToServiceRecord(MY_UUID);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// // Cancel discovery because it will slow down the connection
	// bluetoothAdapter.cancelDiscovery();
	//
	// try {
	// // Connect the device through the socket. This will block
	// // until it succeeds or throws an exception
	// socket.connect();
	// } catch (IOException connectException) {
	// // Unable to connect; close the socket and get out
	// try {
	// socket.close();
	// } catch (IOException closeException) {
	// }
	// return false;
	// }
	//
	// // Do work to manage the connection (in a separate thread)
	// manageConnectedSocket(socket);
	//
	// return true;
	// }
	//
	// @Override
	// protected void onPostExecute(Boolean result) {
	// dialog.dismiss();
	// if (!result) {
	// // connection unsuccessful, show toast
	// String cantConnect = getResources().getString(
	// R.string.cant_connect);
	// Toast.makeText(BTFindActivity.this,
	// cantConnect + " " + remoteDevice.getName(),
	// Toast.LENGTH_SHORT).show();
	// }
	// }
	//
	// }

	protected ProgressDialog dialog;

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
			// TODO rewrite this using Handler

			final String connectingInfo = getResources().getString(
					R.string.connecting_to)
					+ " " + socket.getRemoteDevice().getName() + "…";
			BTFindActivity.this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					dialog = new ProgressDialog(BTFindActivity.this);
					dialog.setCancelable(false);
					dialog.setCanceledOnTouchOutside(false);
					dialog.setTitle(connectingInfo);
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
				BTFindActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(BTFindActivity.this, connectFailInfo,
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
	 * Called when a connection was successful. Starts multiplayer game.
	 * 
	 * @param socket
	 *            The socket
	 * */
	public void manageConnectedSocket(BluetoothSocket socket) {
		final String deviceName = socket.getRemoteDevice().getName();
		BTFindActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
				String connectedTo = getResources().getString(
						R.string.connected_to);
				Toast.makeText(BTFindActivity.this,
						connectedTo + " " + deviceName, Toast.LENGTH_SHORT)
						.show();
			}
		});
		// save the socket to TargetorApplication
		((TargetorApplication) getApplication()).btSocket = socket;
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, true);
		startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		TextView tvAddress = (TextView) view.findViewById(android.R.id.text2);
		String address = tvAddress.getText().toString();
		// not working dunno why
		// new ConnectTask(bluetoothAdapter.getRemoteDevice(address)).execute();
		new ConnectThread(bluetoothAdapter.getRemoteDevice(address)).start();
	}

	/**
	 * Called by clicking {@link R.id#button_discoverable}. Requests
	 * discoverability
	 */
	public void makeDiscoverable(View v) {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivity(discoverableIntent);
	}

	/** Called by clicking {@link R.id#button_search}. */
	public void searchForDevices(View v) {
		bluetoothAdapter.startDiscovery();
		Toast.makeText(this, R.string.searching, Toast.LENGTH_LONG).show();
	}

	/** Show bluetooth help dialog */
	public void help(View v) {
		// TODO show bluetooth help dialog
		Toast.makeText(this, "TODO show bluetooth help dialog",
				Toast.LENGTH_SHORT).show();
	}

}

/**
 * {@link ListAdapter} class for {@link ListView} with
 * {@linkplain android.R.layout#simple_list_item_2} (two {@link TextView}s in
 * each list item)
 * 
 * @author packito
 * @see http://stackoverflow.com/questions/7916834/
 */
class MySimpleAdapter extends SimpleAdapter {
	private static final String KEY_NAME = "name";
	private static final String KEY_ADDRESS = "address";

	private static final String[] from = new String[] { KEY_NAME, KEY_ADDRESS };
	private static final int[] to = new int[] { android.R.id.text1,
			android.R.id.text2 };

	private final ArrayList<HashMap<String, String>> data;

	public MySimpleAdapter(Context context) {
		this(context, new ArrayList<HashMap<String, String>>(),
				android.R.layout.simple_list_item_2, from, to);
	}

	private MySimpleAdapter(Context context,
			ArrayList<HashMap<String, String>> data, int resource,
			String[] from, int[] to) {
		super(context, data, resource, from, to);
		this.data = (ArrayList<HashMap<String, String>>) data;
	}

	/**
	 * Add a new bluetooth device to the list. Need to call
	 * {@link ListView#setAdapter(ListAdapter)} after calling this method to
	 * ensure the {@link ListView} is updated.
	 * 
	 * @param name
	 *            device name to add
	 * @param address
	 *            device address to add
	 */
	void add(String name, String address) {
		HashMap<String, String> datum = new HashMap<String, String>(2);
		datum.put(KEY_NAME, name);
		datum.put(KEY_ADDRESS, address);
		data.add(datum);
	}
}