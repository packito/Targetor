package cz.packito.targetor;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

public class LevelPickerActivity extends Activity implements OnClickListener {
	private int currentLevel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_level_picker);
		TargetorDatabase db = new TargetorDatabase(this);
		db.open();
		currentLevel = db.getLevel();
		db.close();
		LinearLayout levelsLayout = (LinearLayout) findViewById(R.id.levels_layout);
		Button bContinue=(Button) findViewById(R.id.level_continue);
		bContinue.setText(bContinue.getText().toString()+currentLevel);

		LinearLayout layout = ll();
		for (int i = 1; i <= TargetorApplication.LEVELS; i++) {
			Button b = new Button(this);
			android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
			b.setLayoutParams(params);
			b.setText(Integer.toString(i));
			b.setEnabled(i <= currentLevel);
			b.setOnClickListener(this);
			layout.addView(b);
			if (i % 5 == 0) {
				levelsLayout.addView(layout);
				layout = ll();
			}
		}
		levelsLayout.addView(layout);
	}

	private LinearLayout ll() {
		LinearLayout ll = new LinearLayout(this);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(params);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		return ll;
	}

	@Override
	public void onClick(View v) {
		Button b = (Button) v;
		int lvl = Integer.parseInt(b.getText().toString());
		startGame(lvl);
	}

	private void startGame(int lvl) {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, false);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_LEVEL_ID, lvl);
		startActivity(intent);
		finish();
	}
public void startCurrentLevel(View v){
	startGame(currentLevel);
}
}
