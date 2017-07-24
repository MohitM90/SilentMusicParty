package de.tudarmstadt.informatik.tk.silentmusicparty.network;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.MediaPlayerService;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;

/**
 * Created by Mohit on 26.02.2017.
 */

public class Server implements Runnable {

    private NetworkService mNetworkService;
    private MediaPlayerService mMediaPlayerService;

    private ServerSocket mServerSocket;
    private int mLocalPort;

    private ArrayList<ServerThread> clients;
    private ArrayList<Person> persons;

    public Server(NetworkService networkService) {
        mNetworkService = networkService;
        clients = new ArrayList<>();
        persons = new ArrayList<>();

        try {
            // Initialize a server socket on the next available port.
            mServerSocket = new ServerSocket(0);

            // Store the chosen port.
            mLocalPort =  mServerSocket.getLocalPort();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Log.d("Server", "Server started and awaiting connections.");
                Socket sock = mServerSocket.accept();
                Log.d("Server", "Connected");
                ServerThread st = new ServerThread(this, sock);
                clients.add(st);
                st.start();
                //Communication comm = new Communication(mNetworkService, sock);
                //new Thread(comm).start();

            } catch (IOException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    public void interrupt() {
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).interrupt();
        }
    }

    /**
     * Handles received messsage from client
     * @param m message
     */
    public synchronized void handle(Message m) {
        String tag = m.getTag();
        if (tag.equals("JOIN")) {
            Object[] o = (Object[])m.getData();
            Person pers = (Person)o[0];
            byte[] b = (byte[])o[1];
            mNetworkService.guestJoined(pers, b);
            Message m2 = new Message("PARTY", mNetworkService.getParty());
            clients.get(persons.size()).sendMessage(m2);
            for (int i = 0; i < persons.size(); i++) {
                clients.get(i).sendMessage(m);
            }

            persons.add(pers);
        } else if (tag.equals("LEAVE")) {
            String uid = (String)m.getData();

            for (int i = 0; i < persons.size(); i++) {
                if (persons.get(i).getUid().equals(uid)) {
                    clients.remove(i).interrupt();
                    persons.remove(i);
                    i--;
                }
            }
            mNetworkService.leftParty(uid);
            sendBroadcast("LEFT", m.getData());
            //TODO: notify all clients & NetworkService
        } else if (tag.equals("SHOUT")) {
            String[] s = (String[])m.getData();
            mNetworkService.shoutReceived(s[0],s[1]);
            for (int i = 0; i < persons.size(); i++) {
                if (!persons.get(i).getUid().equals(s[0])) {
                    clients.get(i).sendMessage(m);
                }
            }
        } else if (tag.equals("UPVOTE")) {
            Song song = (Song) m.getData();
            mNetworkService.voteReceived(song, 1);

        } else if (tag.equals("DOWNVOTE")) {
            Song song = (Song) m.getData();
            mNetworkService.voteReceived(song, -1);
        } else if (tag.equals("ADDSONG")) {
            Song song = (Song) m.getData();
            mNetworkService.sendSong(song);
        }
    }

    public synchronized void sendBroadcast(String tag, Object o) {
        Message m = new Message(tag, o);
        for (int i = 0; i < clients.size(); i++) {
            Log.d("BROADCASTED", "to " + i);
            clients.get(i).sendMessage(m);
        }
    }

    public synchronized void kick(String uid) {
        for (int i = 0; i < persons.size(); i++) {
            if (persons.get(i).getUid().equals(uid)) {
                clients.remove(i).interrupt();
                persons.remove(i);
                i--;
            }
        }
    }
    public synchronized void sendMessage(String uuid, String tag, Object o) {
        Message m = new Message(tag, o);
        for (int i = 0; i < clients.size(); i++) {
            if (persons.get(i).getUid().equals(uuid)) {
                clients.get(i).sendMessage(m);
                break;
            }
        }
    }
    public int getLocalPort() {
        return mLocalPort;
    }
}
