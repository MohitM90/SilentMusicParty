package de.tudarmstadt.informatik.tk.silentmusicparty.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

public class EditSongActivity extends AppCompatActivity{

    SongsAndPlaylistsDbHelper songsAndPlaylistsDb;

    EditText titleEditText;
    EditText artistEditText;
    EditText urlEditText;

    int songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);

        // get views
        titleEditText = (EditText)findViewById(R.id.add_song_title);
        artistEditText = (EditText)findViewById(R.id.add_song_artist);
        urlEditText = (EditText)findViewById(R.id.add_song_url);

        // if song id is valid (< 0) -> edit mode; else add new song to database
        Intent intent = getIntent();
        songId = intent.getIntExtra("SongId", -1);

        // init database
        songsAndPlaylistsDb = new SongsAndPlaylistsDbHelper(this);
        songsAndPlaylistsDb.initDataBaseHelper();

        // if song id is valid (< 0) -> edit mode; change title from "Add Song" to "Edit Song"
        if (songId != -1){
            // change title to edit song
            setTitle(R.string.title_activity_edit_song);

            // update edittext fields with data from database
            Song song = songsAndPlaylistsDb.getSong(songId);
            titleEditText.setText(song.getTitle());
            artistEditText.setText(song.getArtist());
            urlEditText.setText(song.getPath());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; add check mark to action bar
        getMenuInflater().inflate(R.menu.edit_song, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if check mark is pressed, chack if title, artist and url have valid entries; if yes add song to database
        if (item.getItemId() == android.R.id.home) {
            return false;
        } else if (titleEditText.getText().toString().equals("")){
            Toast.makeText(this, "Title can't be empty!", Toast.LENGTH_LONG).show();
        } else if (artistEditText.getText().toString().equals("")){
            Toast.makeText(this, "Artist name can't be empty!", Toast.LENGTH_LONG).show();
        } else if (urlEditText.getText().toString().equals("")) {
            Toast.makeText(this, "URL can't be empty!", Toast.LENGTH_LONG).show();
        } else if (!URLUtil.isValidUrl(urlEditText.getText().toString())){
            Toast.makeText(this, "Invalid url!", Toast.LENGTH_LONG).show();
        } else {
            addSongToDB();
            return true;
        }
        return false;
    }

    /**
     * add song to database, witth current contents of edittext fields
     */
    private void addSongToDB(){

        String title = titleEditText.getText().toString();
        String artist = artistEditText.getText().toString();
        String album = "";
        String url = urlEditText.getText().toString();

        // if songid == -1; not in edit mode -> add song to database; else update database entry
        if(songId == -1){
            songsAndPlaylistsDb.addSong(title, artist, album, url);
            Toast.makeText(this, "Added song to library", Toast.LENGTH_LONG).show();
        } else {
            songsAndPlaylistsDb.editSong(songId, title, artist, album, url);
            Toast.makeText(this, "Song edited", Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(this, SongLibraryActivity.class));
    }
}
