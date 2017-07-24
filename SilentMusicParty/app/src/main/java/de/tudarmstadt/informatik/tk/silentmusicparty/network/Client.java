package de.tudarmstadt.informatik.tk.silentmusicparty.network;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;

/**
 * Created by Mohit on 26.02.2017.
 */

public class Client implements Runnable {

    private ClientThread client;

    private Socket socket;
    private String ip;
    private int port;
    private Person myself;
    private byte[] myPic;
    private long ping;
    private NetworkService mNetworkService;

    private ObjectOutputStream oos;

    public Client(String ip, int port, Person me, Bitmap myPic, NetworkService networkService) {
        this.ip = ip;
        this.port = port;
        myself = me;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (myPic != null) myPic.compress(Bitmap.CompressFormat.PNG, 100, stream);
        this.myPic = stream.toByteArray();
        mNetworkService = networkService;
    }
    @Override
    public void run() {
        try {
            socket = new Socket(ip, port);
            client = new ClientThread(this, socket);
            client.start();
            oos = new ObjectOutputStream(socket.getOutputStream());
            //bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            ping = System.currentTimeMillis();
            sendMessage("JOIN", new Object[] {myself, myPic, ping});
            //Message m = new Message("JOIN", myself);
            //oos.writeObject(m);
            //oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void interrupt() {
        client.interrupt();
    }

    /**
     * Sends a message object to the server
     * @param tag tag attribute of message
     * @param data data attribute
     */
    public void sendMessage(final String tag, final Object data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Message m = new Message(tag, data);
                    oos.writeObject(m);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * handles a received message
     * @param m
     */
    public void handle(Message m) {
        String tag = m.getTag();
        if (tag.equals("PARTY")) {
            mNetworkService.joinParty((Party)m.getData(), ping);
        } else if (tag.equals("JOIN")){
            Object[] o = (Object[])m.getData();
            Person pers = (Person)o[0];
            byte[] b = (byte[])o[1];
            mNetworkService.guestJoined(pers, b);
        } else if (tag.equals("SHOUT")) {
            String[] s = (String[])m.getData();
            mNetworkService.shoutReceived(s[0],s[1]);
        } else if (tag.equals("PLAYERPOS")) {
            long[] l = (long[])m.getData();
            mNetworkService.playerPosReceived(l[0],l[1]);
        } else if (tag.equals("LEFT")) {
            mNetworkService.leftParty((String)m.getData());
        } else if (tag.equals("CLOSE")) {
            mNetworkService.partyClosed();
        } else if (tag.equals("KICK")) {
            mNetworkService.kick((String)m.getData());
        } else if (tag.equals("SONGUPDATEUP")){
            Song song = (Song) m.getData();
            Log.d("VOTES IN SONG", "" + song.getVotes());
            mNetworkService.updateSong(song, 1);
        } else if (tag.equals("SONGUPDATEDOWN")){
            Song song = (Song) m.getData();
            Log.d("VOTES IN SONG", "" + song.getVotes());
            mNetworkService.updateSong(song, -1);
        } else if (tag.equals("NEXTSONG")){
            Song song = (Song) m.getData();
            Log.d("NEXT", "song is " + song.getTitle());
            mNetworkService.playSong(song);
        } else if (tag.equals("ADD_SONG")){
            Log.d("CLIENT", "received ADDSONG");
            Song song = (Song) m.getData();
            mNetworkService.songReceived(song);
        }
    }
}
