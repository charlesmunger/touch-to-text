package edu.ucsb.cs290.touch.to.chat;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import edu.ucsb.cs290.touch.to.chat.crypto.DatabaseHelper;

public class ConversationListFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private Callbacks mCallbacks = sDummyCallbacks;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	public interface Callbacks {

		public void onItemSelected(String id);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(String id) {
		}
	};

	public ConversationListFragment() {
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((CursorAdapter) getListAdapter()).getCursor().close();
	};

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		mCallbacks.onItemSelected(((Cursor) getListAdapter().getItem(position))
				.getString(2));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	private class GetContactsFromDBTask extends
			AsyncTask<DatabaseHelper, Void, Cursor> {
		@Override
		protected Cursor doInBackground(DatabaseHelper... db) {
			return db[0].getContactsCursor();
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			if (getListAdapter() != null) {
				((SimpleCursorAdapter) getListAdapter()).swapCursor(result).close();
			} else {
				SimpleCursorAdapter s = new SimpleCursorAdapter(getActivity(),
						android.R.layout.simple_list_item_activated_1, result,
						DatabaseHelper.CONTACTS_QUERY,
						new int[] { android.R.id.text1 }, 0);
				setListAdapter(s);
			}
		}
	}

	public void onServiceConnected() {
		new GetContactsFromDBTask()
				.execute(((KeyActivity) getActivity()).mService.getInstance());
	}
}
