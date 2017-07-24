package de.tudarmstadt.informatik.tk.silentmusicparty.activities;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

/**
 * Activity that supports adding a song to the songlist.
 */
public class AddSongActivity extends AppCompatActivity{

    EditText titleEditText;
    EditText artistEditText;
    EditText urlEditText;
    ListView lView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song);

        // get the edit fields
        titleEditText = (EditText)findViewById(R.id.add_song_title);
        artistEditText = (EditText)findViewById(R.id.add_song_artist);
        urlEditText = (EditText)findViewById(R.id.add_song_url);
        lView = (ListView)findViewById(R.id.songs_from_db);

        // create a data base, open it and retrieve all songs
        SongsAndPlaylistsDbHelper songsAndPlaylistsDb = new SongsAndPlaylistsDbHelper(this);
        songsAndPlaylistsDb.openDataBase();
        final ArrayList<Song> songs = songsAndPlaylistsDb.getAllSongs();

        // use a standard adapter
        ArrayAdapter<Song> adapter = new ArrayAdapter<Song> (this, android.R.layout.simple_list_item_2, android.R.id.text1, songs) {

            @Override
            public View getView(int position,
                                View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                // set the layout options for the text fields
                text1.setTypeface(null, Typeface.BOLD);
                text1.setSingleLine(true);
                text1.setEllipsize(TextUtils.TruncateAt.END);
                text2.setSingleLine(true);
                text2.setEllipsize(TextUtils.TruncateAt.END);

                // set the content
                text1.setText(songs.get(position).getTitle());
                text2.setText(songs.get(position).getArtist());

                return view;
            }

        };
        lView.setAdapter(adapter);

        // set the selection options for the listview
        lView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lView.setSelector(android.R.color.darker_gray);
        lView.setClickable(true);

        // on click -> add the selected song to songlist
        lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                Song s = (Song) lView.getItemAtPosition(position);
                addSong(s);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Determines the actions for selecting a menu item.
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // in case the back button in toolbar was pressed -> return to parent activity
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    /**
     * Creates an intent containing the selected song and passes it to PartyActivity
     * @param song
     */
    private void addSong(Song song){
        Intent intent = new Intent(this, PartyActivity.class);
        intent.putExtra("song", (Parcelable) song);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
