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
import android.view.View.OnClickListener;
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
		OnClickListener{

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
		soundToggle.setOnClickListener(this);

	}


	@Override
	protected void onStart() {
		super.onStart();
		soundOn = preferences.getBoolean(
				TargetorApplication.TARGETOR_KEY_SOUND_ON, true);
		soundToggle.setChecked(soundOn);
		if (soundOn)
			startMusic();
	}

	@Override
	protected void onStop() {
		stopMusic();
		super.onStop();
	}

	private void startMusic() {
		if (music == null) {
			music = MediaPlayer.create(this, R.raw.music_menu);
			music.setLooping(true);
			music.start();
		}
	}

	private void stopMusic() {
		if (music != null) {
			music.release();
			music = null;
		}
	}


	public void startSingleplayer(View v) {
		Intent intent = new Intent(this, LevelPickerActivity.class);
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
	 * quit the app
	 * 
	 * @param v
	 *            has no effect
	 */

	public void quit(View v) {
		finish();
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
	public void onClick(View buttonView) {
		switch (buttonView.getId()) {
		case R.id.menu_sound:
			soundOn = ((Checkable)buttonView).isChecked();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(TargetorApplication.TARGETOR_KEY_SOUND_ON,
					soundOn);
			editor.commit();
			if (soundOn) {
				startMusic();
			} else {
				stopMusic();
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
