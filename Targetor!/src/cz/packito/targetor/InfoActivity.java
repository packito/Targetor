package cz.packito.targetor;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class InfoActivity extends Activity {

	public static final Uri SOUNDCLOUD_URI = Uri
			.parse("https://soundcloud.com/aj-novobilski");
	public static final String[] EMAIL_ADDRESS = { "packito1@gmail.com" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);

		// TODO info about app
	}

	/** Send email to developer */
	public void sendEmail(View v) {
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, EMAIL_ADDRESS);
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Targetor!");
		emailIntent.setType("text/plain");
		startActivity(Intent.createChooser(emailIntent,
				getResources().getString(R.string.email_chooser)));
	}

	/** open music author's Soundcloud */
	public void openSoundCloud(View v) {
		Intent intent = new Intent(Intent.ACTION_VIEW, SOUNDCLOUD_URI);
		startActivity(intent);
	}
}
