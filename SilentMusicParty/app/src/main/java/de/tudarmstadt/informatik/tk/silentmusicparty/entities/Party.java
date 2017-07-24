package de.tudarmstadt.informatik.tk.silentmusicparty.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * This class models a party including relevant entities and actions like join party, quit party,...
 * <p>
 * Created by Arbeit on 03.01.2017.
 */

public class Party implements Parcelable, Serializable {

    private String uid;
    private String name;
    private String description;
    private Person host;
    private String location;
    private String genre;
    private Song currentSong;

    // lists all the guests attending the party event
    private HashMap<Person, byte[]> memberList;

    // lists all the songs for which you can vote
    private ArrayList<Song> songList;

    ////////////Parcelable stuff////////////
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(uid);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeParcelable(host, flags);
        parcel.writeString(location);
        parcel.writeString(genre);
        parcel.writeTypedList(songList);
        parcel.writeParcelable(currentSong, flags);
    }
    public static final Parcelable.Creator<Party> CREATOR = new Parcelable.Creator<Party>() {
        public Party createFromParcel(Parcel in) {
            return new Party(in);
        }

        public Party[] newArray(int size) {
            return new Party[size];
        }
    };
    private Party(Parcel in) {
        this();
        // read the attributes from the parcel
        uid = in.readString();
        name = in.readString();
        description = in.readString();
        host = in.readParcelable(Person.class.getClassLoader());
        location = in.readString();
        genre = in.readString();
        in.readTypedList(songList, Song.CREATOR);
        currentSong = in.readParcelable(Song.class.getClassLoader());

    }
    ////////////Parcelable stuff end////////////

    /**
     * Simple constructor
     */
    private Party() {
        songList = new ArrayList<Song>();
        memberList = new HashMap<>();
    }


    /**
     * Full constructor
     * @param uid
     * @param name
     * @param description
     * @param host
     * @param hostPicture
     * @param location
     * @param genre
     */
    public Party(String uid, String name, String description, Person host, byte[] hostPicture, String location, String genre) {
        this();
        this.uid = uid;
        this.name = name;
        this.description = description;
        this.host = host;
        this.location = location;
        this.genre = genre;
        this.memberList.put(host, hostPicture);
    }

    /**
     * Copying constructor
     * @param party
     */
    public Party(Party party){
        this();
        this.uid = party.getUid();
        this.name = party.getName();
        this.description = party.getDescription();
        this.host = party.getHost();
        this.location = party.getLocation();
        this.genre = party.getGenre();
        this.currentSong = party.getCurrentSong();

        this.memberList = new HashMap<>(party.getMemberList());
        this.songList = new ArrayList<Song>(party.getSonglist());
    }


    /*----------------METHODS------------------*/

    /**
     * Upvote a specified song from the song list.
     *
     * @param song
     */
    public void upvoteSong(Song song) {
        int songIdx = songList.indexOf(song);
        songList.get(songIdx).upvote();
    }

    /**
     * Add song to the song list
     * @param song
     */
    public void addSong(Song song){
        if(!songList.contains(song))
            songList.add(song);
     }

    /**
     * Updates the songlist by sorting all entries according to their likes.
     * @param song
     * @param vote
     */
    public void updateSongList(Song song, int vote) {
        // iterate over entire list
        for(int i = 0; i < songList.size(); i++)
            // if songs are the same -> update the votes
            if(Song.Compare(songList.get(i), song)){
                if(vote == 1)
                    songList.get(i).upvote();
                else if(vote == -1)
                    songList.get(i).downvote();
            }
    }

    /**
     * Person becomes a party member.
     *
     * @param pers
     * @return
     */
    public boolean joinParty(Person pers, byte[] pic) {
        if (!memberList.containsKey(pers)) {
            memberList.put(pers, pic);
            return true;
        } else
            return false;
    }

    /**
     * Initialize songlist.
     * @param songlist
     */
    public void initSonglist(ArrayList<Song> songlist) {
        this.songList = new ArrayList<Song>(songlist);
    }

    public String toString(){
        return name + ", by " + host.getName();
    }


    /*----------------GETTERS------------------*/

    /**
     * Get the user ID.
     * @return
     */
    public String getUid() {
        return uid;
    }

    public String getName() { return name; }

    public String getDescription() {
        return description;
    }

    public Person getHost() {
        return host;
    }

    public String getHostName(){
        return host.getName();
    }

    public String getLocation() {
        return location;
    }

    public String getGenre() {
        return genre;
    }

    /**
     * Get the number of guests on the party.
     * @return
     */
    public int getNumberGuests(){
        return memberList.size();
    }

    public HashMap<Person, byte[]> getMemberList(){
        return memberList;
    }

    public ArrayList<Song> getSonglist(){
        return songList;
    }

    public Song getCurrentSong() {
        return currentSong;
    }



    /*-----------------SETTERS --------------------*/

    public void setCurrentSong(Song currentSong){
        this.currentSong = currentSong;
    }


}
