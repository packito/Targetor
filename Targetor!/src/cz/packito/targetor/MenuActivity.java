package cz.packito.targetor;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ToggleButton;

/**
 * The activity the user sees after launching Targetor!
 * 
 * @author packito
 * 
 */

public class MenuActivity extends Activity implements View.OnTouchListener,
		OnCheckedChangeListener {

	private SharedPreferences preferences;
	private MediaPlayer music;
	private boolean soundOn;
	private ToggleButton soundToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		preferences = getSharedPreferences(
				TargetorApplication.SHARED_PREFERENCES, MODE_PRIVATE);
		TargetorApplication.changeTypeface(this, R.id.text_singleplayer,
				R.id.text_multiplayer);

		// ontouchlistener for changing menu button targets
		findViewById(R.id.layout_singleplayer).setOnTouchListener(this);
		findViewById(R.id.layout_multiplayer).setOnTouchListener(this);

		// register sound preference changes
		soundToggle = (ToggleButton) findViewById(R.id.menu_sound);
		soundToggle.setOnCheckedChangeListener(this);

		music = MediaPlayer.create(this, R.raw.music_menu);
		music.setLooping(true);
		try {
			music.prepare();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO fix playback, coontinue in other menu activities

	@Override
	protected void onResume() {
		super.onResume();
		soundOn = preferences.getBoolean(
				TargetorApplication.TARGETOR_KEY_SOUND_ON, true);
		soundToggle.setChecked(soundOn);
		if (soundOn) {
			music.start();
		}
	}

	@Override
	protected void onPause() {
		if (music.isPlaying()) {
			music.pause();
		}
		super.onPause();
	}

	// fix playback end

	@Override
	protected void onDestroy() {
		music.release();
		super.onDestroy();
	}

	public void startSingleplayer(View v) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, false);
		startActivity(intent);
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

	/**
	 * Called by toggling the sound in menu. Writes to SharedPreferences and
	 * plays/stops music playback
	 */
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.menu_sound:
			soundOn = isChecked;
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(TargetorApplication.TARGETOR_KEY_SOUND_ON,
					soundOn);
			editor.commit();
			if (soundOn) {
				music.start();
			} else if (music.isPlaying()) {
				music.pause();
			}
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		// handle changing images single,multi player
		switch (v.getId()) {
		case R.id.layout_singleplayer:
			switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				((ImageView) findViewById(R.id.image_singleplayer))
						.setImageResource(R.drawable.target_normal);
				break;
			case MotionEvent.ACTION_DOWN:
				((ImageView) findViewById(R.id.image_singleplayer))
						.setImageResource(R.drawable.target_normal_broken);
				break;
			}
			break;
		case R.id.layout_multiplayer:
			switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				((ImageView) findViewById(R.id.image_multiplayer))
						.setImageResource(R.drawable.target_bluetooth);
				break;
			case MotionEvent.ACTION_DOWN:
				((ImageView) findViewById(R.id.image_multiplayer))
						.setImageResource(R.drawable.target_bluetooth_broken);
				break;
			}
			break;
		}
		return false;
	}

}
