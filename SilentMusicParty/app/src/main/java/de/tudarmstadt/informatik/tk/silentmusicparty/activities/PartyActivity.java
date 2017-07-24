package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.tudarmstadt.informatik.tk.silentmusicparty.SonglistAdapter;
import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.MediaPlayerService;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.SensorService;

/**
 * Activity that deals with party related stuff, like playlist management, etc.
 */
public class PartyActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MediaPlayerService mPlayer;
    private Timer updateTimer;

    private NetworkService mNetworkService;
    private SensorService mSensor;
    private BroadcastReceiver updateUIReciver;

    private boolean mBound;
    private TabLayout tabLayout;


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // set the navigation view
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_song);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist)mSectionsPagerAdapter.getFragment(0);
                switch (tab.getPosition()) {
                    case 0: navigationView.setCheckedItem(R.id.nav_song);
                        if(!f.adapter.getIsDancing())
                            f.snackbar.setText("You must dance in order to vote").show();
                        else
                            f.snackbar.dismiss();
                        break;
                    case 1: navigationView.setCheckedItem(R.id.nav_guests); f.snackbar.dismiss(); break;
                    case 2:
                        navigationView.setCheckedItem(R.id.nav_shoutbox);
                        tabLayout.getTabAt(2).setText("SHOUTBOX");
                        f.snackbar.dismiss();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // set the header of the navigation view
        View header = navigationView.getHeaderView(0);
        TextView txtName = (TextView)header.findViewById(R.id.partyName);
        TextView txtDescription = (TextView)header.findViewById(R.id.partyDescription);

        //-------- Instantiate the MediaPlayer Service--------
        Intent mediaplayerServiceIntent = new Intent(this, MediaPlayerService.class);
        mediaplayerServiceIntent.setAction("com.example.action.PLAY");

        // need to bind to service
        bindService(mediaplayerServiceIntent, mServerConn, Context.BIND_AUTO_CREATE);
        startService(mediaplayerServiceIntent);

        //-------- Instantiate the Sensor Service--------
        Log.d("SERVICE", "start the sensor service class");
        Intent sensorServiceIntent = new Intent(this, SensorService.class);
        // need to bind to service
        bindService(sensorServiceIntent, serverConnSensor, Context.BIND_AUTO_CREATE);
        startService(sensorServiceIntent);

        // set a broadcast receiver
        updateUIReciver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // update the UI
                if (intent.getAction().equals(NetworkService.UPDATE_SONG_INTENT)) {
                    PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist) mSectionsPagerAdapter.getFragment(0);
                    f.adapter.notifyDataSetChanged();
                }
                // is-dancing changed
                else if (intent.getAction().equals(SensorService.DANCING_UPDATE)) {
                    Boolean isDancing = intent.getExtras().getBoolean("dancing");
                    long remainingTime= intent.getLongExtra("timeleft", 0);
                    PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist)mSectionsPagerAdapter.getFragment(0);
                    f.updateUI(isDancing);

                    // in case the songlist tab is selected
                    if(tabLayout.getSelectedTabPosition()== 0) {
                        if (isDancing) {
                            // for the last 10 sec
                            if (remainingTime / 1000 < 11)
                                f.snackbar.setText(remainingTime / 1000 + " sec remaining").show();
                            else
                                f.snackbar.dismiss();
                        } else if (!isDancing) {
                            f.snackbar.setText("You must dance in order to vote").show();
                        }
                    } else
                        f.snackbar.dismiss();
                    // guest joined the party
                } else if (intent.getAction().equals(NetworkService.GUEST_JOINED_INTENT)) {
                    PartyFragmentTab2Guests f = (PartyFragmentTab2Guests)mSectionsPagerAdapter.getFragment(1);
                    f.showGuestList(mNetworkService.getParty());
                    Person p = intent.getParcelableExtra("Person");
                    mNetworkService.sendPlayerPos(p.getUid(), mPlayer.getCurrentPosition(),System.currentTimeMillis());
                    // new chat message
                } else if (intent.getAction().equals(NetworkService.SHOUTBOX_MESSAGE_INTENT)) {
                    PartyFragmentTab3Shoutbox f = (PartyFragmentTab3Shoutbox) mSectionsPagerAdapter.getFragment(2);
                    String name = intent.getStringExtra("Name");
                    String message = intent.getStringExtra("Message");
                    if (mViewPager.getCurrentItem() != 2) tabLayout.getTabAt(2).setText("SHOUTBOX ●");
                    f.receiveMessage(name, message);
                    // guest left party
                } else if (intent.getAction().equals(NetworkService.GUEST_LEFT_INTENT)) {
                    PartyFragmentTab2Guests f = (PartyFragmentTab2Guests)mSectionsPagerAdapter.getFragment(1);
                    f.showGuestList(mNetworkService.getParty());
                    // guest got kicked out
                } else if (intent.getAction().equals(NetworkService.GUEST_KICKED_INTENT)){
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    String myUUID = prefs.getString("UserUUID",null);
                    String kickedUUID = intent.getStringExtra("Guest");
                    if (myUUID.equals(kickedUUID)) {
                        new AlertDialog.Builder(PartyActivity.this)
                                .setTitle("KICKED")
                                .setMessage("You have been kicked out of the Party!")
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        PartyActivity.super.onBackPressed();
                                    }
                                }).create().show();
                    } else {
                        PartyFragmentTab2Guests f = (PartyFragmentTab2Guests)mSectionsPagerAdapter.getFragment(1);
                        f.showGuestList(mNetworkService.getParty());
                    }
                    // party is over
                } else if (intent.getAction().equals(NetworkService.PARTY_CLOSED_INTENT)) {
                    new AlertDialog.Builder(PartyActivity.this)
                            .setTitle("PARTY CLOSED")
                            .setMessage("The Party has been closed by the Host!")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PartyActivity.super.onBackPressed();
                                }
                            }).create().show();
                    // play next song
                } else if(intent.getAction().equals(NetworkService.NEXT_SONG_INTENT)) {

                    // get song and set media player
                    Song song = intent.getParcelableExtra("nextsong");
                    mPlayer.playSong(song, mNetworkService.getPing());

                    // remove the song from list (since now it will be played
                    ArrayList<Song> list = mNetworkService.getParty().getSonglist();
                    for (int i = 0; i < list.size(); i++) {
                        if (Song.Compare(list.get(i), song)) {
                            list.remove(i);
                            break;
                        }
                    }

                    PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist) mSectionsPagerAdapter.getFragment(0);
                    f.getSongChosen();

                    SonglistAdapter.setTexts(getWindow().getDecorView().getRootView(), song, R.id.current_song, R.id.current_artist);
                    f.adapter.resetVotes();
                    // add a new song to songlist
                } else if(intent.getAction().equals(NetworkService.NEW_SONG_INTENT)){
                    Song song = intent.getParcelableExtra("newsong");
                    ArrayList<Song> list = mNetworkService.getParty().getSonglist();
                    list.add(song);

                    // reset list and view
                    PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist)mSectionsPagerAdapter.getFragment(0);
                    f.adapter.setList(list, true);
                    f.adapter.notifyDataSetChanged();
                }
            }
        };

        // set the party name and description
        String partyname = getIntent().getStringExtra("PartyName");
        String partydesc = getIntent().getStringExtra("PartyDescription");
        this.setTitle(partyname);
        txtName.setText(partyname);
        txtDescription.setText(partydesc);

        // create intent filter and actions
        IntentFilter intentFilter = new IntentFilter(NetworkService.GUEST_JOINED_INTENT);
        intentFilter.addAction(SensorService.DANCING_UPDATE);
        intentFilter.addAction(NetworkService.SHOUTBOX_MESSAGE_INTENT);
        intentFilter.addAction(NetworkService.GUEST_LEFT_INTENT);
        intentFilter.addAction(NetworkService.GUEST_KICKED_INTENT);
        intentFilter.addAction(NetworkService.PARTY_CLOSED_INTENT);
        intentFilter.addAction(NetworkService.UPDATE_SONG_INTENT);
        intentFilter.addAction(NetworkService.NEXT_SONG_INTENT);
        intentFilter.addAction(NetworkService.NEW_SONG_INTENT);
        registerReceiver(updateUIReciver, intentFilter);

        setupNetworkService();
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String uid = prefs.getString("UserUUID",null);
        String text = "leave";
        boolean isHost = false;
        if (uid.equals(this.mNetworkService.getParty().getHost().getUid())) {
            text = "close";
            isHost = true;
        }
        // set dialog for leaving the party
        final boolean isHost2 = isHost;
        new AlertDialog.Builder(this)
                .setTitle(text.toUpperCase() + " PARTY?")
                .setMessage("Do you really want to " + text + " the party?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveParty(isHost2);
                        PartyActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_song) {
            tabLayout.getTabAt(0).select();
        } else if (id == R.id.nav_guests) {
            tabLayout.getTabAt(1).select();
        } else if (id == R.id.nav_shoutbox) {
            tabLayout.getTabAt(2).select();
        } else if (id == R.id.nav_leave) {
            onBackPressed();
        } else if (id == R.id.nav_add_song_songlist){
            // change activity
            startActivityForResult(new Intent(this, AddSongActivity.class), 2134);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Establishes connection to media player service
     */
    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {

            MediaPlayerService.LocalBinder mLocalBinder = (MediaPlayerService.LocalBinder) binder;
            mPlayer = mLocalBinder.getServerInstance();
            updateTimer = new Timer();
            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (mPlayer.isPlaying()) {
                        PartyFragmentTab1Playlist f = (PartyFragmentTab1Playlist)mSectionsPagerAdapter.getFragment(0);
                        f.updatePlayer(mPlayer.getCurrentPosition(), mPlayer.getDuration());
                    }
                }
            },0, 1000);

            // initialization for the initial songplay
            Song song = mNetworkService.getParty().getCurrentSong();
            SonglistAdapter.setTexts(getWindow().getDecorView().getRootView(), song, R.id.current_song, R.id.current_artist);

            mPlayer.playSong(song, 0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayer = null;
            updateTimer.cancel();
            updateTimer.purge();
        }
    };

    /**
     * Establishes the connection to the sensor service
     */
    protected ServiceConnection serverConnSensor = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            SensorService.LocalBinder mLocalBinder = (SensorService.LocalBinder) binder;
            mSensor = mLocalBinder.getServerInstance();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * On destroy make sure that the service connections are unbind
     */
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServerConn);
        mPlayer.stopSelf();
        unbindService(serverConnSensor);
        mSensor.stopSelf();
        unregisterReceiver(updateUIReciver);
    }

    /**
     * Setup the the network service.
     */
    private void setupNetworkService() {
        ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to NetworkService, cast the IBinder and get NetworkService instance
                NetworkService.NetworkBinder binder = (NetworkService.NetworkBinder) service;
                mNetworkService = binder.getService();
                mBound = true;

                // display the guest list
                PartyFragmentTab2Guests f1 = (PartyFragmentTab2Guests)mSectionsPagerAdapter.getFragment(1);
                if (f1 != null) f1.showGuestList(mNetworkService.getParty());

                // display the songlist
                PartyFragmentTab1Playlist f0 = (PartyFragmentTab1Playlist)mSectionsPagerAdapter.getFragment(0);
                if (f0 != null) {
                    f0.adapter.setList(mNetworkService.getParty().getSonglist(), false);
                    f0.adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        Intent intent = new Intent(getBaseContext(), NetworkService.class);

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * User leaves party. If it's the host -> close party.
     * @param isHost
     */
    public void leaveParty(boolean isHost) {
        if (isHost) {
            mNetworkService.closeParty();
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String uid = prefs.getString("UserUUID",null);
            mNetworkService.leaveParty(uid);
        }
    }

    /**
     * Force a user to leave the party.
     * @param uid
     */
    public void kick(String uid) {
        mNetworkService.kick(uid);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private SparseArrayCompat<Fragment> mFragments;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragments = new SparseArrayCompat<>();
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return the current tabs
            Fragment f = null;
            switch (position) {
                case 0:
                    f = Fragment.instantiate(getBaseContext(), PartyFragmentTab1Playlist.class.getName());
                    break;
                case 1:
                    f = Fragment.instantiate(getBaseContext(), PartyFragmentTab2Guests.class.getName());
                    break;
                case 2:
                    f = Fragment.instantiate(getBaseContext(), PartyFragmentTab3Shoutbox.class.getName());
                    break;
                default:
                    f = null;
                    break;
            }
            return f;
        }

        public Fragment getFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragments.remove(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, f);
            return f;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "PLAYLIST";
                case 1:
                    return "GUESTS";
                case 2:
                    return "SHOUTBOX";
            }
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            // get the song that should be added
            Song song = (Song) data.getParcelableExtra("song");
            if(Song.Contains(getNetworkService().getParty().getSonglist(), song))
                Toast.makeText(this, "Song is already in songlist!" , Toast.LENGTH_LONG).show();
            mNetworkService.addSong(song);
        }
    }

    public MediaPlayerService getPlayer() {
        return mPlayer;
    }
    public NetworkService getNetworkService() { return mNetworkService; }
}
