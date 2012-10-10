package edu.ucsb.cs290.touch.to.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class AuthActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_auth, menu);
        return true;
    }
    
    public void submitPassword(View view) {
    	EditText e = (EditText) findViewById(R.id.enter_password);
    	setResult(RESULT_OK, new Intent().putExtra("password", e.getText().toString()));
    	finish();
    }
}
