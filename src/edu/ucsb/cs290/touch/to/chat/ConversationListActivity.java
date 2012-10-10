package edu.ucsb.cs290.touch.to.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ConversationListActivity extends FragmentActivity
        implements ConversationListFragment.Callbacks {

    private boolean mTwoPane;
    private boolean loggedIn = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
        	startActivityForResult(new Intent(getApplicationContext(),AuthActivity.class), 100);
        }
        setContentView(R.layout.activity_conversation_list);

        if (findViewById(R.id.conversation_detail_container) != null) {
            mTwoPane = true;
            ((ConversationListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.conversation_list))
                    .setActivateOnItemClick(true);
        }
    }

    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(ConversationDetailFragment.ARG_ITEM_ID, id);
            ConversationDetailFragment fragment = new ConversationDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.conversation_detail_container, fragment)
                    .commit();

        } else {
            Intent detailIntent = new Intent(this, ConversationDetailActivity.class);
            detailIntent.putExtra(ConversationDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode == 100) {
    		String derp = data.getExtras().getString("password");
    		loggedIn = true;
    		//TODO danny do something with this data
    	}
    }    
    
}
