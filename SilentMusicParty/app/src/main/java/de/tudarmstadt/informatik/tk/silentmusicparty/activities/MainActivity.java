package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;
import de.tudarmstadt.informatik.tk.silentmusicparty.R;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;

/**
 * Activity that is called when the app is open. It provides i.a. the opportunity to search for parties
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private PartiesFoundReceiver partiesFoundReceiver;
    private NetworkService mNetworkService;
    private SwipeRefreshLayout swipeRefresh;
    private ArrayList<Party> partyList;
    private ArrayList<Integer> numGuestList;
    private ArrayList<NsdServiceInfo> deviceList;
    private ListView listView;
    private LinearLayout linearLayout;
    private boolean mBound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add button for getting to host activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), HostPartyActivity.class);
                startActivity(intent);
            }
        });

        // get the drawer and navigation views
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initialize the preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("UserUUID")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("UserUUID", UUID.randomUUID().toString());
            editor.putString("username", "Dancer"+ Math.round(Math.random()*1000));
            editor.apply();
        }
        setupNetworkService();

        // on swiping refresh the search for parties
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        // initialize lists
        partyList = new ArrayList<>();
        numGuestList = new ArrayList<>();
        deviceList = new ArrayList<NsdServiceInfo>();

        //
        listView = (ListView) findViewById(R.id.partylist);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        updatePartyListView(listView);

        // click listener for available parties list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // create intend that leads to next activity
                Intent intent = new Intent(getApplicationContext()/*this*/, JoinPartyActivity.class);
                intent.putExtra("myText", "" + position);
                intent.putExtra("Party", (Parcelable)partyList.get(position));
                intent.putExtra("NumGuests", numGuestList.get(position));
                intent.putExtra("Device", deviceList.get(position));
                startActivity(intent);
            }

        });
    }

    /**
     * Refresh the list of available parties.
     */
    private void refresh() {
        try {
            if (partiesFoundReceiver != null) unregisterReceiver(partiesFoundReceiver);
        } catch (IllegalArgumentException ex) {}

        IntentFilter intentFilter = new IntentFilter(NetworkService.PARTY_DISCOVERED_INTENT);
        intentFilter.addAction(NetworkService.PARTY_DISCOVERY_FAILED_INTENT);
        intentFilter.addAction(NetworkService.PARTY_JOINED_INTENT);
        registerReceiver(partiesFoundReceiver, intentFilter);
        partyList = new ArrayList<Party>();
        numGuestList = new ArrayList<Integer>();
        deviceList = new ArrayList<NsdServiceInfo>();
        mNetworkService.discoverParties();
        updatePartyListView(listView);
    }

    /**
     * Manage the connection between activity and service
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
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        partiesFoundReceiver = new PartiesFoundReceiver();

        // bind to network service
        Intent intent = new Intent(getBaseContext(), NetworkService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // exit activity to parent activity
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_search) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    refresh();
                    swipeRefresh.setRefreshing(true);
                }
            });
            // Handle the search action
        } else if (id == R.id.nav_host) {
            startActivity(new Intent(this, HostPartyActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ViewProfileActivity.class));
        } else if (id == R.id.nav_playlists) {
            startActivity(new Intent(this, PlaylistsActivity.class));
        } else if (id == R.id.nav_song_library) {
            startActivity(new Intent(this, SongLibraryActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Updates the list view that shows all available parties.
     */
    private void updatePartyListView(ListView listView){


        // container for a String version of the party list
        ArrayList<String> partyListAsString = new ArrayList<String>();
        // transform partylist into a String version
        for(Party party : partyList)
            partyListAsString.add(party.toString());

        // set up a standard adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1, partyListAsString) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(partyList.get(position).getName());
                text2.setText("Hosted by " + partyList.get(position).getHostName());
                return view;
            }
        };


        // put list into the view
        listView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class PartiesFoundReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NetworkService.PARTY_DISCOVERED_INTENT)) {
                // Do stuff - maybe update my view based on the changed DB contents
                swipeRefresh.setRefreshing(false);

                // get the packet data
                NsdServiceInfo device = intent.getParcelableExtra("Device");
                String partyUUID = intent.getStringExtra("UUID");
                String partyName = intent.getStringExtra("Name");
                String partyHost = intent.getStringExtra("Host");
                String partyDescription = intent.getStringExtra("Description");
                String partyLocation = intent.getStringExtra("Location");
                String partyGenre = intent.getStringExtra("Genre");

                // create a new party
                Person host = new Person("", partyHost);
                Party party = new Party(partyUUID, partyName, partyDescription, host, null, partyLocation, partyGenre);
                if (partyList.size() == 0) {
                    linearLayout.setVisibility(View.GONE);
                }
                for (int i = 0; i < partyList.size(); i++) {
                    if (partyUUID.equals(partyList.get(i).getUid())) {
                        return;
                    }
                }
                // add party and its properties (number of guests...) to the respective lists
                partyList.add(party);
                numGuestList.add(Integer.parseInt(intent.getStringExtra("NumGuests")));
                deviceList.add(device);

                // update the view
                updatePartyListView(listView);
            } else if (intent.getAction().equals(NetworkService.PARTY_DISCOVERY_FAILED_INTENT)) {
                swipeRefresh.setRefreshing(false);
            } else if (intent.getAction().equals(NetworkService.PARTY_JOINED_INTENT)) {
                swipeRefresh.setRefreshing(false);
                unregisterReceiver(partiesFoundReceiver);
                partyList.clear();
                numGuestList.clear();
                deviceList.clear();
                linearLayout.setVisibility(View.VISIBLE);
                updatePartyListView(listView);
            }
        }
    }

}
