package edu.ucsb.cs290.touch.to.text;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AuthActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
    }
    
    public void submitPassword(View view) {
    	EditText e = (EditText) findViewById(R.id.enter_password);
    	setResult(RESULT_OK, new Intent().putExtra("edu.ucsb.cs290.touch.to.text.password", e.getText().toString()));
    	if(e.getText().length() ==0) {
    		return;
    	}
    	finish();
    }
}
