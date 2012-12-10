package edu.ucsb.cs290.touch.to.text.crypto;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class ContactsListCursorAdapter extends CursorAdapter {

	public ContactsListCursorAdapter(Context context, Cursor c) {
		super(context, c, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		((TextView) view).setText(c.getString(c.getColumnIndex(DatabaseHelper.NICKNAME)));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_1, parent, false);
	}

}
