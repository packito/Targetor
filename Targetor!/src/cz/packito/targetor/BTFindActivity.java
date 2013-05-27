package cz.packito.targetor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class BTFindActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btfind);
		TargetorApplication.changeTypeface(this, R.id.button_discoverable,R.id.button_search,R.id.tv_paired_devices);
	}
	
	public void help(View v){
		//TODO show bluetooth help dialog
		Toast.makeText(this, "TODO show bluetooth help dialog", Toast.LENGTH_SHORT).show();
	}
}
