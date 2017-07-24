package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.databases.SongsAndPlaylistsDbHelper;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

public class EditPlaylistActivity  extends AppCompatActivity {

    SongsAndPlaylistsDbHelper playlistsDb;
    EditText filterEditText;
    RecyclerView filteredSongListView;
    RecyclerView addedSongListView;
    ArrayList<Song> filteredSongList;
    ArrayList<Song> addedSongList;

    EditText titleEditText;

    PlaylistAdapter filteredSongListAdapter;
    PlaylistAdapter addedSongListAdapter;

    int playlistId;

    /*
    // Show check mark; not used
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // check if playlist title is empty when back button is pressed
        if (titleEditText.getText().toString().equals("")){
            Toast.makeText(this, "Playlist title can't be empty!", Toast.LENGTH_LONG).show();
            return true;
        }
        // if title is not empty, change title in database entry and go to parent activity (PlaylistActivity)
        playlistsDb.changePlaylistTitle(playlistId, titleEditText.getText().toString());
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_playlist);

        // hide softkeyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Set toolbar to edit the playlist title in the actionbar and add back button
        Toolbar myToolbar = (Toolbar) findViewById(R.id.edit_playlist_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        titleEditText = (EditText) findViewById(R.id.edit_playlist_toolbar_edittext);

        // get views
        filterEditText = (EditText) findViewById(R.id.filter_input);
        filteredSongListView = (RecyclerView) findViewById(R.id.filtered_song_list);
        addedSongListView = (RecyclerView) findViewById(R.id.added_song_list);
        filteredSongListView.setLayoutManager(new LinearLayoutManager(this));
        addedSongListView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(filteredSongListView.getContext(), LinearLayout.VERTICAL);
        filteredSongListView.addItemDecoration(dividerItemDecoration);
        dividerItemDecoration = new DividerItemDecoration(addedSongListView.getContext(), LinearLayout.VERTICAL);
        addedSongListView.addItemDecoration(dividerItemDecoration);

        // get intent extra (playlistId of playlist which is edited)
        Intent intent = getIntent();
        playlistId = intent.getIntExtra("PlaylistId", -1);

        // init database helper
        playlistsDb = new SongsAndPlaylistsDbHelper(this);
        playlistsDb.initDataBaseHelper();

        // update actionbar edittext to title of playlist
        titleEditText.setText(playlistsDb.getPlaylistTitle(playlistId));
        titleEditText.setSelection(titleEditText.getText().length());

        // fill addedSonglist and filteredSonglist
        fillSongLists();

        // set onClickListener for filteredSonglist to add song to playlist
        View.OnClickListener filteredOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = filteredSongListView.getChildLayoutPosition(v);
                int songId = filteredSongList.get(position).getId();
                addSongToPlayList(songId, position);
            }
        };
        filteredSongListAdapter = new PlaylistAdapter(filteredSongList, filteredOnClickListener);
        filteredSongListView.setAdapter(filteredSongListAdapter);

        // set onClickListener for addedSonglist to remove song from playlist
        View.OnClickListener addedOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = addedSongListView.getChildLayoutPosition(v);
                int songId = addedSongList.get(position).getId();
                removeSongFromPlayList(songId, position);
            }
        };
        addedSongListAdapter = new PlaylistAdapter(addedSongList, addedOnClickListener);
        addedSongListView.setAdapter(addedSongListAdapter);

        // update both listviews (filteredsongs and addedsongs)
        updateSongListViews();

        // set textChangedListener for filter edittext; update songlists and -views with current filter
        filterEditText.addTextChangedListener(new TextWatcher(){
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count){
                fillSongLists();
                updateSongListViews();
            }

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
        });
    }

    /**
     * Scroll addedSongListView to last position
     * Add song - playlist entry to database
     * update lists and listviews
     *
     * @param songId Id of song to be added to playlist
     * @param position of change in listview
     */
    public void addSongToPlayList(int songId, int position){
        //Log.d("playlistid + songid", ""+ playlistId + " " + songId);
        addedSongListView.scrollToPosition(addedSongList.size());

        playlistsDb.addSongToPlaylist(playlistId, songId);
        fillSongLists();
        filteredSongListAdapter.notifyItemRemoved(position);
        addedSongListAdapter.notifyItemInserted(addedSongList.size());
        //addedSongListAdapter.notifyDataSetChanged();
        //updateSongListViews();
    }

    /**
     * Remove song - playlist entry from database
     * update lists and listviews
     *
     * @param songId Id of song to be removed from playlist
     * @param position of change in listview
     */
    public void removeSongFromPlayList(int songId, int position){
        playlistsDb.removeSongFromPlaylist(playlistId, songId);
        fillSongLists();
        addedSongListAdapter.notifyItemRemoved(position);
        filteredSongListAdapter.notifyDataSetChanged();
        //filteredSongListAdapter.notifyItemInserted(songId-1);
        //addedSongListAdapter.notifyDataSetChanged();
        //updateSongListViews();
    }

    /**
     * fill addedSonglist and filteredSonglist with appropriate songs
     */
    private void fillSongLists(){
        addedSongList = playlistsDb.getSongsOfPlaylist(playlistId);
        if (addedSongListAdapter != null) addedSongListAdapter.setSonglist(addedSongList);
        fillFilteredSongList();
    }

    /**
     * delete songs from available songs which are already in playlist
     */
    private void fillFilteredSongList(){
        ArrayList<Song> temp = playlistsDb.getFilteredSongList(filterEditText.getText().toString());

        // Remove songs which are already in the playlist (Do this in dbhelper?)
        for(int i = 0; i < temp.size(); i++){
            Song song = temp.get(i);
            for (Song alreadyInPlaylist : addedSongList){
                if (song.getId() == alreadyInPlaylist.getId()){
                    temp.remove(i);
                    i--;
                    break;
                }
            }
        }
        filteredSongList = temp;
        if (filteredSongListAdapter != null) filteredSongListAdapter.setSonglist(filteredSongList);
    }

    /**
     * update both listviews (filteredsonglistview and addedsonglistview)
     */
    private void updateSongListViews(){
        updateFilteredSongListView();
        updateAddedSongListView();
    }

    /**
     * update filteredsonglistview
     */
    private void updateFilteredSongListView(){
        ArrayList<String> songs = new ArrayList<String>();
        for(Song song : filteredSongList){
            songs.add(song.getTitle());
        }
        filteredSongListAdapter.notifyDataSetChanged();
    }

    /**
     * update addedsonglistview
     */
    private void updateAddedSongListView(){
        ArrayList<String> songs = new ArrayList<String>();
        for(Song song : addedSongList){
            songs.add(song.getTitle());
            // TEST:
            //songs.add(song.getTitle() + " " + song.getArtist());
        }
        //addedSongListViewArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songs);
        addedSongListAdapter.notifyDataSetChanged();
    }


    public static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
        private ArrayList<Song> songs;
        private View.OnClickListener onClickListener;

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
        public PlaylistAdapter(ArrayList<Song> songs, View.OnClickListener listener) {
            this.songs = songs;
            this.onClickListener = listener;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public PlaylistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.edit_playlist_list_item /*android.R.layout.simple_list_item_2*/, parent, false);
            v.setOnClickListener(onClickListener);
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
