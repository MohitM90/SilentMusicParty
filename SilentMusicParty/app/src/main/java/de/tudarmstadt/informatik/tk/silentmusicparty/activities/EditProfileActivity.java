package de.tudarmstadt.informatik.tk.silentmusicparty.activities;

import android.content.Context;
import android.content.Intent;
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
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.tudarmstadt.informatik.tk.silentmusicparty.R;

public class EditProfileActivity extends AppCompatActivity {

    private static Intent data;
    private ImageView profilePic;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getFragmentManager().beginTransaction().add(R.id.profile_items, new EditProfileFragment()).commit();
        data = new Intent();

        profilePic = (ImageView) findViewById(R.id.profilePic);

        //load profile picture, if there is one
        try {
            FileInputStream fis = new FileInputStream(getFileStreamPath("profilepic.jpg"));
            Bitmap bmp = BitmapFactory.decodeStream(fis);
            profilePic.setImageBitmap(cirlceBitmap(bmp));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_OK, this.data);
        finish();
    }

    public static class EditProfileFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_profile);
            bindPreferenceSummaryToValue(findPreference("username"));
            bindPreferenceSummaryToValue(findPreference("birthday"));
            bindPreferenceSummaryToValue(findPreference("height"));
            bindPreferenceSummaryToValue(findPreference("gender"));
            bindPreferenceSummaryToValue(findPreference("location"));
            bindPreferenceSummaryToValue(findPreference("message"));
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                if (preference.getKey().equals("height")) {
                    stringValue += " cm";
                }
                preference.setSummary(stringValue);

            }
            data.putExtra(preference.getKey(), stringValue);
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    //opens gallery to select a profile picture
    public void selectImage(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();

                    //save the profile picture with reduced resolution
                    Bitmap bmp = null;
                    try {
                        bmp = shrink(BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage)));
                        FileOutputStream outputStream = openFileOutput("profilepic.jpg", Context.MODE_PRIVATE);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        profilePic.setImageBitmap(cirlceBitmap(bmp));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    /**
     * Shrinks a Bitmap to max 2000x2000 resolution
     * @param bmp the Bitmap to shrink
     * @return the shrinked Bitmap (or bmp if neither height nor width are >2000 px)
     */
    private Bitmap shrink(Bitmap bmp) {
        final int MAX_WIDTH = 2000;
        final int MAX_HEIGHT = 2000;
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        if (w > MAX_WIDTH || h > MAX_HEIGHT) {
            double wf = w/(double)MAX_WIDTH;
            double hf = h/(double)MAX_HEIGHT;
            if (wf > hf) hf = wf;
            else wf = hf;
            w = (int)(w/wf);
            h = (int)(h/hf);
            return Bitmap.createScaledBitmap(bmp, w, h, true);
        } else {
            return bmp;
        }
    }

    /**
     * Makes the given Bitmap circular
     * @param pic original Bitmap
     * @return original Bitmap in a circular shape
     */
    private Bitmap cirlceBitmap(Bitmap pic) {
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
}