package de.tudarmstadt.informatik.tk.silentmusicparty;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

/**
 * Class manages the date preference.
 *
 * Created by Mohit on 07.02.2017.
 */

public class DatePreference extends DialogPreference {

    // initialze date picker
    private DatePicker picker = null;

    /**
     * Constructor
     * @param context
     * @param attrs
     */
    public DatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView() {
        picker = new DatePicker(getContext());
        String dateval = getPersistedString("");
        // in case the date value in not empty -> update value
        if (!dateval.equals("")) {
            String[] date = dateval.split("\\.");
            picker.updateDate(Integer.parseInt(date[2]), Integer.parseInt(date[1])-1, Integer.parseInt(date[0]));
        }
        return picker;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        StringBuilder sb = new StringBuilder();
        // convert single digit into two digits
        if (picker.getDayOfMonth() < 10) {
            sb.append(0);
        }
        sb.append(picker.getDayOfMonth());
        sb.append(".");
        // convert single digit into two digits
        if (picker.getMonth() < 9) {
            sb.append(0);
        }
        sb.append((picker.getMonth()+1));
        sb.append(".");
        sb.append(picker.getYear());

        // set listener and broadcast change
        String dateval = sb.toString();
        callChangeListener(dateval);
        persistString(dateval);
        notifyChanged();
    }
}
