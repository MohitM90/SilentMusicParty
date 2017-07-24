package de.tudarmstadt.informatik.tk.silentmusicparty.entities;

/**
 * Class for managing the playlists.
 */
public class Playlist {
    private int id;
    private String title;

    /**
     * Constructor
     * @param id
     * @param title
     */
    public Playlist(int id, String title){
        this.id = id;
        this.title = title;
    }

    /*--------------GETTERS---------------*/
    public int getId(){
        return id;
    }

    public String getTitle(){
        return title;
    }
}
