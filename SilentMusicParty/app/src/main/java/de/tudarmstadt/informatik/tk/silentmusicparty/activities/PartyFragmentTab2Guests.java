package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Party;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;

/**
 * Created by Mohit on 02.03.2017.
 */

public class PartyFragmentTab2Guests extends Fragment {

    private GuestListAdapter adapter;

    private RecyclerView mRecyclerView;

    public PartyFragmentTab2Guests() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_party_tab2_guests, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.guests_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getBaseContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), LinearLayout.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        PartyActivity pa = (PartyActivity)getActivity();
        if (pa.getNetworkService() != null) showGuestList(pa.getNetworkService().getParty());
    }

    public void showGuestList(Party p) {
        adapter = new GuestListAdapter(p);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.invalidate();
    }


    public static class GuestListAdapter extends RecyclerView.Adapter<GuestListAdapter.ViewHolder> {

        private Party party;
        ArrayList<Person> person;
        ArrayList<byte[]> image;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // each data item is just a string in this case
            public PopupMenu popup;

            public Person p;
            public ImageView mImageViewPic;
            public TextView mTextViewName;


            public ViewHolder(View v) {
                super(v);
                mImageViewPic = (ImageView) v.findViewById(R.id.profilePic);
                mTextViewName = (TextView) v.findViewById(R.id.text1/*android.R.id.text1*/);

                mTextViewName.setTypeface(null, Typeface.BOLD);

                v.setOnClickListener(this);
            }

            @Override
            public void onClick(final View v) {
                popup.show();
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public GuestListAdapter(Party party) {
            this.party = party;
            HashMap<Person, byte[]> guests = party.getMemberList();
            Set<Person> persons = guests.keySet();
            int size = persons.size();
            person = new ArrayList<>(size);
            image = new ArrayList<>(size);
            for (Person p : persons) {
                person.add(p);
                image.add(guests.get(p));
            }
        }

        public void addItem(Person p, byte[] pic) {
            person.add(p);
            image.add(pic);
        }
        // Create new views (invoked by the layout manager)
        @Override
        public GuestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.guests_list_item, parent, false);

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(holder.itemView.getContext());
            String uuid = prefs.getString("UserUUID", null);

            holder.p = person.get(position);
            holder.mTextViewName.setText(person.get(position).getName());
            holder.mImageViewPic.setImageBitmap(loadBitmap(image.get(position)));

            holder.popup = new PopupMenu(holder.itemView.getContext(), holder.itemView);
            holder.popup.getMenuInflater().inflate(R.menu.menu_guestlist,
                    holder.popup.getMenu());

            //Can kick only if Host. Can't kick yourself
            if (!uuid.equals(party.getHost().getUid()) || uuid.equals(holder.p.getUid())) {
                holder.popup.getMenu().removeItem(R.id.kick);
            }
            holder.popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.viewProfile:
                            int i = person.indexOf(holder.p);
                            byte[] b = image.get(i);
                            ViewProfileActivity.setGuest(holder.p, b);
                            Intent intent = new Intent(holder.itemView.getContext(), ViewProfileActivity.class);
                            intent.putExtra("Guest", true);
                            holder.itemView.getContext().startActivity(intent);
                            break;
                        case R.id.kick:
                            String uid = holder.p.getUid();
                            PartyActivity pa = (PartyActivity)holder.itemView.getContext();
                            pa.kick(uid);
                            break;

                    }

                    return true;
                }
            });
        }

        /**
         * loads a profile picture and shows it as a circle
         * @param image picture to load
         * @return Bitmap showing the loaded picture as a circle.
         */
        private Bitmap loadBitmap(byte[] image) {

            Bitmap pic = BitmapFactory.decodeByteArray(image, 0, image.length);
            if (pic == null) return null;
            Bitmap circleBitmap;
            Rect r;
            if (pic.getHeight() > pic.getWidth()) {
                circleBitmap = Bitmap.createBitmap(pic.getWidth(), pic.getWidth(), Bitmap.Config.ARGB_8888);
                r = new Rect(0, (pic.getHeight()/2)-(pic.getWidth()/2), pic.getWidth(), (pic.getHeight()/2)+(pic.getWidth()/2));
            } else {
                circleBitmap = Bitmap.createBitmap(pic.getHeight(), pic.getHeight(), Bitmap.Config.ARGB_8888);
                r = new Rect((pic.getWidth()/2)-(pic.getHeight()/2), 0, (pic.getWidth()/2)+(pic.getHeight()/2), pic.getHeight());
            }

            Paint paint = new Paint();

            paint.setColor(Color.argb(0xFF, 0xFF, 0x40, 0x81));
            paint.setStrokeWidth(5);
            Canvas c = new Canvas(circleBitmap);
            BitmapShader shader = new BitmapShader(pic, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            paint.setAntiAlias(true);

            c.drawCircle(circleBitmap.getWidth()/2, circleBitmap.getHeight()/2, circleBitmap.getWidth()/2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            c.drawBitmap(pic, r, new Rect(0, 0, circleBitmap.getWidth(), circleBitmap.getHeight()),paint);
            pic.recycle();
            return circleBitmap;
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return person.size();
        }
    }
}
