package de.tudarmstadt.informatik.tk.silentmusicparty.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Playlist;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

public class SongsAndPlaylistsDbHelper extends SQLiteOpenHelper {


    private static String DB_NAME = "SongsAndPlaylists.db";
    private final Context myContext;
    private String DB_PATH;// = "/data/data/de.../databases/";
    private SQLiteDatabase myDB;

    public SongsAndPlaylistsDbHelper(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;
        DB_PATH = context.getDatabasePath(DB_NAME).getPath();
    }


    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();
        if(dbExist){
            //do nothing
        }else{
            this.getReadableDatabase();
            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }


    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;
        try{
            checkDB = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
        }catch(Exception e){
            //database does't exist yet.
        }
        if(checkDB != null){
            checkDB.close();
        }
        return checkDB != null;
    }


    private void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException {

        myDB = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public synchronized void close() {

        if(myDB != null)
            myDB.close();
        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public ArrayList<Playlist> getPlaylists(){

        Playlist playlist;
        ArrayList<Playlist> playlistList = new ArrayList<Playlist>();

        Cursor cursor = myDB.rawQuery("SELECT * FROM Playlists", null);
        if(cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                playlist = new Playlist(cursor.getInt(0), cursor.getString(1));
                playlistList.add(playlist);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return playlistList;
    }

    public int addPlaylist(String title){

        ContentValues cv = new ContentValues();
        cv.put("title", title);
        myDB.insertOrThrow("Playlists", null, cv);

        Cursor cursor = myDB.rawQuery("SELECT last_insert_rowid()", null);
        cursor.moveToFirst();
        int id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    public void removePlaylist(int id){

        myDB.delete("Playlists", "playlist_id="+id, null);
        myDB.delete("Playlists_Songs", "playlist_id="+id, null);

    }


    public Cursor getPlaylistCursor() {

        return myDB.rawQuery("SELECT * FROM Playlists", null);

    }

    public void addSongToPlaylist(int playlistId, int songId){

        ContentValues cv = new ContentValues();
        cv.put("playlist_id", playlistId);
        cv.put("song_id", songId);
        myDB.insert("Playlists_Songs", null, cv);
    }

    public void removeSongFromPlaylist(int playlistId, int songId){

        myDB.delete("Playlists_Songs", "playlist_id=" + playlistId + " AND song_id=" + songId, null);
    }

    // TODO christian use this
    public ArrayList<Song> getSongsOfPlaylist(int playlistId){

        Song song;
        ArrayList<Song> songList = new ArrayList<Song>();

        Cursor cursor = myDB.rawQuery("SELECT song_id FROM Playlists_Songs WHERE playlist_id="+playlistId, null);
        if(cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                int song_id = cursor.getInt(0);
                Cursor cursor2 = myDB.rawQuery("SELECT * FROM Songs WHERE song_id="+song_id, null);
                if(cursor2.moveToFirst()){
                    song = new Song(cursor2.getInt(cursor2.getColumnIndex("song_id")), cursor2.getString(cursor2.getColumnIndex("title")),
                            cursor2.getString(cursor2.getColumnIndex("artist")), cursor2.getString(cursor2.getColumnIndex("path")));
                    songList.add(song);
                }
                cursor2.close();
                cursor.moveToNext();
            }
        }
        cursor.close();
        return songList;
    }

    public String getPlaylistTitle(int playlistId){
        String playlistTitle = "";
        Cursor cursor = myDB.rawQuery("SELECT title FROM Playlists WHERE playlist_id=" + playlistId, null);
        if(cursor.moveToFirst()){
            playlistTitle = cursor.getString(0);
        }
        cursor.close();
        return playlistTitle;
    }

    public void changePlaylistTitle(int playlistId, String title) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        myDB.update("Playlists", cv, "playlist_id="+playlistId, null);
    }

//<<<<<<< HEAD
    public Song getSong(int songId){
        Song song = new Song(-1, "","","");
        Cursor cursor;

        cursor = myDB.rawQuery("SELECT * FROM Songs WHERE song_id="+songId, null);

        if(cursor.moveToFirst()){
            song = new Song(cursor.getInt(0),cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")), cursor.getString(cursor.getColumnIndexOrThrow("path")));
        }
        cursor.close();
        return song;
    }

    public ArrayList<Song> getAllSongs(){

        return getFilteredSongList("");
/*=======
    public ArrayList<Song> getAllSongs() {
        Song song;
        ArrayList<Song> songList = new ArrayList<Song>();

        Cursor cursor = myDB.rawQuery("SELECT song_id FROM Songs", null);
        if(cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                int song_id = cursor.getInt(0);
                Cursor cursor2 = myDB.rawQuery("SELECT * FROM Songs WHERE song_id="+song_id, null);
                if(cursor2.moveToFirst()){
                    song = new Song(cursor2.getInt(cursor2.getColumnIndex("song_id")), cursor2.getString(cursor2.getColumnIndex("title")),
                            cursor2.getString(cursor2.getColumnIndex("artist")), cursor2.getString(cursor2.getColumnIndex("path")));
                    songList.add(song);
                }
                cursor.moveToNext();
            }
        }

        return songList;
>>>>>>> remotes/origin/chrisbe6*/
    }

    public ArrayList<Song> getFilteredSongList(String filter) {
        Song song;
        ArrayList<Song> songList = new ArrayList<Song>();
        Cursor cursor;
        if(filter.equals("")){
            cursor = myDB.rawQuery("SELECT * FROM Songs", null);
        } else {
            filter = "%" + filter + "%";
            cursor = myDB.rawQuery("SELECT * FROM Songs WHERE title LIKE ? OR artist LIKE ?", new String[] {filter, filter});
        }
        if(cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                song = new Song(cursor.getInt(0),cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")), cursor.getString(cursor.getColumnIndexOrThrow("path")));
                songList.add(song);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return songList;
    }

    public void addSong(String title, String artist, String album, String path) {

        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("album", album);
        cv.put("artist", artist);
        cv.put("path", path);
        myDB.insert("Songs", null, cv);

    }

    public void editSong(int songId, String title, String artist, String album, String path) {

        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("album", album);
        cv.put("artist", artist);
        cv.put("path", path);
        myDB.update("Songs", cv, "song_id="+songId, null);

    }

    public void deleteSong(int songId) {

        myDB.delete("Playlists_Songs", "song_id=" + songId, null);
        myDB.delete("Songs", "song_id="+songId, null);

    }


    public void updateDatabase() throws IOException {

        this.getReadableDatabase();
        try {
            copyDataBase();
        } catch (IOException e) {
            throw new Error("Error copying database");
        }
    }

    public void initDataBaseHelper(){

        try {
            createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            openDataBase();
        }catch(SQLException sqle){
            throw sqle;
        }
    }


}
