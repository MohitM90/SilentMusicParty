package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;

/**
 * Created by Mohit on 02.03.2017.
 */

public class PartyFragmentTab3Shoutbox extends Fragment {

    ListView listview;
    EditText text;
    FloatingActionButton btnSend;
    List<HashMap<String, String>> messages;

    SimpleAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_party_tab3_shoutbox, container, false);
        listview = (ListView)rootView.findViewById(R.id.shoutbox);
        text = (EditText)rootView.findViewById(R.id.editText);
        btnSend = (FloatingActionButton) rootView.findViewById(R.id.button2);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(v);
            }
        });
        messages = new ArrayList<>();
        adapter = new SimpleAdapter(getContext(), messages, R.layout.shoutbox_list_item, new String[] {"Name", "Message"}, new int[] {R.id.name, R.id.message});

        listview.setAdapter(adapter);
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    /**
     * Sends Chat-message to server, which distributes it to all guests
     * @param v
     */
    public void sendMessage(View v) {
        if (!text.getText().toString().equals("")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String uid = prefs.getString("UserUUID",null);
            String name = prefs.getString("username", null);
            String message = text.getText().toString();
            HashMap<String, String> mess = new HashMap<>();
            mess.put("Name", name);
            mess.put("Message", message);
            messages.add(mess);
            text.getText().clear();
            adapter.notifyDataSetChanged();

            PartyActivity pa = (PartyActivity)getActivity();
            pa.getNetworkService().sendShout(uid, message);
        }
    }

    /**
     * shows received message
     * @param name name of sender
     * @param message received message
     */
    public void receiveMessage(String name, String message) {
        HashMap<String, String> mess = new HashMap<>();
        mess.put("Name", name);
        mess.put("Message", message);
        messages.add(mess);
        adapter.notifyDataSetChanged();
    }
}
