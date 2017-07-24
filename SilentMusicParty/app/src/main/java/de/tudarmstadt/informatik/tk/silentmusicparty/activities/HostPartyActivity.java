package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Playlist;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;

/**
 * Activity that enables to host a party and choose party name, etc.
 */
public class HostPartyActivity extends AppCompatActivity {

    private NetworkService mNetworkService;
    private ServiceConnection mNetworkConnection;
    private boolean mBound;
    private EditText txtPartyName;
    private EditText txtPartyLocation;
    private EditText txtPartyGenre;
    private EditText txtPartyDescription;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.host_party, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_party);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SongsAndPlaylistsDbHelper songsAndPlaylistsDb = new SongsAndPlaylistsDbHelper(this);
        try {
            songsAndPlaylistsDb.createDataBase();
        } catch (IOException e){
            Log.e("HostPartyActivity", "IOException while creating Database");
        }

        // get the views of the editable fields
        txtPartyName = (EditText) findViewById(R.id.txtName);
        txtPartyLocation = (EditText) findViewById(R.id.txtLocation);
        txtPartyGenre = (EditText) findViewById(R.id.txtGenre);
        txtPartyDescription = (EditText) findViewById(R.id.txtDescription);


        // open the database, get the playlists and put it into a spinner
        songsAndPlaylistsDb.openDataBase();
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        List<String> spinnerArray =  new ArrayList<String>();
        Cursor cursor = songsAndPlaylistsDb.getPlaylistCursor();

        if(cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                String playlist = cursor.getString(cursor.getColumnIndex("title"));
                spinnerArray.add(playlist);
                cursor.moveToNext();
            }
        }
        // use a standard adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        setupNetworkService();
    }


    /**
     * Creates a party with user as host.
     */
    public void addParty(){
        String defaultUsername = getResources().getString(R.string.pref_default_username);
        String[] defaultProfile = getResources().getStringArray(R.array.pref_default_profile);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // get user details from preferences
        String uid = prefs.getString("UserUUID", "");
        String hostName = prefs.getString("username", defaultUsername);
        String birthday = prefs.getString("birthday", defaultProfile[0]);
        int height = Integer.parseInt(prefs.getString("height", defaultProfile[1]));
        int gender = Integer.parseInt(prefs.getString("gender", defaultProfile[2]));
        String location = prefs.getString("location", defaultProfile[3]);
        String message= prefs.getString("message", defaultProfile[4]);

        // make yourself a host
        Person host = new Person(uid, hostName, birthday, height, gender, location, message);

        String partyUUID = UUID.randomUUID().toString();
        String partyName = txtPartyName.getText().toString();
        String partyDescription = txtPartyDescription.getText().toString();
        String partyLocation = txtPartyLocation.getText().toString();
        String partyGenre = txtPartyGenre.getText().toString();

        if (partyLocation.equals("")) partyLocation = "-";

        // try to get the image
        byte[] b = null;
        try {
            File f = getFileStreamPath("profilepic.jpg");
            b = new byte [(int)f.length()];
            FileInputStream fis = new FileInputStream(f);
            fis.read(b, 0, b.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create new party
        Party party = new Party(partyUUID, partyName, partyDescription, host, b, partyLocation, partyGenre);

        SongsAndPlaylistsDbHelper songsdb = new SongsAndPlaylistsDbHelper(this);
        songsdb.openDataBase();

        Spinner sp = (Spinner) findViewById(R.id.spinner);
        String selection = sp.getItemAtPosition(sp.getSelectedItemPosition()).toString();

        // use arbitryty initialization
        int id = 99;

        // get ID of the selected playlist
        ArrayList<Playlist> plist = songsdb.getPlaylists();
        for( Playlist l : plist){
            Log.d("DB", l.getTitle() + "  " + l.getId());
            if(selection.equals(l.getTitle())){
                id = l.getId();
                break;
            }
        }

        // load all songs into a list
        ArrayList<Song> songs = songsdb.getSongsOfPlaylist(id);
        for(int i = 0; i < songs.size(); i++)
            Log.d("SONGS", songs.get(i).getTitle());

        // pick the first song in the list and set is as the first song to be played
        Song song = songs.get(0);
        songs.remove(0);
        song.setStartTime(System.currentTimeMillis());
        party.setCurrentSong(song);

        // initialize the party
        party.initSonglist(songs);

        mNetworkService.hostParty(party);

        // create intent that leads to party activity
        Intent intent = new Intent(this, PartyActivity.class);
        intent.putExtra("PartyName", party.getName());
        intent.putExtra("PartyDescription", party.getDescription());

        startActivity(intent);
        finish();
    }

    /**
     * Host and thereby create a new party.
     * @param item
     */
    public void hostParty(MenuItem item) {
        addParty();
    }

    /**
     * Establish the networt service.
     */
    private void setupNetworkService() {
        mNetworkConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to NetworkService, cast the IBinder and get NetworkService instance
                NetworkService.NetworkBinder binder = (NetworkService.NetworkBinder) service;
                mNetworkService = binder.getService();

                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        Intent intent = new Intent(getBaseContext(), NetworkService.class);

        bindService(intent, mNetworkConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mNetworkConnection);
    }
}
