package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.tudarmstadt.informatik.tk.silentmusicparty.SonglistAdapter;
import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

/**
 * Fragment for the tab that holds the songlist.
 *
 * Created by Mohit on 02.03.2017.
 */

public class PartyFragmentTab1Playlist extends Fragment {

    SonglistAdapter adapter;

    private ListView lView;
    private FloatingActionButton muteBtn;
    private SeekBar seekbar;
    private TextView txtDuration;
    private boolean songChosen = false;
    Snackbar snackbar;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_party_tab1_playlist, container, false);

        //handle listview and assign adapter
        lView = (ListView) rootView.findViewById(R.id.songlist);
        lView.setAdapter(adapter);

        // create a listener for the mute button
        muteBtn = (FloatingActionButton) rootView.findViewById(R.id.mute_button);
        muteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mute or unmute the player
                PartyActivity pa = (PartyActivity) getActivity();
                boolean mute = pa.getPlayer().mute(muteBtn);
                // create a snake bar that tells whether player is mute or not
                if(mute)
                    Snackbar.make(view, "Player is mute", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                else
                    Snackbar.make(view, "Player is not mute", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        });

        // get the views (in order to later set the duration of the song)
        seekbar = (SeekBar) rootView.findViewById(R.id.seekBar);
        txtDuration = (TextView) rootView.findViewById(R.id.duration);

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make sure that a song is initially chosen
        songChosen = false;

        PartyActivity pa = (PartyActivity) getActivity();
        // if network service already available -> create songlist view
        if(pa != null && pa.getNetworkService() != null && pa.getNetworkService().getParty() != null){
            Party party = pa.getNetworkService().getParty();
            adapter = new SonglistAdapter(party.getSonglist(), getActivity());
        }
        // otherwise set adapter with empty songlist
        else{
            adapter = new SonglistAdapter(new ArrayList<Song>(), getActivity());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize snackbar
        snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE);

        // set layout options for the snackbar that informs about dancing
        View snackView = snackbar.getView();
        TextView tv = (TextView) snackView.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    /**
     * Propagate is-dancing-info to the adapter.
     * @param isDancing
     */
    public void updateUI(boolean isDancing) {
        // in case user is dancing -> propagate info to adapter
        if(isDancing)
            adapter.isDancing(true);
        else
            adapter.isDancing(false);
    }

    /**
     *  Update the seekbar that shows the elapsed time of the song.
     *
     * @param pos
     * @param duration
     */
    public void updatePlayer(final int pos, final int duration) {

        // set the corret time as a text
        txtDuration.post(new Runnable() {
            @Override
            public void run() {
                int posSec = pos/1000;
                int posMin = posSec/60;
                int durationSec = duration/1000;
                int durationMin = durationSec/60;

                // trigger the next song process 0.5 sec before end
                if(pos > duration-2000 && !songChosen){
                    PartyActivity pa = (PartyActivity) getActivity();
                    pa.getNetworkService().determineNextSong();
                    songChosen = true;
                }
                // set a corretly formated text
                txtDuration.setText(posMin + ":" + ("00" + posSec%60).substring(("" + posSec%60).length()) + " / " + durationMin + ":" + ("00" + durationSec%60).substring(("" + durationSec%60).length()));
            }
        });
        // set the correct position at seekbar
        seekbar.setMax(duration);
        seekbar.setProgress(pos);
    }

    /**
     * Pass the correct list to the adapter.
     * @param party
     */
    public void setAdapter(Party party) {
        adapter.setList(party.getSonglist(), false);
        adapter.notifyDataSetChanged();
    }

    public void getSongChosen(){
        songChosen = false;
    }
}
