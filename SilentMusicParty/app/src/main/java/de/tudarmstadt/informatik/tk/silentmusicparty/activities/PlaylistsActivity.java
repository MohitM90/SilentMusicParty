package de.tudarmstadt.informatik.tk.silentmusicparty.activities;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Playlist;

public class PlaylistsActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<Playlist> playlistList;
    SongsAndPlaylistsDbHelper playlistsDb;
    ArrayAdapter<String> arrayAdapter;

    Context context;
    int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);
        context = this;

        // init the dbhelper
        playlistsDb = new SongsAndPlaylistsDbHelper(this);
        playlistsDb.initDataBaseHelper();

        // fill playlistList with data from database
        fillPlaylistList();

        listView = (ListView) findViewById(R.id.playlist_list);

        // set adapter for playlistListView (titles from playlistList)
        updatePlaylistListView();

        // Playlist onClickListener to edit a playlist
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // create intend that leads to next activity
                Intent intent = new Intent(getApplicationContext()/*this*/, EditPlaylistActivity.class);
                intent.putExtra("PlaylistId", playlistList.get(position).getId());
                startActivity(intent);
            }

        });

        // Set onLongClickListener to delete a playlist
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // Dialog box to check if user wants to delete playlist
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                deletePlaylist(pos);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                //Toast?
                                break;
                        }
                    }
                };

                pos = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Delete playlist \"" + playlistList.get(pos).getTitle() + "\"?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            }
        });

        // Floating action button to create a new playlist
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_playlists);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistsActivity.this);
                builder.setTitle("Create new Playlist");
                // Set up the input
                final EditText input = new EditText(PlaylistsActivity.this);

                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                input.setHint("Playlist Title");

                // Set up the buttons; if positive answer, go to editplaylist activity to add songs
                builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String title = input.getText().toString();
                        SongsAndPlaylistsDbHelper songsAndPlaylistsDb = new SongsAndPlaylistsDbHelper(PlaylistsActivity.this);
                        songsAndPlaylistsDb.initDataBaseHelper();
                        int playlistId = songsAndPlaylistsDb.addPlaylist(title);

                        Intent intent = new Intent(getApplicationContext()/*this*/, EditPlaylistActivity.class);
                        intent.putExtra("PlaylistId", playlistId);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                float dpi = PlaylistsActivity.this.getResources().getDisplayMetrics().density;
                dialog.setView(input, (int)(16*dpi), (int)(5*dpi), (int)(16*dpi), 0);

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (TextUtils.isEmpty(s)) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        } else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    }
                });
                dialog.show();
                // Disable positive "create" button to prevent creation of empty playlist
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
    }

    /**
     * get all playlist from database and fill playlistList
     */
    private void fillPlaylistList(){
        playlistList = playlistsDb.getPlaylists();
    }


    /**
     * set PlaylistslistView adapter and update view
     */
    private void updatePlaylistListView(){
        ArrayList<String> playlistsString = new ArrayList<String>();
        for(Playlist playlist : playlistList){
            playlistsString.add(playlist.getTitle());
        }
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, playlistsString);
        listView.setAdapter(arrayAdapter);
    }

    /**
     * delete playlist from database and update playlistList and playlistListView
     *
     * @param pos position in playlistlistview of playlist to delete
     */
    void deletePlaylist(int pos){
        int id = playlistList.get(pos).getId();
        playlistsDb.removePlaylist(id);
        fillPlaylistList();
        updatePlaylistListView();
    }
}
