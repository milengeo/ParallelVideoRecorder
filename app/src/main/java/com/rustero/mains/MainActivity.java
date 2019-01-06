
package com.rustero.mains;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.rustero.App;
import com.rustero.tools.Caminf;
import com.rustero.tools.Size2;
import com.rustero.tools.Tools;
import com.rustero.tools.Lmx;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


@SuppressWarnings("deprecation")






public class MainActivity extends AppCompatActivity {

    private static final int TACK_PERIOD = 999;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    private Activity mActivity;
    private Toolbar mToolbar;
    private ActionBar mActBar;
    private Handler mTimerHandler = null;
    private boolean mHaveTacked;

    private MenuItem mOutputMeit, mAddMeit, mEditMeit, mDeleteMeit;

    private TextView mStatusView;
    private ListView mListView;
    private ProfsAdapter mAdapter;

    private Button btnRecord, btnOutput;
    private RadioButton mFrontButton, mBackButton;
    private int mChoosen;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        onAttach();
        App.logLine("onCreate");

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        //mToolbar.setLogo(R.drawable.ic_movie24);

        mActBar = getSupportActionBar();
        mActBar.setDisplayShowTitleEnabled(false); // Hide default toolbar title

        App.gCurContext = this;

        mStatusView = (TextView) findViewById(R.id.main_tevi_status);
        mListView = (ListView) findViewById(R.id.main_livi_profs);

        mListView.setOnItemClickListener(new ListViewItemClickListener());
        mListView.setOnItemLongClickListener(new ListViewItemLongClickListener());

        btnRecord = (Button) findViewById(R.id.main_pubu_record);
        btnRecord.setOnClickListener(new RecordClicker());

        mFrontButton = (RadioButton) findViewById(R.id.main_rabu_front);
        mFrontButton.setOnClickListener(new FrontClicker());

        mBackButton = (RadioButton) findViewById(R.id.main_rabu_back);
        mBackButton.setOnClickListener(new BackClicker());

        loadCameras();
        loadProfList();
        updateProfList();

        mTimerHandler = new Handler();
        mTimerHandler.postDelayed(timerRunnable, 99);
        mAdView = (AdView) findViewById(R.id.main_adView);

//        if (App.wantBannerAd()) {
//            adView.setVisibility(View.VISIBLE);
//            AdRequest.Builder adBuilder = new AdRequest.Builder();
//            ///adBuilder.addTestDevice("C4D6093E90313D96792DB26EF5248F41");
//            AdRequest adRequest = adBuilder.build();
//            adView.loadAd(adRequest);
//        } else {
//            //App.showLongToast("no ads");
//        }


        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-2719230232522738/5126348003");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                launchRecording();
            }
        });
        requestNewInterstitial();

        //Lmx.selfTest1();
    };



    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
            .addTestDevice("C4D6093E90313D96792DB26EF5248F41")
            .build();

        mInterstitialAd.loadAd(adRequest);
    }



    void askBannerAd() {
        if (!App.wantBannerAd()) return;
        mAdView.setVisibility(View.VISIBLE);
        Thread adThread = new Thread() {
            @Override
            public void run() {
                final AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("C4D6093E90313D96792DB26EF5248F41")
                    .build();

                // Load Ads on UI Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdView.loadAd(adRequest);
                        updateUI();
                    }
                });
            }
        };
        adThread.start();
    }


    private void test() {
        //long installDays = App.getInstallDays();
        //App.showLongToast("ispro: " + App.gIsPro);
        //App.showLongToast("gRunCount: " + App.gRunCount + "  days: " + installDays  + "  for: " + App.getAppSecs());

    }



    public void onAttach() {
        if (App.live) return; // already created
        App.live = true;
        App.logLine("onAttach");
        App.gBitrateList = Arrays.asList(getResources().getStringArray(R.array.new_bitrate_list));
    }



    private void onShowActivity() {
        loadMainState();

        if ( (null==App.gFrontCam) ) {
            mFrontButton.setEnabled(false);
            switchBack();
        }

        if ( (null==App.gBackCam) ) {
            mBackButton.setEnabled(false);
            switchFront();
        }

        askBannerAd();
    }




    private void loadMainState() {
        String code = Tools.readPrivateFile(this, App.MAIN_STATE_XML);
        if (code.isEmpty()) return;

        Lmx xml = new Lmx(code);
        xml.pullNode("last_state");
        App.gNowFront = xml.getBln("now_front");

        if (App.gNowFront)
            switchFront();
        else
            switchBack();
    }



    private void saveMainState() {
        Lmx xml = new Lmx();
        xml.addBln("now_front", App.gNowFront);
        xml.pushNode("last_state");
        String code = xml.getCode();
        Tools.writePrivateFile(this, App.MAIN_STATE_XML, code);
    }



    private void switchFront() {
        mFrontButton.setChecked(true);
        mBackButton.setChecked(false);
        App.gNowFront = true;
        updateUI();
    }



    private void switchBack() {
        mFrontButton.setChecked(false);
        mBackButton.setChecked(true);
        App.gNowFront = false;
        updateUI();
    }



    private class ListViewItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ProfileC prof = (ProfileC) mAdapter.getItem(position);
            if (App.gCurProf != prof)
                App.gCurProf = prof;
            else
                App.gCurProf = null;
            mAdapter.notifyDataSetChanged();
            updateMenu();
            updateStatus();
        }
    }



    private class ListViewItemLongClickListener implements AdapterView.OnItemLongClickListener {
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            return true;
        }
    }



    private class RecordClicker implements View.OnClickListener {
        public void onClick(View v) {
            if (mChoosen == 0) {
                App.showAlert(MainActivity.this, App.getAppName(), "Please select at least one profile!");
                return;
            }

            if (mInterstitialAd.isLoaded() && App.wantInterstitialAd()) {
                mInterstitialAd.show();
            } else {
                launchRecording();
            }
        }
    }


    private void launchRecording() {
        Intent intent = new Intent(mActivity, RecordActivity.class);
        startActivity(intent);
        App.makeAnim(MainActivity.this);
    }





    private class FrontClicker implements View.OnClickListener {
        public void onClick(View v) {
            switchFront();
        }
    }



    private class BackClicker implements View.OnClickListener {
        public void onClick(View v) {
            switchBack();
        }
    }







    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mHaveTacked) {
                // first tack
                mHaveTacked = true;
                firstTack();
            }

            tackWork();

            mTimerHandler.postDelayed(this, TACK_PERIOD);
        }
    };



    private void tackWork() {
    }


    private void firstTack() {
        App.logLine("firstTack");
    }



    public void shutdown() {
        finish();
    }



    @Override
    protected void onStart() {
        super.onStart();
        App.logLine("onStart");
    }


    @Override
    protected void onResume() {
        super.onResume();
        App.logLine("onResume");
    }


    @Override
    protected void onPause() {
        super.onPause();
        App.logLine("onPause");
        saveMainState();
    }



    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mTimerHandler.removeCallbacksAndMessages(null);
        App.logLine("onDestroy");
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        App.logLine("onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        mOutputMeit = menu.findItem(R.id.main_meit_output);
        mAddMeit = menu.findItem(R.id.main_meit_add_prof);
        mEditMeit = menu.findItem(R.id.main_meit_edit_prof);
        mDeleteMeit = menu.findItem(R.id.menu_meit_delete_prof);

        updateMenu();

        onShowActivity();
        return true;
    }



    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        Log.i(LOG_TAG, "onPrepareOptionsMenu");
//        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
//        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);
        return super.onPrepareOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        App.logLine("onOptionsItemSelected");
        switch(item.getItemId()) {
            case R.id.main_meit_output:      showOutput(); return true;
            case R.id.main_meit_add_prof:    addProf(); return true;
            case R.id.main_meit_edit_prof:   editProf(); return true;
            case R.id.menu_meit_delete_prof: deleteProf(); return true;

            case R.id.main_meit_settings: showSettings(); return true;
            case R.id.main_meit_view_log: viewLog(); return true;
            case R.id.main_meit_help: viewHelp(); return true;
            case R.id.main_meit_about: viewAbout(); return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case App.ACT_RESULT_SETTINGS:  onSettingsResult(resultCode);  break;
            case App.ACT_RESULT_ADD_PROF:  onAddProfResult(resultCode);  break;
            case App.ACT_RESULT_EDIT_PROF: onEditProfResult(resultCode);  break;
        }
    }



    private boolean isExternalStoragePresent() {

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        if (!((mExternalStorageAvailable) && (mExternalStorageWriteable))) {
            Toast.makeText(this, "SD card not present", Toast.LENGTH_LONG)
                .show();

        }
        return (mExternalStorageAvailable) && (mExternalStorageWriteable);
    }



    private void showOutput() {
        Intent intent = new Intent(this, OutputActivity.class);
        startActivity(intent);
        App.makeAnim(MainActivity.this);
    }


    private void showSettings() {
//        File[] filesDirs  = getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
//        File[] mediaDirs  = getExternalMediaDirs();

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, App.ACT_RESULT_SETTINGS);
        App.makeAnim(MainActivity.this);
    }



    public void onSettingsResult(int aResultCode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.self);
        String path = preferences.getString("output_folder", "");
        if (Tools.isBlank(path)) return;
//        File[] mediaDirs  = getExternalMediaDirs();

        File folder = new File(path);
        folder.mkdirs();

        boolean badFolder = false;
        if (!Tools.folderExists(path)) {
            badFolder = true;
            App.showAlert(this, "Error creating folder!", "Output folder does not exists! \n\nThe default one will be used instead.");
        }

        File file = new File(path + "/test");
        try {
            file.createNewFile();
        }
        catch (Exception ex) {}
        if (!file.isFile()) {
            badFolder = true;
            App.showAlert(this, "No write access!", "Output folder is not writable ! \n\nThe default one will be used instead.");
        }
        else
            file.delete();

        if (badFolder) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("output_folder", App.getDefaultFolder());
            editor.commit();
        }

//        if (!App.gIsPro__) {
//            int maxdur = 5;
//            String text = preferences.getString("max_duration", "5");
//            if ((text != null) && (text.length() > 0)) {
//                maxdur = Tools.parseInt(text);
//            }
//
//            if (maxdur > 5) {
//                maxdur = 5;
//                SharedPreferences.Editor editor = preferences.edit();
//                editor.putString("max_duration", "5");
//                editor.commit();
//                App.showAlert(this, "Free version limit!", "Maximum duration is limit to 5 minutes in the free version");
//            }
//        }

    }



    private void viewLog() {
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
        App.makeAnim(MainActivity.this);
    }



    private void viewHelp() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.rustero.com/pavire.html#main"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }



    private void viewAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
        App.makeAnim(MainActivity.this);
    }





    private void updateUI() {
        updateProfList();
        updateStatus();
        updateMenu();
    }



    public void updateProfList() {
        List<ProfileC> profList = App.getProfList();
        mAdapter = new ProfsAdapter(this, R.layout.main_prof_row, profList);
        mListView.setAdapter(mAdapter);
    }



    private void updateStatus() {
        countChosen();
        String text = " " + App.gFrontProfs.size() + " profiles";
        text += ", " + mChoosen + " chosen";
        mStatusView.setText(text);
    }



    private void rowChecked(CheckBox aBox) {
        int pos = (int) aBox.getTag();
        ProfileC prof = mAdapter.getItem(pos);
        if (null == prof) return;
        prof.chosen = !prof.chosen;

        updateUI();
        saveProfList();
    }



    private void countChosen() {
        mChoosen = 0;
        for (ProfileC prof : App.getProfList()) {
            if (prof.chosen) {
                mChoosen++;
            }
        }
    }




    public String getPageTitle() {
        return "hosts";
    }




    public void updateMenu() {
        if (null == mEditMeit) return;
        mEditMeit.setVisible((App.gCurProf != null));
        mDeleteMeit.setVisible((App.gCurProf != null));
    }







    public class ProfsAdapter extends ArrayAdapter<ProfileC> {

        private Context mContext;
        private int mLayout;
        List<ProfileC> mItems;

        public ProfsAdapter(Context context, int resource, List<ProfileC> objects) {
            super(context, resource, objects);
            this.mContext = context;
            this.mLayout = resource;
            this.mItems = objects;
        }


        public ProfileC getItem(int i)
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
            final ProfileC prof = mItems.get(position);
            if ( (App.gCurProf != null) && App.gCurProf.name.equals(prof.name) )
                rowView.setBackgroundColor(0xffcccccc);
            else
                rowView.setBackgroundColor(0xffffffff);

            CheckBox cb = (CheckBox) rowView.findViewById(R.id.main_row_chkbox);
            if (null != cb) {
                cb.setTag(position);
                if (prof.chosen)
                    cb.setChecked(true);
                cb.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        rowChecked((CheckBox) v);
                    }
                });
            }

            TextView tvName = (TextView) rowView.findViewById(R.id.row_prof_name);
            tvName.setText(prof.name);

            TextView tvReso = (TextView) rowView.findViewById(R.id.row_prof_resolution);
            tvReso.setText(prof.blendResolution());

            TextView tvUser = (TextView) rowView.findViewById(R.id.row_prof_bitrate);
            tvUser.setText(prof.bitrate + " kbps");

            return rowView;
        }
    }





    public void addProf() {
        App.pNewProfInner = null;
        App.pNewProfOuter = null;
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivityForResult(intent, App.ACT_RESULT_ADD_PROF);
        App.makeAnim(MainActivity.this);
    }



    public void onAddProfResult(int aResultCode) {
        if (aResultCode == Activity.RESULT_OK) {
            ///Toast.makeText(getApplication(), "onNewHostResult", Toast.LENGTH_SHORT).show();
            ProfileC prof = getProfByName(App.pNewProfOuter.name);
            if (null != prof)
                App.getProfList().remove(prof);
            App.getProfList().add(App.pNewProfOuter);

            saveProfList();
            App.gCurProf = null;
            updateUI();
        }
    }



    public void editProf() {
        if (App.gCurProf == null) return;
        App.pNewProfInner = App.gCurProf;
        App.pNewProfOuter = null;
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivityForResult(intent, App.ACT_RESULT_EDIT_PROF);
        App.makeAnim(MainActivity.this);
    }



    public void onEditProfResult(int aResultCode) {
        if (aResultCode == Activity.RESULT_OK) {
            ProfileC host = getProfByName(App.pNewProfInner.name);
            if (null != host)
                App.getProfList().remove(host);
            App.getProfList().add(App.pNewProfOuter);

            saveProfList();
            App.gCurProf = null;
            updateUI();
        }
    }



    public void deleteProf() {
        if (App.gCurProf == null) return;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileC prof = getProfByName(App.gCurProf.name);
                if (null == prof) return;
                App.getProfList().remove(prof);

                saveProfList();
                App.gCurProf = null;
                updateUI();
            }
        });
        alertDialog.setNegativeButton("No", null);
        alertDialog.setMessage(App.gCurProf.name + " profile will be deleted! Are you sure?");
        alertDialog.setTitle("Delete profile");
        alertDialog.show();
    }



















    private void loadCameras() {
        String code = Tools.readPrivateFile(this, App.CAMERAS_XML);
        if (code.isEmpty()) {
            App.showWaitDlg(this, "Detecting cameras...");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    findCameras_first();
//                }
//            }).start();

            findCameras_first();
        } else {
            readCameras(code);
        }
    }



    private void findCameras_first() {
        // first run, detect cameras
        Lmx xml = new Lmx();
        Camera camera = Caminf.openFrontCamera();
        if (null != camera) {
            Caminf.addCameraSizes(camera, App.gFrontCam.resolutions);
            camera.release();

            App.logLine("front camera sizes");
            packCameraSizes(xml, App.gFrontCam);
            xml.pushNode("front_sizes");
        }

        camera = Caminf.openBackCamera();
        if (null != camera) {
            Caminf.addCameraSizes(camera, App.gBackCam.resolutions);
            camera.release();

            App.logLine("back camera sizes");
            packCameraSizes(xml, App.gBackCam);
            xml.pushNode("back_sizes");
        }

        xml.pushNode("cameras");
        final String code = xml.getCode();
        Tools.writePrivateFile(this, App.CAMERAS_XML, code);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findCameras_second(code);
            }
        });
    }



    private void packCameraSizes(Lmx aXml, Caminf aCaminf) {
        aCaminf.resolutions.sort();
        for (int i = 0; i < aCaminf.resolutions.length(); i++) {
            Size2 size = aCaminf.resolutions.get(i);
            App.logLine("cam size: " + size.width + "x" + size.height);
        }

        for (int i=0; i<aCaminf.resolutions.length(); i++) {
            Size2 size = aCaminf.resolutions.get(i);
            aXml.addInt("width", size.width);
            aXml.addInt("height", size.height);
            aXml.pushItem("item");
        }
    }



    private void findCameras_second(String aCode) {
        App.hideWaitDlg();
        readCameras(aCode);
    }



    private void readCameras(String aCode) {
        if (aCode.isEmpty()) {
            App.finishMessage(this, getResources().getText(R.string.app_name).toString(), "No camera is found!");
            return;
        }

        try {
            Lmx xml = new Lmx(aCode);
            xml.pullNode("front_sizes");
            if (!xml.isEmpty()) {
                readCameraSizes(xml, App.gFrontCam);
            } else
                App.gFrontCam = null;

            xml.pullNode("back_sizes");
            if (!xml.isEmpty()) {
                readCameraSizes(xml, App.gBackCam);
            } else
                App.gBackCam = null;
        } catch (Exception ex) {
            App.logLine(" *** " + ex.getMessage());
        }

        if ( ((null==App.gFrontCam) && (null==App.gBackCam)) ) {
            App.finishMessage(this, getResources().getText(R.string.app_name).toString(), "No camera is found!");
            return;
        }
    }



    private void readCameraSizes(Lmx aXml, Caminf aCaminf) {
        aCaminf.resolutions.clear();
        while (true) {
            aXml.pullItem("item");
            if (aXml.isEmpty()) break;
            int width = aXml.getInt("width");
            int height = aXml.getInt("height");
            aCaminf.resolutions.addSize(new Size2(width, height));
        }
    }













    public ProfileC getProfByName(String aName) {
        for (int i=0; i< App.getProfList().size(); i++) {
            ProfileC prof = App.getProfList().get(i);
            if (prof.name.equalsIgnoreCase(aName))
                return prof;
        }
        return null;
    }



    private int bitrateFromHeight(int aHeight) {
        int result = 8000;
        if (aHeight < 200)
            result = 500;
        else if (aHeight < 400)
            result = 1000;
        else if (aHeight < 600)
            result = 2000;
        else if (aHeight < 800)
            result = 4000;
        return result;
    }



    private ProfileC makeProfBySize(String aPrefix, Size2 aSize) {
        if (null == aSize) return null;
        ProfileC prof = new ProfileC();
        prof.width = aSize.width;
        prof.height = aSize.height;
        prof.bitrate = bitrateFromHeight(aSize.height);
        prof.name = aPrefix + prof.height;
        return prof;
    }



    private void createFrontDefaults() {
        App.logLine("createFrontDefaults_11");
        if (null == App.gFrontCam) return;
        Size2 sz2, sz1, sz0;

        App.logLine("createFrontDefaults_22");
        sz2 = App.gFrontCam.resolutions.getBelowHeight(720);
        App.logLine("createFrontDefaults_33: " + sz2);
        ProfileC prof2 = makeProfBySize("fp_", sz2);

        if (sz2.height > 480)
            sz1 = App.gFrontCam.resolutions.getBelowHeight(480);
        else
            sz1 = App.gFrontCam.resolutions.getBelowHeight(240);
        ProfileC prof1 = makeProfBySize("fp_", sz1);

        if (sz1.height > 240)
            sz0 = App.gFrontCam.resolutions.getBelowHeight(240);
        else
            sz0 = App.gFrontCam.resolutions.getBelowHeight(120);
        ProfileC prof0 = makeProfBySize("fp_", sz0);

        if (null != prof0) App.gFrontProfs.add(prof0);
        if (null != prof1) App.gFrontProfs.add(prof1);
        if (null != prof2) App.gFrontProfs.add(prof2);
    }



    private void createBackDefaults() {

        if (null == App.gBackCam) return;
        Size2 sz2, sz1, sz0;

        sz2 = App.gBackCam.resolutions.getBelowHeight(720);
        ProfileC prof2 = makeProfBySize("bp_", sz2);

        if (sz2.height > 480)
            sz1 = App.gBackCam.resolutions.getBelowHeight(480);
        else
            sz1 = App.gBackCam.resolutions.getBelowHeight(240);
        ProfileC prof1 = makeProfBySize("bp_", sz1);

        if (sz1.height > 240)
            sz0 = App.gBackCam.resolutions.getBelowHeight(240);
        else
            sz0 = App.gBackCam.resolutions.getBelowHeight(120);
        ProfileC prof0 = makeProfBySize("bp_", sz0);

        if (null != prof0) App.gBackProfs.add(prof0);
        if (null != prof1) App.gBackProfs.add(prof1);
        if (null != prof2) App.gBackProfs.add(prof2);
    }



    public void loadProfList() {
        String code = Tools.readPrivateFile(this, App.PROFILES_XML);
        if (code.isEmpty()) {
            // first run, create default profiles
            createFrontDefaults();
            createBackDefaults();
            saveProfList();
        }

        try {
            code = Tools.readPrivateFile(this, App.PROFILES_XML);
            Lmx xml = new Lmx(code);

            xml.pullNode("front_profiles");
            pullProfiles(xml, App.gFrontProfs);

            xml.pullNode("back_profiles");
            pullProfiles(xml, App.gBackProfs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void pullProfiles(Lmx aXml, List<ProfileC> aProfList) {
        aProfList.clear();
        while (true) {
            aXml.pullItem("profile");
            if (aXml.isEmpty()) return;
            ProfileC prof = new ProfileC();
            prof.name = aXml.getStr("name");

            prof.width = aXml.getInt("width");
            if (prof.width < 99) prof.width = 99;

            prof.height = aXml.getInt("height");
            if (prof.height < 99) prof.height = 99;

            prof.bitrate = aXml.getInt("bitrate");
            if (prof.bitrate < 99) prof.bitrate = 99;

            prof.chosen = aXml.getBln("chosen");
            aProfList.add(prof);
        }
    }



    public void saveProfList() {
        Lmx xml = new Lmx();
        packProfiles(xml, App.gFrontProfs);
        xml.pushNode("front_profiles");

        packProfiles(xml, App.gBackProfs);
        xml.pushNode("back_profiles");

        String code = xml.getCode();
        Tools.writePrivateFile(this, App.PROFILES_XML, code);
    }



    private void packProfiles(Lmx aXml, List<ProfileC> aList) {
        for (int i=0; i< aList.size(); i++) {
            ProfileC prof = aList.get(i);
            aXml.addStr("name", prof.name);
            aXml.addInt("width", prof.width);
            aXml.addInt("height", prof.height);
            aXml.addInt("bitrate", prof.bitrate);
            aXml.addBln("chosen", prof.chosen);
            aXml.pushItem("profile");
        }
    }






}
