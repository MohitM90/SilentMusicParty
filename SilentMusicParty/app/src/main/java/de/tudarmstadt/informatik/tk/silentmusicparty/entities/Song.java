package de.tudarmstadt.informatik.tk.silentmusicparty.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class that models the songs that are played on the party and listed (upvoted, downvoted) on the party's play list.
 * <p>
 * Created by Arbeit on 03.01.2017.
 */
public class Song implements Parcelable, Serializable {

    private int id;
    private String title;
    private String artist;
    private float length;
    private String url;
    private int votes;
    private long startTime;

    ////////////Parcelable stuff////////////
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(artist);
        parcel.writeFloat(length);
        parcel.writeString(url);
        parcel.writeLong(startTime);
    }
    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
    private Song(Parcel in) {
        id = in.readInt();
        title = in.readString();
        artist = in.readString();
        length = in.readFloat();
        url = in.readString();
        startTime = in.readLong();
    }
    ////////////Parcelable stuff end////////////

    /**
     * Constructor
     * @param id
     * @param title
     * @param artist
     * @param url
     */
    public Song(int id, String title, String artist, String url){
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.url = url;
        this.startTime = 9;
    }

    /**
     * Increment the votes by one.
     */
    public void upvote(){
        votes++;
    }

    /**
     * Decrement the votes by one.
     */
    public void downvote(){
        votes--;
    }


    /*-------------GETTERS---------------*/

    public int getVotes(){
        return votes;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {return url;}
    //...
    public int getId() {
        return id;
    }

    public long getStartTime(){
        return startTime;
    }


    /*-------------SETTERS---------------*/

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setPath(String path) {
        this.url = path;
    }

    /*-------------STATIC METHODS---------------*/

    /**
     * Compare to songs whether they are equal.
     * @param song
     * @param s
     * @return
     */
    public static boolean Compare(Song song, Song s) {
        if(song.getTitle().equals(s.getTitle()) &&
                song.getArtist().equals(s.getArtist()))
            return true;
        else
            return false;
    }

    /**
     * Checks whether a song is contained in list or not.
     * @param songs
     * @param song
     * @return
     */
    public static boolean Contains(ArrayList<Song> songs, Song song){
        for( Song s : songs){
            if(Compare(s, song))
                return true;
        }
        return false;
    }
}
