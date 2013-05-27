package cz.packito.targetor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		TargetorApplication.changeTypeface(this, R.id.button_singleplayer,
				R.id.button_multiplayer);
	}

	public void startSingleplayer(View v) {
		// TODO start singleplayer game
	}

	public void startMultiplayer(View v) {
		startActivity(new Intent(this, BTFindActivity.class));
	}

	/**
	 * share this app
	 * 
	 * @param v
	 *            has no effect
	 */

	public void share(View v) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		Resources r = getResources();

		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT,
				r.getString(R.string.share_subject));
		shareIntent.putExtra(Intent.EXTRA_TEXT,
				r.getString(R.string.share_text));

		startActivity(Intent.createChooser(shareIntent,
				r.getString(R.string.share_chooser)));
	}

	/**
	 * Prompt to quit the app
	 * 
	 * @param v
	 *            has no effect
	 */

	public void quit(View v) {
		AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
		quitDialog
				.setTitle(R.string.are_you_sure)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						});
		quitDialog.show();
	}

	@Override
	public void onBackPressed() {
		quit(null);
	}

	/**
	 * show the app info screen
	 * 
	 * @param v
	 */
	public void info(View v) {
		startActivity(new Intent(this, InfoActivity.class));
	}
}
