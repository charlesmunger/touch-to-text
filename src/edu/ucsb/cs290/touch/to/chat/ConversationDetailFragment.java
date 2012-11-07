package edu.ucsb.cs290.touch.to.chat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.ucsb.cs290.touch.to.chat.dummy.DummyContent;

public class ConversationDetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";

    DummyContent.DummyItem mItem;
    ListView messageList;

    public ConversationDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_conversation_detail, container, false);
        if (mItem != null) {
        	String[] stuff = new String[] {"Testing","Attack at dawn"};
            messageList = ((ListView) rootView.findViewById(R.id.messages_list));
            messageList.setAdapter( new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_2,android.R.id.text1, stuff));
        }
        return rootView;
    }
}
