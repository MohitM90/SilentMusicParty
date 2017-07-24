package de.tudarmstadt.informatik.tk.silentmusicparty.activities;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Playlist;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

public class SongLibraryActivity extends AppCompatActivity {

    RecyclerView listView;
    ArrayList<Song> songList;
    SongsAndPlaylistsDbHelper songsDb;
    ArrayAdapter<String> arrayAdapter;
    SongAdapter songAdapter;

    Context context;
    int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_library);
        context = this;

        // init database
        songsDb = new SongsAndPlaylistsDbHelper(this);
        songsDb.initDataBaseHelper();

        // fill song list with all songs from database
        fillSongList();


        listView = (RecyclerView) findViewById(R.id.songs_list);
        listView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listView.getContext(), LinearLayout.VERTICAL);
        listView.addItemDecoration(dividerItemDecoration);

        // update song listview with data from song list
        updateSongListView();

        // songlistview on click listener -> start EditSongActivity with song id as extra
        View.OnClickListener listViewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create intend that leads to next activity (EditSongActivity)
                Intent intent = new Intent(getApplicationContext()/*this*/, EditSongActivity.class);
                int position = listView.getChildLayoutPosition(v);
                intent.putExtra("SongId", songList.get(position).getId());
                startActivity(intent);
            }
        };

        // set onLongClickListener to delete a song from library
        View.OnLongClickListener listViewLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                deleteSong(pos);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                //Toast?
                                break;
                        }
                    }
                };
                int position = listView.getChildLayoutPosition(v);
                pos = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Delete song \"" + songList.get(pos).getTitle() + "\"?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                //deletePlaylist(position);
                return true;
            }
        };

        // add onClickListener for floating action button to add a new song -> start activity
        // EditSongAvtivity without an extra -> Add mode
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_songs);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext()/*this*/, EditSongActivity.class);
                startActivity(intent);
            }
        });

        // set songadapter
        songAdapter = new SongAdapter(songList, listViewClickListener, listViewLongClickListener);
        listView.setAdapter(songAdapter);
    }

    /**
     * fill song list with all available songs
     */
    private void fillSongList(){

        songList = songsDb.getAllSongs();
        if (songAdapter != null) songAdapter.setSonglist(songList);
    }

    /**
     * update songListView with the titles of all songs currently in song list
     */
    private void updateSongListView(){

        ArrayList<String> songsString = new ArrayList<String>();
        for(Song song : songList){
            songsString.add(song.getTitle());
        }
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songsString);

    }

    /**
     * Delete song from database, notify Songlistview adapter of the changes and update the listview
     *
     * @param pos current position in listview
     */
    void deleteSong(int pos){
        int id = songList.get(pos).getId();
        songsDb.deleteSong(id);
        fillSongList();
        songAdapter.notifyItemRemoved(pos);
        updateSongListView();
    }


    public static class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
        private ArrayList<Song> songs;
        private View.OnClickListener onClickListener;
        private View.OnLongClickListener onLongClickListener;
        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView mTextViewTitle;
            public TextView mTextViewSub;
            public ViewHolder(View v) {
                super(v);
                mTextViewTitle = (TextView) v.findViewById(R.id.text1/*android.R.id.text1*/);
                mTextViewSub = (TextView) v.findViewById(R.id.text2/*android.R.id.text2*/);

                mTextViewTitle.setTypeface(null, Typeface.BOLD);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public SongAdapter(ArrayList<Song> songs, View.OnClickListener listener, View.OnLongClickListener listener2) {
            this.songs = songs;
            this.onClickListener = listener;
            this.onLongClickListener = listener2;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public SongAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.edit_playlist_list_item /*android.R.layout.simple_list_item_2*/, parent, false);
            v.setOnClickListener(onClickListener);
            v.setOnLongClickListener(onLongClickListener);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.mTextViewTitle.setText(songs.get(position).getTitle());
            holder.mTextViewSub.setText(songs.get(position).getArtist());

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return songs.size();
        }

        public void setSonglist(ArrayList<Song> songs) {
            this.songs = songs;
        }
    }
}
