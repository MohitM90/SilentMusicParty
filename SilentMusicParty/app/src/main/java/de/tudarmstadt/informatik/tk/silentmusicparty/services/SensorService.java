package de.tudarmstadt.informatik.tk.silentmusicparty.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * The class provides a service that handles the changes in the accelerometer sensor.
 *
 * Created by chrisbe on 07.02.2017.
 */
public class SensorService extends Service implements SensorEventListener {
    public static final String DANCING_UPDATE = "com.hello.action";

    IBinder mBinder = new LocalBinder();
    public boolean isDancing = true;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean isActive;
    private Context context;

    /**
     * Fields for is-dancing decision
     */
    private float[] oldVals = new float[3];
    private CountDownTimer countDown;
    private boolean startCounter;
    private long seconds;
    private int totalEvents;
    private int overThreshold;
    private double threshold;


    public int onStartCommand(Intent intent, int flags, int startId) {

        // make context available
        this.context = this;

        // set up sensor manager and sensor
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        isActive = false;

        // initialize the container for memorizing sensor values of the last time point
        oldVals = new float[3];

        // set up the variables for evaluating dancing
        seconds = System.currentTimeMillis();
        totalEvents = 0;
        overThreshold = 0;
        threshold = 1.0;

        // make sure that dancing is evaluated right on start
        startCounter = false;

        // start sensor tracking
        start();

        return startId;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // sum of absolute differences between (SAD) the current and the values from last time step
        double delta = Math.abs(event.values[0]-oldVals[0])+
                Math.abs(event.values[1]-oldVals[1])+
                Math.abs(event.values[1]-oldVals[1]);

        // increase the number of events
        totalEvents++;

        // if the threshold is exceeded
        if(delta > threshold)
            // increase the number of events over threshold
            overThreshold++;


        // if there is truely no movement, update time
        if((double) overThreshold / totalEvents == 0)
            seconds = System.currentTimeMillis();

        // after 5 seconds of listening
        if(System.currentTimeMillis() - seconds > 5000 || !startCounter){
            // if more than 40% of values exceeded threshold
            if((double) overThreshold / totalEvents > 0.4 || !startCounter) {
                startCounter = true;

                // set dancing true
                isDancing = true;

                // broadcast intent to PartyActivity
                Intent local = new Intent();
                local.setAction(DANCING_UPDATE);
                local.putExtra("dancing", new Boolean(true));
                this.sendBroadcast(local);

                // in case there already is a count down -> cancel it
                if(countDown != null)
                    countDown.cancel();
                // set count down to 30 sec
                countDown = new CountDownTimer(30000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        // broadcast intent to PartyActivity
                        Intent local = new Intent();
                        local.setAction(DANCING_UPDATE);
                        local.putExtra("dancing", new Boolean(true));
                        local.putExtra("timeleft", millisUntilFinished);
                        context.sendBroadcast(local);
                    }

                    public void onFinish() {
                        // set dancing true
                        isDancing = false;

                        // broadcast intent to PartyActivity
                        Intent local = new Intent();
                        local.setAction(DANCING_UPDATE);
                        local.putExtra("dancing", new Boolean(false));
                        context.sendBroadcast(local);
                    }
                }.start();
            }
            // reset time and counts
            seconds = System.currentTimeMillis();
            totalEvents = 0;
            overThreshold = 0;
        }

        // memorize current values for next check
        oldVals[0] = event.values[0];
        oldVals[1] = event.values[1];
        oldVals[2] = event.values[2];
    }


    //@Override
    public void start() {
        mSensorManager.registerListener(this,mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isActive=true;
    }

    //@Override
    public void stop() {
        if(!isActive) return;
        mSensorManager.unregisterListener(this);
        isActive=false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SensorService getServerInstance() {
            return SensorService.this;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        stop();
    }

    public boolean onError(SensorService mp, int what, int extra) {
        // The MediaPlayer has moved to the Error state, must be reset!
        Log.d("SERVICE", "something went wrong with Sensor tracking");
        return false;
    }
}
