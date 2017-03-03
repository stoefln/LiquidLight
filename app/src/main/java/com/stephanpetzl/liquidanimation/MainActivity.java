package com.stephanpetzl.liquidanimation;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.stephanpetzl.liquidanimation.util.Tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int TRACK_NUM = 8;


    private Handler handler = new Handler();

    private boolean mRunSequencer = false;

    private Vector<SeekBar> mTimingOffsetSeekbars;
    private Vector<SeekBar> mDurationSeekbars;
    private Vector<TrackSettings> mTrackSettings;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRunSequencer) {
                mGridView.next();
                // send data packets: "p12t45d100" (pin 12, timing offset 45, duration 100)
                boolean[] column = mGridView.getCurrentColumn();
                for (int i = 0; i < column.length; i++) {
                    if (column[i]) { // if active
                        TrackSettings ts = mTrackSettings.get(i);
                        String cmd = "p" + ts.arduinoPin + "t" + (int) ts.timingOffsetMillis + "d" + (int) ts.durationMillis + ";";
                        mBluetooth.send(cmd);
                    }
                }
                handler.postDelayed(mRunnable, 500);
            }
        }
    };
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BluetoothConnector mBluetooth;
    private LinearLayout mSeekbarContainer1;
    private LinearLayout mSeekbarContainer2;
    private SeekBar.OnSeekBarChangeListener mTimingOffsetChanged;
    private SeekBar.OnSeekBarChangeListener mDurationChanged;
    private View mSeekbarContainer;
    private ToggleButton mToggleView;


    public void stopSequencer() {
        mRunSequencer = false;
        handler.removeCallbacks(mRunnable);
    }

    public void startSequencer() {
        mRunSequencer = true;
        handler.post(mRunnable);
    }


    @Override
    protected void onStop() {
        stopSequencer();
        mBluetooth.close();
        Tools.putPreference(this, Static.TRACK_SETTINGS, mTrackSettings);
        super.onStop();
    }

    private DrawableGridView mGridView;
    private ToggleButton mTogglePlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView = (DrawableGridView) findViewById(R.id.drawable_grid_view);
        mTogglePlay = (ToggleButton) findViewById(R.id.toggle_play);
        mToggleView = (ToggleButton) findViewById(R.id.toggle_view);
        mSeekbarContainer1 = (LinearLayout) findViewById(R.id.seekbar_container1);
        mSeekbarContainer2 = (LinearLayout) findViewById(R.id.seekbar_container2);
        mSeekbarContainer = findViewById(R.id.timing_seekbar_container);


        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mToggleView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mSeekbarContainer.setVisibility(View.VISIBLE);
                }else{
                    mSeekbarContainer.setVisibility(View.GONE);
                }
            }
        });
        mToggleView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                for (SeekBar seekBar : mDurationSeekbars) {
                    seekBar.setProgress(mDurationSeekbars.get(0).getProgress());
                }
                for (SeekBar seekBar : mTimingOffsetSeekbars) {
                    seekBar.setProgress(mTimingOffsetSeekbars.get(0).getProgress());
                }
                return false;
            }
        });
        mTogglePlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startSequencer();
                } else {
                    stopSequencer();
                }
            }
        });

        mBluetooth = new BluetoothConnector(this);
        mBluetooth.init();

        initTrackSettings();
        createTimingOffsetSeekbars();
        createDurationSeekbars();

        initDrawableGrid();


    }

    private void initDrawableGrid() {
        mGridView.setOnDrawableGridChangedListener(new DrawableGridView.OnDrawableGridChanged() {
            @Override
            public void onDrawableGridChanged(int trackNum, int col, boolean value) {
                TrackSettings trackSettings = mTrackSettings.get(trackNum);
                char[] pattern = trackSettings.pattern.toCharArray();
                pattern[col] = value ? 'x' : '_';
                trackSettings.pattern = new String(pattern);
            }
        });
        int trackCount = mTrackSettings.size();
        int patternLength = mTrackSettings.get(0).pattern.length();
        boolean[][] pattern = new boolean[patternLength][trackCount];
        for (int r = 0; r < trackCount; r++) {
            for (int c = 0; c < patternLength; c++) {
                pattern[c][r] = mTrackSettings.get(r).pattern.charAt(c) == 'x';
            }
        }
        mGridView.setPattern(pattern);
    }

    private void initTrackSettings() {
        mTrackSettings = (Vector<TrackSettings>) Tools.getPreferenceSerializable(this, Static.TRACK_SETTINGS);
        if(mTrackSettings == null) {
            mTrackSettings = new Vector<>();
            for (int i = 0; i < TRACK_NUM; i++) {
                TrackSettings ts = new TrackSettings(i, TrackSettings.ARDUINO_PINS[i]);
                mTrackSettings.add(ts);
            }
        }
    }

    private void createTimingOffsetSeekbars() {

        mTimingOffsetChanged = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int val, boolean b) {
                int seekbarIndex = mTimingOffsetSeekbars.indexOf(seekBar);
                mTrackSettings.get(seekbarIndex).timingOffsetMillis = (int) ((float) TrackSettings.MAX_TIMING_OFFSET * val / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        mTimingOffsetSeekbars = new Vector<>();
        for (int i = 0; i < TRACK_NUM; i++) {
            SeekBar sb = new SeekBar(this);
            sb.setProgress((int) (100 * mTrackSettings.get(i).timingOffsetMillis / TrackSettings.MAX_TIMING_OFFSET));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            params.weight = 1;
            sb.setOnSeekBarChangeListener(mTimingOffsetChanged);
            mSeekbarContainer1.addView(sb, params);
            mTimingOffsetSeekbars.add(sb);
        }
    }

    private void createDurationSeekbars() {

        mDurationChanged = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int val, boolean b) {
                int seekbarIndex = mDurationSeekbars.indexOf(seekBar);
                mTrackSettings.get(seekbarIndex).durationMillis = (int) (TrackSettings.MAX_DURATION * val / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        mDurationSeekbars = new Vector<>();
        for (int i = 0; i < TRACK_NUM; i++) {
            SeekBar sb = new SeekBar(this);
            sb.setProgress((int) (100 * mTrackSettings.get(i).durationMillis / TrackSettings.MAX_DURATION));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            params.weight = 1;
            sb.setOnSeekBarChangeListener(mDurationChanged);
            mSeekbarContainer2.addView(sb, params);
            mDurationSeekbars.add(sb);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
