package edu.ucsb.cs290.touch.to.text;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignedObject;
import java.security.cert.CertificateException;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import edu.ucsb.cs290.touch.to.text.crypto.Contact;
import edu.ucsb.cs290.touch.to.text.crypto.DatabaseHelper;
import edu.ucsb.cs290.touch.to.text.crypto.MessagesListCursorAdapter;
import edu.ucsb.cs290.touch.to.text.https.TorProxy;
import edu.ucsb.cs290.touch.to.text.remote.messages.Message;
import edu.ucsb.cs290.touch.to.text.remote.messages.ProtectedMessage;
import edu.ucsb.cs290.touch.to.text.remote.messages.SignedMessage;
import edu.ucsb.cs290.touch.to.text.remote.messages.TokenAuthMessage;

public class ConversationDetailFragment extends Fragment {

	public static final String ARG_ITEM_ID = "contact name";

	Contact mItem;
	ListView messageList;
	EditText messageText;
	View rootView;
	boolean connectedA = false;
	boolean connectedService = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(ARG_ITEM_ID)) {
			mItem = (Contact) getArguments().get(ARG_ITEM_ID);
		} else {
			Log.wtf("touch-to-text", "contact arg not passed");
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//currently working around a bug that causes crash on layout transitions
//		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN  && false) {
//			final LayoutTransition l = new LayoutTransition();
//			l.enableTransitionType(LayoutTransition.APPEARING);
//			l.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
//			l.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
//			l.enableTransitionType(LayoutTransition.CHANGING);
//			l.enableTransitionType(LayoutTransition.DISAPPEARING);
//			container.setLayoutTransition(l);
//		}
		rootView = inflater.inflate(R.layout.fragment_conversation_detail,
				container, false);
		messageList = (ListView) rootView.findViewById(R.id.messages_list);
		messageList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		messageList.setStackFromBottom(true);
		if(((KeyActivity) getActivity()).mBound) {
			inflateContact();
		}
		return rootView;
	}

	@Override
	public void onDestroyView() {
		if(messageList.getAdapter() != null) {
			((CursorAdapter) messageList.getAdapter()).getCursor().close();
		}
		super.onDestroyView();
	};
	
	private void sendMessage(View v) {
		DatabaseHelper instance = ((KeyActivity) getActivity()).mService.getInstance();
		EditText messageToSend = (EditText) v
				.findViewById(R.id.edit_message_text);
		if (messageToSend.getText() == null) {
			return;
		}
		Message m = new Message(messageToSend.getText().toString());
		messageToSend.getEditableText().clear();
		ProtectedMessage pm = null;
		SignedMessage signedMessage = null;
		
		SignedObject newToken = instance.getOutgoingToken(mItem.getID());

		try {
			signedMessage = new SignedMessage(m, instance.getSigningKey());
			pm = new ProtectedMessage(signedMessage, mItem.getEncryptingKey(),
					instance.getSigningKey(), newToken);
			instance.addOutgoingMessage(signedMessage, mItem);

		} catch (GeneralSecurityException e) {
			Log.w("touch-to-text","Problem creating ProtectedMessage!", e);
		} catch (IOException e) {
			Log.d("touch-to-text","Problem creating ProtectedMessage!", e);
		}
		new GetMessagesFromDBTask().execute(
				((KeyActivity) getActivity()).mService.getInstance(), mItem);
		TokenAuthMessage tm = new TokenAuthMessage(pm, mItem.getTokenKey(),
				mItem.getToken());
		new AsyncTask<TokenAuthMessage, Void, Void>() {

			@Override
			protected Void doInBackground(TokenAuthMessage... params) {
				try {
					TorProxy.postThroughTor(getActivity().getApplicationContext(), params[0]);
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			
		}.execute(tm);
	}

	private class GetMessagesFromDBTask extends AsyncTask<Object, Void, Cursor> {
		private PublicKey author;
		private PublicKey self;
		
		@Override
		protected Cursor doInBackground(Object... ids) {
			
			Log.v("touch-to-text", "Updating message view");
			DatabaseHelper databaseHelper = (DatabaseHelper) ids[0];
			self = databaseHelper.getSealablePublicKey().sign();
			Contact contact = (Contact) ids[1];
			author = contact.getSigningKey();
			return databaseHelper.getMessagesCursor(contact.getID());
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			if (messageList.getAdapter() != null) {
				((CursorAdapter) messageList.getAdapter()).swapCursor(result).close();
			} else {
				MessagesListCursorAdapter s = new MessagesListCursorAdapter(
						getActivity(),result, author, self);
				messageList.setAdapter(s);
			}
		}
	}

	protected void onServiceConnected() {
		connectedService = true;
		checkDone();
	}

	private void inflateContact() {
		rootView.findViewById(R.id.send_message_button)
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						sendMessage(rootView);

					}
				});
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		connectedA = true;
		checkDone();
	}
	
	private void checkDone() {
		if(connectedA && connectedService) {
			new GetMessagesFromDBTask().execute(
					((KeyActivity) getActivity()).mService.getInstance(), mItem);
			if (rootView != null) {
				inflateContact();
			}
		}
	}

	public void refresh() {
		checkDone();
	}

}
