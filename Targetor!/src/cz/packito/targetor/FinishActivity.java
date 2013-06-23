package cz.packito.targetor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class FinishActivity extends Activity {

	private boolean multiplayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finish);
		TextView tvTitle=(TextView)findViewById(R.id.finish_title);
		TextView tvScore=(TextView)findViewById(R.id.finish_score);
		
		TargetorApplication.changeTypeface(this, R.id.finish_title, R.id.finish_score);
		
		Intent i=getIntent();

		int score= i.getIntExtra(TargetorApplication.TARGETOR_EXTRA_SCORE, 0);
		 multiplayer= i.getBooleanExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, false);
		boolean won= i.getBooleanExtra(TargetorApplication.TARGETOR_EXTRA_WIN, false);
		
		tvScore.append(Integer.toString(score));
		if(multiplayer){
			int titleStringId=won?R.string.win:R.string.lose;
			tvTitle.setText(titleStringId);
		}
	}
	
	/** Play another game. If multiplayer, plays with the same player */
	public void again(View v){
		Intent intent= new Intent(this, GameActivity.class);
		intent.putExtra(TargetorApplication.TARGETOR_EXTRA_MULTIPLAYER, multiplayer);
		startActivity(intent);
		finish();
	}
	
	public void exit(View v){
		finish();
	}
}
