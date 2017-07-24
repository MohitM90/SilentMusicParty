package de.tudarmstadt.informatik.tk.silentmusicparty.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.youview.tinydnssd.DiscoverResolver;
import com.youview.tinydnssd.MDNSDiscover;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;
import de.tudarmstadt.informatik.tk.silentmusicparty.network.Client;
import de.tudarmstadt.informatik.tk.silentmusicparty.network.Server;

/**
 * Network service the mediates between the network and the activities.
 */
public class NetworkService extends Service {
    public static final String SERVICE_REG_TYPE = "_smp._tcp.";

    public static final String PARTY_DISCOVERED_INTENT = "PartyDiscoveredIntent";
    public static final String PARTY_DISCOVERY_FAILED_INTENT = "PartyDiscoveryFailedIntent";
    public static final String GUEST_JOINED_INTENT = "GuestJoinedIntent";
    public static final String GUEST_LEFT_INTENT = "GuestLeftIntent";
    public static final String GUEST_KICKED_INTENT = "GuestKickedIntent";
    public static final String PARTY_JOINED_INTENT = "PartyJoinedIntent";
    public static final String SHOUTBOX_MESSAGE_INTENT = "ShoutboxMessageIntent";
    public static final String PARTY_CLOSED_INTENT = "PartyClosedIntent";
    public static final String UPDATE_SONG_INTENT = "UpdateSongIntent";
    public static final String NEXT_SONG_INTENT = "NextSongIntent";
    public static final String NEW_SONG_INTENT = "NewSongIntent";

    private final IBinder mBinder = new NetworkBinder();

    private NsdServiceInfo mService;
    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;

    private HashMap<String, Socket> connections;
    private Client client;
    private Socket socket;

    private String mServiceName;

    private Party party;
    private Song currentSong;

    private Server server;

    private long ping;

    /**
     * Constructor
     */
    public NetworkService() {
    }

    /**
     * Decide which song to play next.
     */
    public void determineNextSong() {
        // SERVER
        if(client == null) {
            // sort the list
            ArrayList<Song> songs = party.getSonglist();
            Collections.sort(songs, new Comparator<Song>() {
                public int compare(Song o1, Song o2) {
                    return o2.getVotes() - o1.getVotes();
                }
            });

            // get the uppermost song
            Song nextSong = songs.get(0);
            nextSong.setStartTime(System.currentTimeMillis());
            party.setCurrentSong(nextSong);

            // broadcast the selected song
            server.sendBroadcast("NEXTSONG", nextSong);
            playSong(nextSong);
        }
    }

    /**
     * Signal that a new song can be played by broadcasting an intent.
     *
     * @param song
     */
    public void playSong(Song song) {
        Intent local = new Intent();
        local.setAction(NEXT_SONG_INTENT);
        local.putExtra("nextsong", (Parcelable) song);
        this.sendBroadcast(local);

    }

    /**
     * Inform that a new song should be added to songlist.
     *
     * @param song
     */
    public void addSong(Song song) {
        // SERVER
        if(client == null){
            sendSong(song);
        // CLIENT
        } else {
            client.sendMessage("ADDSONG", song);
        }
    }

    /**
     * Broadcast song to all clients.
     *
     * @param song
     */
    public void sendSong(Song song){
        for(Song s : party.getSonglist()){
            if(Song.Compare(song,s)){
                return;
            }
        }
        //party.getSonglist().add(song);
        server.sendBroadcast("ADD_SONG", song);

        songReceived(song);

    }

    /**
     * Reception of a song triggers a respective intent.
     * @param song
     */
    public void songReceived(Song song) {
        Intent local = new Intent();
        local.putExtra("newsong", (Parcelable) song);
        local.setAction(NEW_SONG_INTENT);
        this.sendBroadcast(local);
    }


    /**
     * Bind to network service
     */
    public class NetworkBinder extends Binder {
        public NetworkService getService() {
            // Return this instance of NetworkService so clients can call public methods
            return NetworkService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Start hosting a party.
     * @param party
     */
    public void hostParty(Party party) {
        this.party = new Party(party);
        startServer();
        registerService();
    }

    private void startServer() {
        server = new Server(this);
        new Thread(server).start();
    }

    private void registerService() {
        String uid = party.getUid();
        connections = new HashMap<>(5);
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        if (mRegistrationListener != null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName(uid);
        serviceInfo.setServiceType(SERVICE_REG_TYPE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            serviceInfo.setAttribute("Name", party.getName());
            serviceInfo.setAttribute("Host", party.getHostName());
            serviceInfo.setAttribute("Description", party.getDescription());
            serviceInfo.setAttribute("Location", party.getLocation());
            serviceInfo.setAttribute("Genre", party.getGenre());
            serviceInfo.setAttribute("NumGuests", "" + party.getNumberGuests());
        }
        initializeRegistrationListener();
        serviceInfo.setPort(server.getLocalPort());

        mNsdManager = (NsdManager)getBaseContext().getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                Log.d("NetworkService", "Service registration success");
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d("NetworkService", "Service registration failed: " + errorCode);
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

    /**
     * Discovery hosted parties in the network.
     */
    public void discoverParties() {
        if (mNsdManager == null) {
            mNsdManager = (NsdManager)this.getSystemService(Context.NSD_SERVICE);
        } else {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (IllegalArgumentException ex) {}
        }
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_REG_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    DiscoverResolver resolver;
    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            public static final String TAG = "NsdHelper";

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_REG_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else {
                    initializeResolveListener();
                    try {
                        resolver.start();
                        Log.d(TAG, "resolver started");
                    } catch (Exception ex) {
                        resolver.start();
                    }
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                Intent i = new Intent(PARTY_DISCOVERY_FAILED_INTENT);
                sendBroadcast(i);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);

            }
        };
    }

    public void initializeResolveListener() {
        resolver = new DiscoverResolver(getBaseContext(), SERVICE_REG_TYPE, new DiscoverResolver.Listener() {
            public static final String TAG = "NsdHelper";
            @Override
            public void onServicesChanged(Map<String, MDNSDiscover.Result> services) {
                Log.d(TAG, "try resolving...");
                for (MDNSDiscover.Result result : services.values()) {
                    if (result.txt.dict.size() > 1) {
                        Log.e(TAG, "Resolve Succeeded.");
                        Map<String, String> record = result.txt.dict;
                        Intent i = new Intent(PARTY_DISCOVERED_INTENT);
                        NsdServiceInfo serviceInfo = new NsdServiceInfo();
                        try {
                            serviceInfo.setHost(InetAddress.getByName(result.a.ipaddr));
                            serviceInfo.setPort(result.srv.port);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        i.putExtra("Device", serviceInfo);
                        i.putExtra("UUID", result.srv.fqdn.split("\\.")[0]);
                        i.putExtra("Name", record.get("Name"));
                        i.putExtra("Host", record.get("Host"));
                        i.putExtra("Description", record.get("Description"));
                        i.putExtra("Location", record.get("Location"));
                        i.putExtra("Genre", record.get("Genre"));
                        i.putExtra("NumGuests", record.get("NumGuests"));
                        sendBroadcast(i);
                    }
                }
            }
        }, 1000);
    }

    /**
     * Process the request of a user to join the party.
     * @param info
     */
    public void joinPartyRequest(NsdServiceInfo info) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String defaultUsername = getResources().getString(R.string.pref_default_username);
        String[] defaultProfile = getResources().getStringArray(R.array.pref_default_profile);

        // get all the user properties
        String uid = prefs.getString("UserUUID", "");
        String hostName = prefs.getString("username", defaultUsername);
        String birthday = prefs.getString("birthday", defaultProfile[0]);
        int height = Integer.parseInt(prefs.getString("height", defaultProfile[1]));
        int gender = Integer.parseInt(prefs.getString("gender", defaultProfile[2]));
        String location = prefs.getString("location", defaultProfile[3]);
        String message= prefs.getString("message", defaultProfile[4]);

        // create user
        Person me = new Person(uid, hostName, birthday, height, gender, location, message);

        // try to get picture
        Bitmap myPic = null;
        try {
            FileInputStream fis = new FileInputStream(getFileStreamPath("profilepic.jpg"));
            myPic = BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String ip = info.getHost().getHostAddress();
        client = new Client(ip, info.getPort(), me, myPic, this);

        new Thread(client).start();
        //c.joinPartyRequest();

    }

    /**
     * Become part of the party.
     * @param party
     * @param ping
     */
    public void joinParty(Party party, long ping) {
        this.ping = System.currentTimeMillis()-ping;
        this.party = party;
        Intent i = new Intent(PARTY_JOINED_INTENT);
        //i.putExtra("Party", (Parcelable)party);
        sendBroadcast(i);
    }

    public void guestJoined(Person person, byte[] pic) {
        Log.d("Network", "Guest " + person.getName() + " joined");
        party.joinParty(person, pic);
        if (server != null) registerService();
        Intent i = new Intent(GUEST_JOINED_INTENT);
        i.putExtra("Person", (Parcelable)person);
        sendBroadcast(i);

    }

    /**
     * Send the position the mediaplayer should be set to.
     *
     * @param uuid
     * @param currentPos
     * @param currentTime
     */
    public void sendPlayerPos(String uuid, int currentPos, long currentTime) {
        if (server != null) server.sendMessage(uuid, "PLAYERPOS", new long[] {currentPos, currentTime});
    }

    /**
     * On receive of media player position set the times in the player
     * @param currentPos
     * @param sendTime
     */
    public void playerPosReceived(long currentPos, long sendTime) {
        MediaPlayerService.startPos = currentPos;
        MediaPlayerService.setTime = System.currentTimeMillis();
    }

    /**
     * Send a chat message for the shoutbox.
     *
     * @param uid
     * @param message
     */
    public void sendShout(String uid, String message) {
        if (server != null) {
            server.sendBroadcast("SHOUT", new String[] {uid, message});
        }
        else {
            client.sendMessage("SHOUT", new String[] {uid, message});
        }
    }


    /**
     * Triger vote updating after a vote has been received.
     * @param song
     * @param vote
     */
    public void voteReceived(Song song, int vote) {
        ArrayList<Song> songs = party.getSonglist();

        Song s = song;
        // when song is found up-/downvote it
        for(int i = 0; i < songs.size(); i++){
            s = songs.get(i);

            if(Song.Compare(s, song)) {
                if (vote == 1) {
                    s.upvote();
                    server.sendBroadcast("SONGUPDATEUP", s);
                }
                else if (vote == -1) {
                    s.downvote();
                    server.sendBroadcast("SONGUPDATEDOWN", s);
                }
                break;
            }
        }
        // sort the list to provide a sorted view
        sortSonglist();

        // trigger to update view
        Intent local = new Intent();
        local.setAction(UPDATE_SONG_INTENT);
        this.sendBroadcast(local);
    }

    private void sortSonglist() {
        Collections.sort(party.getSonglist(), new Comparator<Song>() {
            public int compare(Song o1, Song o2) {
                return o2.getVotes() - o1.getVotes();
            }
        });
    }

    public void sendVote(Song song, int vote) {
        // in case it's the HOST
        if(client == null) {
            ArrayList<Song> songs = party.getSonglist();
            int i = 0;
            for(i = 0; i < songs.size(); i++){
                if(songs.get(i).getTitle().equals(song.getTitle())) {
                    // UPVOTE
                    if (vote == 1) {
                        songs.get(i).upvote();
                        server.sendBroadcast("SONGUPDATEUP", songs.get(i));
                    }
                    // DOWNVOTE
                    else if (vote == -1) {
                        songs.get(i).downvote();
                        server.sendBroadcast("SONGUPDATEDOWN", songs.get(i));
                    }
                    break;
                }
            }

            sortSonglist();

            Intent local = new Intent();
            local.setAction(UPDATE_SONG_INTENT);
            this.sendBroadcast(local);
        }
        // in case it's a CLIENT
        else if (client != null){
            if(vote == 1)
                client.sendMessage("UPVOTE", song);
            else if(vote == -1)
                client.sendMessage("DOWNVOTE", song);
        }
    }

    /**
     * Update songlist by new song.
     * @param song
     * @param vote
     */
    public void updateSong(Song song, int vote) {
        party.updateSongList(song, vote);
        sortSonglist();

        Intent local = new Intent();
        local.setAction(UPDATE_SONG_INTENT);
        this.sendBroadcast(local);

    }

    /**
     * Chat message from shoutbox received.
     * @param uid
     * @param message
     */
    public void shoutReceived(String uid, String message) {
        Intent i = new Intent(SHOUTBOX_MESSAGE_INTENT);
        String name = "unknown";
        for (Person p : party.getMemberList().keySet()) {
            if (p.getUid().equals(uid)) {
                name = p.getName();
                break;
            }
        }
        i.putExtra("Name", name);
        i.putExtra("Message", message);
        sendBroadcast(i);
    }

    public void leaveParty(String uid) {
        client.sendMessage("LEAVE", uid);
    }

    /**
     * Trigger a guestlist update after a guest left the party.
     * @param uid
     */
    public void leftParty(String uid) {
        HashMap<Person, byte[]> partylist = party.getMemberList();
        Iterator<Person> iP = partylist.keySet().iterator();
        while (iP.hasNext()) {
            Person p = iP.next();
            if (uid.equals(p.getUid())) {
                iP.remove();
            }
        }
        if (server != null) registerService();
        Intent i = new Intent(GUEST_LEFT_INTENT);
        sendBroadcast(i);
    }

    public void closeParty() {
        if (server != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mRegistrationListener = null;
            server.sendBroadcast("CLOSE", null);
            server.interrupt();
            server = null;
        }
    }

    public void partyClosed() {
        if (client != null) client.interrupt();
        Intent i = new Intent(PARTY_CLOSED_INTENT);
        sendBroadcast(i);
        client = null;
    }


    public void kick(String uid) {
        if (server != null) {
            server.sendBroadcast("KICK", uid);
            server.kick(uid);
        }
        leftParty(uid);
        Intent i = new Intent(GUEST_KICKED_INTENT);
        i.putExtra("Guest", uid);
        sendBroadcast(i);
    }

    public Party getParty() {
        return party;
    }

    public long getPing() {
        return ping;
    }

}
