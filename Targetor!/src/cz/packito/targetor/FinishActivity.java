package cz.packito.targetor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FinishActivity extends Activity {

	private boolean multiplayer;
	private int levelId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finish);
		TextView tvTitle = (TextView) findViewById(R.id.finish_title);
		TableLayout table = (TableLayout) findViewById(R.id.finish_table);
		View next = findViewById(R.id.next_lvl);

		TargetorApplication.changeTypeface(this, R.id.finish_title);

		Intent i = getIntent();

		multiplayer = i.getBooleanExtra(
				TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, false);
		int score = i.getIntExtra(TargetorApplication.TARGETOR_EXTRA_SCORE, 0);
		int targetsShot = i.getIntExtra(
				TargetorApplication.TARGETOR_EXTRA_TARGETS_SHOT, 0);
		int misses = i
				.getIntExtra(TargetorApplication.TARGETOR_EXTRA_MISSES, 0);

		int scoreOpponent = i.getIntExtra(
				TargetorApplication.TARGETOR_EXTRA_OPPONENT_SCORE, 0);
		int targetsShotOpponent = i.getIntExtra(
				TargetorApplication.TARGETOR_EXTRA_OPPONENT_TARGETS_SHOT, 0);
		int missesOpponent = i.getIntExtra(
				TargetorApplication.TARGETOR_EXTRA_OPPONENT_MISSES, 0);

		levelId = i.getIntExtra(TargetorApplication.TARGETOR_EXTRA_LEVEL_ID, 0);

		if (multiplayer) {
			next.setVisibility(View.GONE);
			int titleStringId = R.string.tie;
			if (score < scoreOpponent)
				titleStringId = R.string.lose;
			else if (score > scoreOpponent)
				titleStringId = R.string.win;

			tvTitle.setText(titleStringId);

			BluetoothDevice remoteDevice = ((TargetorApplication) getApplication()).btSocket
					.getRemoteDevice();
			String opponentName = remoteDevice.getName();
			String opponentAddress = remoteDevice.getAddress();

			// results view in multiplayer
			TableRow trName = new TableRow(this);
			trName.setGravity(Gravity.CENTER);
			trName.addView(tv(""));
			trName.addView(tv(R.string.me));
			trName.addView(tv(opponentName));
			table.addView(trName);

			TableRow trScore = new TableRow(this);
			trScore.setGravity(Gravity.CENTER);
			trScore.addView(tv(R.string.score));
			trScore.addView(tv(Integer.toString(score)));
			trScore.addView(tv(Integer.toString(scoreOpponent)));
			table.addView(trScore);
			
//			TableRow trTargetsShot= new TableRow(this);
//			trTargetsShot.addView(tv(R.string.score));
//			trTargetsShot.addView(tv(Integer.toString(score)));
//			trTargetsShot.addView(tv(Integer.toString(scoreOpponent)));
//			table.addView(trTargetsShot);

			TableRow trAccuracy = new TableRow(this);
			trAccuracy.setGravity(Gravity.CENTER);
			trAccuracy.addView(tv(R.string.accuracy));
			String acc = String.format("%.1f%%", (100.0* targetsShot)
					/ (targetsShot + misses));
			String accOpponent = String.format("%.1f%%", (100.0* targetsShotOpponent)
					/ (targetsShotOpponent + missesOpponent));
			trAccuracy.addView(tv(acc));
			trAccuracy.addView(tv(accOpponent));
			table.addView(trAccuracy);
			
			// TODO writing to db
			
			//TODO total wins vs loses (database)

		} else {
			// results view in singleplayer

		}
	}

	/** generate a TextView for the results table */
	private TextView tv(String text) {
		TextView textView = new TextView(this);
		textView.setText(text);
		TargetorApplication.changeTypeface(this, textView);
		textView.setTextColor(Color.BLACK);
		textView.setTextSize(24);
		textView.setGravity(Gravity.CENTER);

		return textView;
	}

	/** generate a TextView for the results table */
	private TextView tv(int resid) {
		TextView textView = new TextView(this);
		textView.setText(resid);
		TargetorApplication.changeTypeface(this, textView);
		textView.setTextColor(Color.BLACK);
		textView.setTextSize(24);
		textView.setGravity(Gravity.CENTER);

		return textView;
	}

	/** Play another game. If multiplayer, plays with the same player */
	public void again(View v) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER,
				multiplayer);

		if (!multiplayer)
			intent.putExtra(TargetorApplication.TARGETOR_EXTRA_LEVEL_ID,
					levelId);

		startActivity(intent);
		finish();
	}

	/** TODO continue to next level in singleplayer */
	public void next(View v) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER,
				multiplayer);

		// TODO check for max level
		// intent.putExtra(TargetorApplication.TARGETOR_EXTRA_LEVEL_ID,
		// levelId+1);

		startActivity(intent);
		finish();
	}

	public void exit(View v) {
		finish();
	}
}
