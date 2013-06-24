package cz.packito.targetor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class InfoActivity extends Activity {

	public static final Uri SOUNDCLOUD_URI = Uri
			.parse("https://soundcloud.com/aj-novobilski");
	public static final String EMAIL_ADDRESS = "packito1@gmail.com";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);

		// TODO info about app
	}

	/** Send email to developer */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressLint("ServiceCast")
	@SuppressWarnings("deprecation")
	public void sendEmail(View v) {
		// / open only email apps
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Targetor!");
		emailIntent.setData(Uri.parse("mailto:"+EMAIL_ADDRESS));
		
		
		// check if email client is installed
		if (getPackageManager().queryIntentActivities(emailIntent, 0).size() > 0) {
		startActivity(Intent.createChooser(emailIntent, getResources()
		.getString(R.string.email_chooser)));
		} else {
			// copy email to clipboard
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(EMAIL_ADDRESS);
			} else {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData
						.newPlainText("Email", EMAIL_ADDRESS);
				clipboard.setPrimaryClip(clip);
			}
			Toast.makeText(this, R.string.no_email_client, Toast.LENGTH_LONG).show();
		}
	}

	/** open music author's Soundcloud */
	public void openSoundCloud(View v) {
		Intent intent = new Intent(Intent.ACTION_VIEW, SOUNDCLOUD_URI);
		startActivity(intent);
	}
}
