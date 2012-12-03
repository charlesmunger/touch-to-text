package edu.ucsb.cs290.touch.to.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import edu.ucsb.cs290.touch.to.chat.crypto.Contact;
import edu.ucsb.cs290.touch.to.chat.crypto.SealablePublicKey;

public class ConversationListActivity extends KeyActivity implements
ConversationListFragment.Callbacks {
	private static final long NEW_CONTACT_ID = 0;
	private boolean mTwoPane;
	private boolean fragmentInit = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation_list);

		if (findViewById(R.id.conversation_detail_container) != null) {
			mTwoPane = true;
			((ConversationListFragment) getFragmentManager()
					.findFragmentById(R.id.conversation_list))
					.setActivateOnItemClick(true);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_conversation_view, menu);
		return true;
	}

	public boolean addContact(MenuItem item) {
		startActivityForResult(new Intent(getApplicationContext(),
				NewContactActivity.class), 101);
		return true;
	}
	
	public boolean wipeDB(MenuItem item) {
		return getInstance().wipeDB();
	}

	@Override
	public void onItemSelected(Contact id) {
		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putSerializable(ConversationDetailFragment.ARG_ITEM_ID, id);
			final ConversationDetailFragment fragment = new ConversationDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
			.replace(R.id.conversation_detail_container, fragment)
			.commit();
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					fragment.onServiceConnected();
					fragmentInit = true;
				}
			});
			
		} else {
			Intent detailIntent = new Intent(this,
					ConversationDetailActivity.class);
			detailIntent.putExtra(ConversationDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
		case 101:
			if (resultCode == Activity.RESULT_CANCELED) {
				return;
			}
			System.out.println("Contact added");
			// String long SealablePublicKey
			// name timeAdded (irrelevant/bad to store) key+signedsecret
			String name = data.getExtras().getString("name");
			Long dateTime = data.getExtras().getLong("date");
			SealablePublicKey keyAndToken = (SealablePublicKey) data
					.getExtras().get("key");
			Contact newContact = new Contact(
					name, keyAndToken, NEW_CONTACT_ID);
			getInstance().addContact(
					newContact);
			return;
		}
	}
	
	@Override
	public void onServiceConnected() {
		((ConversationListFragment) getFragmentManager()
				.findFragmentById(R.id.conversation_list))
				.onServiceConnected();
				if(mTwoPane && fragmentInit) {
					ConversationDetailFragment f = ((ConversationDetailFragment) getFragmentManager()
							.findFragmentById(R.id.conversation_detail));
					f.onServiceConnected();
				}
	}
}
