package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.SensorService;

/**
 * Activity that displays details on the party to join and accomplishes the join.
 */
public class JoinPartyActivity extends AppCompatActivity {


    private NetworkService mNetworkService;
    private PartyJoinReceiver mPartyJoinReceiver;

    private boolean mBound;

    private Party party;
    private Person user;

    private ProgressDialog waitToJoinPartyDialog;
    private String[] partydata;
    private RecyclerView.Adapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_party);

        // set the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initially no user profile
        user = null;

        // set listern to button that brings you to the next activtiy (PartyActivity)
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if no user is selected
                if(user == null) {
                    waitToJoinPartyDialog = ProgressDialog.show(JoinPartyActivity.this, "Joining Party", "Please wait...", true, false);
                    mNetworkService.joinPartyRequest((NsdServiceInfo) getIntent().getParcelableExtra("Device"));
                } else{
                    // create intent that leads you to PartyActivity
                    Intent intent = new Intent(getApplicationContext(), PartyActivity.class);
                    // also transfer some data to this activity
                    intent.putExtra("Party", (Parcelable)party);
                    intent.putExtra("user", (Parcelable)user);
                    // start the PartyActivity
                    startActivity(intent);
                }
            }
        });

        // set up the view for showing the details of the party to join
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.party_recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), LinearLayout.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        LinearLayout listView = (LinearLayout) findViewById(R.id.party_listview);
        String[] titles = getResources().getStringArray(R.array.party_titles);

        // set adapter
        partydata = new String[4];
        adapter = new PartyAdapter(titles, partydata);
        mRecyclerView.setAdapter(adapter);

        // get the party data passed from the main activity
        party = (Party) getIntent().getParcelableExtra("Party");
        int num = getIntent().getIntExtra("NumGuests", 1);
        toolbar.setTitle(party.getName());

        partydata[0] = party.getHostName();
        partydata[1] = Integer.toString(num);
        partydata[2] = party.getGenre();
        partydata[3] = party.getDescription();

        this.setTitle(party.getName());
        adapter.notifyDataSetChanged();

        setupNetworkService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(mNetworkService.PARTY_JOINED_INTENT);
        registerReceiver(mPartyJoinReceiver, intentFilter);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPartyJoinReceiver != null) unregisterReceiver(mPartyJoinReceiver);
    }

    /**
     * Initialize the network service.
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
        // set receiver and bind service
        mPartyJoinReceiver = new PartyJoinReceiver();
        Intent intent = new Intent(getBaseContext(), NetworkService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Set the broadcast receiver
     */
    private class PartyJoinReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mNetworkService.PARTY_JOINED_INTENT)) {
                Intent i = new Intent(getBaseContext(), PartyActivity.class);
                i.putExtra("PartyName", party.getName());
                i.putExtra("PartyDescription", party.getDescription());
                waitToJoinPartyDialog.dismiss();
                startActivity(i);
                finish();
            }
        }
    }


    /**
     * Custom adapter that manages the recycler view.
     */
    public static class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.ViewHolder> {
        private String[] mTitles;
        private String[] mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView mTextViewTitle;
            public TextView mTextViewSub;
            public ViewHolder(View v) {
                super(v);
                mTextViewTitle = (TextView) v.findViewById(R.id.text1);
                mTextViewSub = (TextView) v.findViewById(R.id.text2);

                mTextViewTitle.setTypeface(null, Typeface.BOLD);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public PartyAdapter(String[] titles, String[] myDataset) {
            mTitles = titles;
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public JoinPartyActivity.PartyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                            int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_profile_list_item, parent, false);

            JoinPartyActivity.PartyAdapter.ViewHolder vh = new JoinPartyActivity.PartyAdapter.ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(JoinPartyActivity.PartyAdapter.ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.mTextViewTitle.setText(mTitles[position]);
            holder.mTextViewSub.setText(mDataset[position]);

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }

    }

}
