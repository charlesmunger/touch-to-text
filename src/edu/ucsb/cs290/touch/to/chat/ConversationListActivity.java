package edu.ucsb.cs290.touch.to.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import edu.ucsb.cs290.touch.to.chat.crypto.CryptoContacts;
import edu.ucsb.cs290.touch.to.chat.crypto.SealablePublicKey;

public class ConversationListActivity extends KeyActivity implements
ConversationListFragment.Callbacks {
	private boolean mTwoPane;

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

	@Override
	public void onItemSelected(String id) {
		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(ConversationDetailFragment.ARG_ITEM_ID, id);
			ConversationDetailFragment fragment = new ConversationDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
			.replace(R.id.conversation_detail_container, fragment)
			.commit();

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
			// name time key+signedsecret
			String name = data.getExtras().getString("name");
			Long dateTime = data.getExtras().getLong("date");
			SealablePublicKey keyAndToken = (SealablePublicKey) data
					.getExtras().get("key");
			CryptoContacts.Contact newContact = new CryptoContacts.Contact(
					name, keyAndToken);
			getInstance().addContact(
					newContact);
			return;
		}
	}
}
