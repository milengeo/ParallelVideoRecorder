package com.rustero.mains;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.tools.Caminf;
import com.rustero.tools.Size2;
import com.rustero.tools.Tools;
import com.rustero.units.MultiEncor;

import java.util.List;


public class RecordActivity extends Activity {

    private static String TAG = "RecordActivity";
    private Activity mActivity;
    private Handler mHandler;

    private ImageButton mStartButton, mStopButton;
    private TextView mZoomInfo, mTimeInfo;
    private ListView mListView;

    private MultiEncor mEncor;
    private SurfaceView mSurview;
    private FlowAdapter mAdapter;
    private List<MultiEncor.FlowC> mFlowList;
    private int mMaxMinutes;
    private boolean mScaling;
    ScaleGestureDetector mScaleDetector;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.record_activity);

            mActivity = this;
            Log.d(TAG, "1111111111");

            mListView = (ListView) findViewById(R.id.record_livi_flows);
            mStartButton = (ImageButton) findViewById(R.id.record_pubu_start);
            mStartButton.setOnClickListener(new StartClicker());

            mStopButton = (ImageButton) findViewById(R.id.record_pubu_stop);
            mStopButton.setOnClickListener(new StopClicker());

            mZoomInfo = (TextView) findViewById(R.id.record_tevi_zoom_info);
            mTimeInfo = (TextView) findViewById(R.id.record_tevi_time_info);

            mSurview = (SurfaceView) findViewById(R.id.id_port_view);
            mEncor = new MultiEncor(new EncorEventer(), mSurview);

            mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
///                onclickWork(null);
                }
            }, 999);

            findAskedSize();

            Log.i(TAG, "1111111111");
        } catch (Exception ex) {
            App.logLine(" ***** RecordActivity_onCreate: " + ex.getMessage());
        };
    }




    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        ///Log.d(TAG, "dispatchTouchEvent");
        mScaleDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }






    public class ScaleListener extends  ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float mFactor = 1.0f;



//        private class ZoomInClicker implements View.OnClickListener {
//            public void onClick(View v) {
//                mEncor.incZoom();
//            }
//        }
//        private class ZoomOutClicker implements View.OnClickListener {
//            public void onClick(View v) {
//                mEncor.decZoom();
//            }
//        }



        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            mFactor *= detector.getScaleFactor();
//            mFactor = ((float)((int)(mFactor * 100))) / 100; // Change precision to help with jitter when user just rests their fingers
//            if (mFactor < 1.0f)
//                mFactor = 1.0f;
//            if (mFactor > 2.0f)
//                mFactor = 2.0f;
//            Log.d(TAG, "onScale: " + mFactor);
            float prev = detector.getPreviousSpan();
            float curr = detector.getCurrentSpan();
            if (curr > prev) {
                //Log.d(TAG, "onScale inc");
                mEncor.incZoom();
            } else if (curr < prev) {
                //Log.d(TAG, "onScale dec");
                mEncor.decZoom();
            }
            String zoin = mEncor.getZoom() + "x";
            mZoomInfo.setText(zoin);
            //Log.d(TAG, "onScale: " + mEncor.getZoom());
            return true;
        }


        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin");
            return true;
        }


        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //Log.d(TAG, "onScaleEnd: " + mEncor.getZoom());
        }

    }
















    private void findAskedSize() {

        int maxHe = 480;
        List<ProfileC> profList = App.getProfList();
        for (int i=0; i<profList.size(); i++) {
            ProfileC prof = profList.get(i);
            if (!prof.chosen) continue;
            if (prof.height > maxHe)
                maxHe = prof.height;
        }

        Caminf caminf = App.getCaminf();
        Size2 size = caminf.resolutions.getAboveHeight(maxHe);
        mEncor.askWidth = size.width;
        mEncor.askHeight = size.height;
    }



    @Override
    protected void onStart() {
        super.onStart();
    }



    @Override
    protected void onResume() {
        super.onResume();
    }



    @Override
    protected void onPause() {
        super.onPause();
        doStop();
    }



    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }




    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        //Handle the back button
//        new AlertDialog.Builder(this)
//            .setTitle("Closing Activity")
//            .setMessage("Are you sure you want to stop the recording?")
//            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//
//            })
//            .setNegativeButton("No", null)
//            .show();
    }




    private class StartClicker implements View.OnClickListener {
        public void onClick(View v) {
            doStart();
        }
    }



    private class StopClicker implements View.OnClickListener {
        public void onClick(View v) {
            doStop();
        }
    }




    private void updateUI() {
        if (mEncor.isRecording()) {
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.VISIBLE);
        }   else {
            mStartButton.setVisibility(View.VISIBLE);
            mStopButton.setVisibility(View.GONE);
        }
        updateFlowList();
    }




    private void doStart() {
        String folder = App.getOutputFolder();
        if (!Tools.folderExists(folder)) {
            App.showAlert(this, "Something is wrong!", "The output directory does not exist!");
            return;
        }

        mMaxMinutes = App.getMaxDuration();
        mFlowList = mEncor.makeFlowList();
        mEncor.startRecording();
        updateUI();
    }



    private void doStop() {
        mEncor.stopRecording();
        updateUI();
    }



    private void updateFlowList() {
        if (null == mFlowList) return;
        mAdapter = new FlowAdapter(this, R.layout.record_flow_row, mFlowList);
        mListView.setAdapter(mAdapter);
    }







    public class FlowAdapter extends ArrayAdapter<MultiEncor.FlowC> {

        private Context mContext;
        private int mLayout;
        List<MultiEncor.FlowC> mItems;

        public FlowAdapter(Context context, int resource, List<MultiEncor.FlowC> objects) {
            super(context, resource, objects);
            this.mContext = context;
            this.mLayout = resource;
            this.mItems = objects;
        }


        public MultiEncor.FlowC getItem(int i)
        {
            return mItems.get(i);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(mLayout, null);
            }
            // create a new view of my layout and inflate it in the row
            final MultiEncor.FlowC flow = mItems.get(position);

            TextView tvName = (TextView) rowView.findViewById(R.id.record_row_name);
            tvName.setText(flow.name);

            TextView tvSize = (TextView) rowView.findViewById(R.id.record_row_size);
            tvSize.setText(Tools.formatFileSize(flow.size));

            TextView tvRate = (TextView) rowView.findViewById(R.id.record_row_rate);
            tvRate.setText(" - " + Tools.formatBitrate(flow.rate));

            return rowView;
        }
    }

















    public class EncorEventer implements MultiEncor.Events {


        public EncorEventer() {
            super();
        }


        @Override
        public void onLogLine(final String aLine) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mEncoInfo.setText(aLine);
                }
            });
        }


        public void onAttached() {
            App.logLine("EncorEventer_onAttached");
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (mEncor.hasZoom())
//                        mZoomControls.setVisibility(View.VISIBLE);
//                }
//            });
        }


        public void onDetached() {
            App.logLine("EncorEventer_onDetached");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    MainActivity.this.mScene.clearSurface();
                }
            });
        }



        public void onStarted() {
            App.logLine("EncorEventer_onStarted");

        }


        public void onStopped() {
            App.logLine("EncorEventer_onStopped");
        }



        public void onFlowInfo(int aIndex, final long aSize, final int aRate) {
            if (aIndex >= mFlowList.size()) return;
            MultiEncor.FlowC flow = mFlowList.get(aIndex);
            flow.size = aSize;
            flow.rate = aRate;
        }



        public void onTimeInfo(final long aSofar) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String text = String.format("%02d:%02d:%02d", aSofar / 3600, (aSofar % 3600) / 60, (aSofar % 60));
                    mTimeInfo.setText(text);
                    updateFlowList();

                    if (mEncor.isRecording())
                        if (aSofar > mMaxMinutes*60)
                            doStop();
                }
            });
        }



        public void onError(final String aMessage)
        {
            App.logLine("EncorEventer_onError");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new android.app.AlertDialog.Builder(mActivity).setTitle("Playback error").setMessage(aMessage)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with click
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                }
            });
        }


    }


}
