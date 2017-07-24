package de.tudarmstadt.informatik.tk.silentmusicparty;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.tudarmstadt.informatik.tk.silentmusicparty.activities.PartyActivity;
import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Song;

/**
 * Custom adapter to set and fill the songlist appropriately.
 * Retrieved from webpage: http://stackoverflow.com/questions/17525886/listview-with-add-and-delete-buttons-in-each-row-in-android
 *
 * Created by Arbeit on 07.02.2017.
 */
public class SonglistAdapter extends BaseAdapter implements ListAdapter {
    private String danceToast = "You must dance";
    private ArrayList<Song> list = new ArrayList<Song>();
    private LinkedHashMap<Song,Boolean> songVoted;
    private Context context;
    private boolean isDancing;
    private PartyActivity pa;


    /**
     * Constructor
     * @param list
     * @param context
     */
    public SonglistAdapter(ArrayList<Song> list, Context context) {
        this.list = list;
        this.songVoted = new LinkedHashMap<Song,Boolean>();
        for(Song s : list)
            this.songVoted.put(s, false);
        this.context = context;
        this.pa = (PartyActivity) context;
        this.isDancing = true;
    }

    @Override
    public int getCount() {
        return list.size();
    }


    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }


    public void setList(ArrayList<Song> list, boolean showToast){
        this.list = list;
        if(this.songVoted == null)
            this.songVoted= new LinkedHashMap<Song,Boolean>();
        for(Song s : list)
            if(!this.songVoted.containsKey(s)) {
                if(showToast) Toast.makeText(context, "The song \"" + s.getTitle() + "\" has been added", Toast.LENGTH_LONG).show();
                this.songVoted.put(s, false);
            }

    }

    public void addSong(Song song){
        Log.d("ADD SONG", "in adapter was called");
        song = new Song(99, "22222222", "2222rain Purist", "https://www.student.informatik.tu-darmstadt.de/~bm66duca/Brain_Purist_-_Curse_the_Day__Radio_Edit_.mp3");
        this.list.add(song);
        this.songVoted.put(song, false);
        notifyDataSetChanged();
    }

    public long getItemId(int pos) {
        return 0;//list.get(pos).getId();
        //just return 0 if your list items do not have an Id variable.
    }

    public void isDancing(boolean bool){
        Log.d("DANCING", "method was called with " + bool);
        this.isDancing = bool;
        //if(!bool)
        //    Toast.makeText(context, danceToast, Toast.LENGTH_LONG).show();
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.my_custom_list_layout, null);//my_custom_list_layout, null);
        }

        //Handle TextView and display string from your list
        // TODO think whether limiting text size for songtitle and artist can be done in xml
        setTexts(view, list.get(position), R.id.text1, R.id.text2);
        /*TextView text1 = (TextView)view.findViewById(R.id.text1);
        TextView text2 = (TextView)view.findViewById(R.id.text2);


        if(list.get(position).getTitle().length() > 17)
            text1.setText(list.get(position).getTitle().substring(0,17) + "...");
        else
            text1.setText(list.get(position).getTitle());

        if(list.get(position).getArtist().length() > 23)
            text2.setText(list.get(position).getArtist().substring(0,23) + "...");
        else
            text2.setText(list.get(position).getArtist());*/

        TextView numberOfVotes = (TextView)view.findViewById(R.id.number_votes);
        numberOfVotes.setText("" + list.get(position).getVotes());


        //Handle buttons and add onClickListeners
        FloatingActionButton upBtn = (FloatingActionButton)view.findViewById(R.id.voteUp_btn);
        FloatingActionButton downBtn = (FloatingActionButton)view.findViewById(R.id.voteDown_btn);

        final Song song = list.get(position);
        boolean isVoted = true;
        if(song != null) isVoted = (boolean) songVoted.get(song);

        if(isDancing && !isVoted){
            //Log.d("ADAPTER", " is set clickable");
            upBtn.setAlpha(1.0f);
            upBtn.setClickable(true);
            downBtn.setAlpha(1.0f);
            downBtn.setClickable(true);
            upBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Log.d("LISTENER", "isDacning " + isDancing);
                    // TODO send an downvote somewhere
                    //song.downvote();
                    pa.getNetworkService().sendVote(song, -1);

                    songVoted.put(song,true);

                    //notifyDataSetChanged();
                }
            });
            downBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Log.d("LISTENER", "isDacning " + isDancing);
                    // TODO send an downvote somewhere
                    //song.upvote();
                    pa.getNetworkService().sendVote(song, 1);

                    songVoted.put(song,true);

                    /*for(int i = 0; i < list.size(); i++)
                        Log.d("ITEM", i + " " + list.get(i).getTitle() + " = " + list.get(i).getVotes());
                    notifyDataSetChanged();*/
                }
            });

        }else{
            //Log.d("ADAPTER", " is NOT set clickable");
            upBtn.setAlpha(0.2f);
            upBtn.setClickable(false);
            downBtn.setAlpha(0.2f);
            downBtn.setClickable(false);
            upBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(!isDancing)
                        Toast.makeText(context, danceToast, Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(context, "Only one vote per song", Toast.LENGTH_LONG).show();
                }
            });
            downBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(!isDancing)
                        Toast.makeText(context, danceToast, Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(context, "Only one vote per song", Toast.LENGTH_LONG).show();
                }
            });
        }


        //upBtn.setEnabled(false);


        return view;
    }

    public void update() {
        notifyDataSetChanged();
    }


    public void resetVotes() {
        for(Song song : songVoted.keySet()){
            songVoted.put(song, false);
        }
        notifyDataSetChanged();
    }

    public static void setTexts(View view, Song song, int RID1, int RID2){
        TextView text1 = (TextView) view.findViewById(RID1);
        TextView text2 = (TextView) view.findViewById(RID2);

        text1.setText(song.getTitle());
        text2.setText(song.getArtist());
    }

    public boolean getIsDancing(){
        return isDancing;
    }
}
