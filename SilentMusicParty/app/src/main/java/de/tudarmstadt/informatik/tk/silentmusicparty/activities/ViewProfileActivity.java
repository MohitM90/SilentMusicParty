package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.activities.EditProfileActivity;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;

/* TODO: This Acitivity can be extended to view profiles of other users as well.
 *       Set the FloatingActionButton invisible in that case.
 */
public class ViewProfileActivity extends AppCompatActivity {

    private static Person guest;
    private static byte[] guestPic;

    String defaultUsername;
    String[] defaultProfile;
    String[] profile;

    RecyclerView.Adapter adapter;

    ImageView profilePic;

    Toolbar toolbar;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(view.getContext(), EditProfileActivity.class), 1);
            }
        });
        if (getIntent().hasExtra("Guest")) {
            fab.setVisibility(View.INVISIBLE);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.profile_recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), LinearLayout.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        LinearLayout listView = (LinearLayout) findViewById(R.id.profile_listview);
        String[] titles = getResources().getStringArray(R.array.profile_titles);
        profile = new String[5];
        /*List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (int i=0; i<titles.length; i++) {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("title", titles[i]);
            datum.put("subtitle", values[i]);
            data.add(datum);
        }*/

        adapter = new ProfileAdapter(titles, profile);
        mRecyclerView.setAdapter(adapter);

        defaultUsername = getResources().getString(R.string.pref_default_username);
        defaultProfile = getResources().getStringArray(R.array.pref_default_profile);

        profilePic =(ImageView) findViewById(R.id.imageView4);
    }

    @Override
    protected void onResume() {
        //super.onResume();
        if (!getIntent().hasExtra("Guest")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String[] genders = getResources().getStringArray(R.array.gender);
            //toolbar.invalidate();
            toolbar.setTitle(prefs.getString("username", defaultUsername));

            profile[0] = prefs.getString("birthday", defaultProfile[0]);
            profile[1] = prefs.getString("height", defaultProfile[1]) + " cm";
            profile[2] = genders[Integer.parseInt(prefs.getString("gender", defaultProfile[2]))];
            profile[3] = prefs.getString("location", defaultProfile[3]);
            profile[4] = prefs.getString("message", defaultProfile[4]);

            try {
                FileInputStream fis = new FileInputStream(getFileStreamPath("profilepic.jpg"));
                Bitmap bmp = BitmapFactory.decodeStream(fis);
                profilePic.setImageBitmap(bmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            this.setTitle(guest.getName());
            profile[0] = guest.getBirthday();
            profile[1] = guest.getHeight() + " cm";
            profile[2] = guest.getGender();
            profile[3] = guest.getLocation();
            profile[4] = guest.getMessage();
            Bitmap bmp = BitmapFactory.decodeByteArray(guestPic, 0, guestPic.length);
            profilePic.setImageBitmap(bmp);
        }

        adapter.notifyDataSetChanged();
        super.onResume();
    }

    public void showImage(View v) {
        Dialog builder = new Dialog(this);

        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.argb(255, 0x3F, 0x51, 0xB5)));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });
        ImageView im = (ImageView)findViewById(R.id.imageView4);
        ImageView imageView = new ImageView(this);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        imageView.setMinimumWidth(size.x);
        imageView.setMinimumHeight(size.y);
        imageView.setImageDrawable(im.getDrawable());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        builder.setContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        //getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        builder.show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //this.setTitle(prefs.getString("username", defaultUsername));
        toolbar.invalidate();
    }

    public static void setGuest(Person p, byte[] b) {
        guest = p;
        guestPic = b;
    }

    public static class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
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
                mTextViewTitle = (TextView) v.findViewById(R.id.text1/*android.R.id.text1*/);
                mTextViewSub = (TextView) v.findViewById(R.id.text2/*android.R.id.text2*/);

                mTextViewTitle.setTypeface(null, Typeface.BOLD);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public ProfileAdapter(String[] titles, String[] myDataset) {
            mTitles = titles;
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ProfileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_profile_list_item/*android.R.layout.simple_list_item_2*/, parent, false);

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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

