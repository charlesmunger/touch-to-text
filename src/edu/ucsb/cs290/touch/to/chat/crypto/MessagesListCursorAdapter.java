package edu.ucsb.cs290.touch.to.chat.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TwoLineListItem;
import edu.ucsb.cs290.touch.to.chat.remote.Helpers;
import edu.ucsb.cs290.touch.to.chat.remote.messages.Message;
import edu.ucsb.cs290.touch.to.chat.remote.messages.SignedMessage;

public class MessagesListCursorAdapter extends CursorAdapter {
	private final PublicKey author;
	private final PublicKey self;
	private static final DateFormat df = DateFormat.getDateTimeInstance();

	public MessagesListCursorAdapter(Context context, Cursor c,
			PublicKey author, PublicKey self) {
		super(context, c, false);
		this.author = author;
		this.self = self;
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		TwoLineListItem v = (TwoLineListItem) view;
		final Date date = new Date(c.getLong(c
				.getColumnIndex(DatabaseHelper.DATE_TIME)));
		final SignedMessage sm = (SignedMessage) Helpers.deserialize(c
				.getBlob(c.getColumnIndex(DatabaseHelper.MESSAGE_BODY)));
		Message message = null;
		try {
			if (sm.getAuthor().equals(author)) {
				v.getText1().setGravity(Gravity.LEFT);
				v.getText2().setGravity(Gravity.LEFT);
				message = sm.getMessage(author);
			} else if (sm.getAuthor().equals(self)) {
				v.getText1().setGravity(Gravity.RIGHT);
				v.getText2().setGravity(Gravity.RIGHT);
				message = sm.getMessage(self);
			} else {
				Log.wtf("touch-to-text", "Author not recognized!!");
			}
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		v.getText1().setText(message.getBody());
		v.getText2().setText(df.format(date));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
	}

}
