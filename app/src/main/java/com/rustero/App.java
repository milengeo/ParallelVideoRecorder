package com.rustero;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.rustero.mains.ProfileC;
import com.rustero.mains.R;
import com.rustero.tools.Caminf;
import com.rustero.tools.Tools;
import com.rustero.tools.Lmx;
import com.rustero.units.MimeInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class App extends Application {
    static final public boolean DEV = false;
    static final public String LOG_TAG = "pavire_app";
    static final public int ACT_RESULT_SETTINGS = 1;
    static final public int ACT_RESULT_ADD_PROF = 2;
    static final public int ACT_RESULT_EDIT_PROF = 3;

    static final public String LOG_TXT = "log.txt";
    static final public String CAMERAS_XML = "cameras.xml";
    static final public String PROFILES_XML = "profiles.xml";
    static final public String APP_STATE_XML = "app_state.xml";
    static final public String MAIN_STATE_XML = "main_state.xml";
    static final public String NAMECOUNT_XML = "namecount.xml";

    public static App self;
    public static boolean live;
    public static boolean gIsPro = true;
    public static long gAppTick, gFirstRun, gRunCount, gInterstitialCount;
    public static boolean gNowFront = true;
    public static Context gCurContext = null;
    public static Caminf gFrontCam = new Caminf();
    public static Caminf gBackCam = new Caminf();

    static public List<ProfileC> gFrontProfs = new ArrayList<ProfileC>();
    static public List<ProfileC> gBackProfs = new ArrayList<ProfileC>();
    static public ProfileC gCurProf;

    //static public List<String> gFpsList = new ArrayList<String>();
    static public List<String> gBitrateList = new ArrayList<String>();

    //static public String pLocRoot;
    //static public String pLocDir;

    static public ProfileC pNewProfInner = null;
    static public ProfileC pNewProfOuter = null;

    static public ProgressDialog gWaitDlg;
    public static SimpleDateFormat LogStamp = null;
    public static String gLogText;

    public static MetaHeapC gMetaHeap = new MetaHeapC();
    static public class MetaHeapC extends HashMap<String, MetaC> {}

    private ArrayList<Integer> mAnimList = new ArrayList<>();
    private int mNextAnim = -1;



    @Override
    public void onCreate() {
        super.onCreate();
        self = this;

//        if (android.os.Debug.isDebuggerConnected()) {
//            Tools.delay(999);
//        }

        LogStamp = new SimpleDateFormat("HH:mm:ss.SSS");
        gLogText = Tools.readPrivateFile(self, LOG_TXT);
        logLine(getSysInfo());
        setDefaultPrefs();

        gAppTick = System.currentTimeMillis();
        loadAppState();
        if (gFirstRun == 0) gFirstRun = System.currentTimeMillis()/1000;
        gRunCount++;
        saveLastState();

        new MimeInfo(this);
        mAnimList.add(new Integer(R.anim.fade));
        mAnimList.add(new Integer(R.anim.hslide));
        mAnimList.add(new Integer(R.anim.hunfold));
        mAnimList.add(new Integer(R.anim.hvunfold));
        mAnimList.add(new Integer(R.anim.rotate180));
        mAnimList.add(new Integer(R.anim.vslide));
        mAnimList.add(new Integer(R.anim.vslide));


        Resources res = getResources();
        gIsPro = res.getBoolean(R.bool.is_pro);
    }



    static public void logLine(String aLine) {
        Log.d(LOG_TAG, aLine);

        String stamp = LogStamp.format(new Date());
        gLogText += stamp + " " + aLine + "\n";
        if (gLogText.length() > 9999)
            gLogText = gLogText.substring(5555);
        Tools.writePrivateFile(self, App.LOG_TXT, gLogText);
    }


    public static String getSysInfo() {
        String s = "\n---";
        s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
        s += "\n Device: " + android.os.Build.DEVICE;
        s += "\n Model: " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")";
        s += "\n---";
        return s;
    }


    static public boolean wantBannerAd() {
        if (gIsPro) return false;
        if (isBabyInstallation()) return false;
        return true;
    }


    static public boolean wantInterstitialAd() {
        final int INTERSTITIAL_LEAP = 5;
        if (gIsPro) return false;
        if (isBabyInstallation()) return false;
        gInterstitialCount++;
        if (gInterstitialCount > INTERSTITIAL_LEAP) gInterstitialCount = 0;
        if (gInterstitialCount != INTERSTITIAL_LEAP) return false;
        return true;
    }


    private void loadAppState() {
        String code = Tools.readPrivateFile(this, APP_STATE_XML);
        if (code.isEmpty()) return;

        Lmx xml = new Lmx(code);
        xml.pullNode("last_state");
        gFirstRun = xml.getLong("first_run");
        gRunCount = xml.getLong("run_count");
    }


    private void saveLastState() {
        Lmx xml = new Lmx();
        xml.addLong("first_run", gFirstRun);
        xml.addLong("run_count", gRunCount);
        xml.pushNode("last_state");
        String code = xml.getCode();
        Tools.writePrivateFile(this, APP_STATE_XML, code);
    }




    static public boolean isBabyInstallation() {
        if (getInstallDays() < 2) return true;
        if (gRunCount < 9) return true;
        return false;
    }


    public static long getInstallDays() {
        long nowSecs = System.currentTimeMillis()/1000;
        long sinceSecs = (nowSecs - gFirstRun);
        long result = sinceSecs/60/60/24;
        return result;
    }


    public static long getAppSecs() {
        long nowSecs = System.currentTimeMillis()/1000;
        long result = (nowSecs - gAppTick/1000);
        return result;
    }


    public static String getAppName() {
        if (null == self) return "";
        return self.getResources().getText(R.string.app_name).toString();
    }


    public static String takeNameCount() {
        String code = Tools.readPrivateFile(self, App.NAMECOUNT_XML);
        Lmx xml = new Lmx(code);
        xml.pullNode("last");
        int count = xml.getInt("count");
        count++;

        xml = new Lmx();
        xml.addInt("count", count);
        xml.pushNode("last");
        code = xml.getCode();
        Tools.writePrivateFile(self, App.NAMECOUNT_XML, code);
        String result = String.format("%04d", count);
        return result;
    }


    static public Caminf getCaminf() {
        Caminf result;
        if (gNowFront) {
            result = gFrontCam;
        } else {
            result = gBackCam;
        }
        return result;
    }


    static public List<ProfileC> getProfList() {
        List<ProfileC> result;
        if (gNowFront)
            result = gFrontProfs;
        else
            result = gBackProfs;
        Collections.sort(result);
        return result;
    }


    public static void showShortToast(String aText) {
        if (null == self) return;
        Toast.makeText(self, aText, Toast.LENGTH_SHORT).show();
    }


    public static void showLongToast(String aText) {
        if (null == self) return;
        Toast.makeText(self, aText, Toast.LENGTH_LONG).show();
    }


    static public class TipC {
        int Count, Total, Duration;
        String Text;

        public TipC(String aText) {
            Count = 0;
            Total = 1;
            Duration = Toast.LENGTH_LONG;
            Text = aText;
        }

        public void show() {
            if (Count >= Total) return;
            Count++;
            Toast.makeText(App.gCurContext.getApplicationContext(), Text, Duration).show();
        }

        public void cease() {
            Count = Total;
        }
    }


    static public void showAlert(Context aContext, String aTitle, String aText) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(aContext);
        alertDialog.setIcon(R.drawable.pavire1);
        alertDialog.setPositiveButton("Ok", null);
        alertDialog.setMessage(aText);
        alertDialog.setTitle(aTitle);
        alertDialog.show();
    }


    static public void finishMessage(final Activity aActivity, String aTitle, String aText) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(aActivity);
        alertDialog.setIcon(R.drawable.pavire1);
        alertDialog.setMessage(aText);
        alertDialog.setTitle(aTitle);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                aActivity.finish();
            }
        });
        alertDialog.show();
    }


    static public void showWaitDlg(Context aContext, String aMessage) {
        gWaitDlg = new ProgressDialog(aContext);
        gWaitDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        gWaitDlg.setMessage(aMessage);
        gWaitDlg.setCanceledOnTouchOutside(false);
        gWaitDlg.setCancelable(false);
        gWaitDlg.show();
    }


    static public void hideWaitDlg() {
        if (null == gWaitDlg) return;
        gWaitDlg.dismiss();
        gWaitDlg = null;
    }


    static public int getMaxDuration() {
        int result = 5;
        if (null == self) return result;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
        String text = preferences.getString("max_duration", "5");
        if ((text != null) && (text.length() > 0)) {
            result = Tools.parseInt(text);
        }

        if (result < 1) result = 1;
        if (!App.gIsPro) {
            if (result > 5) result = 5;
        }

        return result;
    }


    private void setDefaultPrefs() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
        String text = preferences.getString("output_folder", "");
        if (Tools.isBlank((text))) {
            text = getOutputFolder();
            preferences.edit().putString("output_folder", text).commit();
        }
        text = preferences.getString("max_duration", "");
        if (Tools.isBlank((text))) {
            text = Integer.toString(getMaxDuration());
            preferences.edit().putString("max_duration", text).commit();
        }
    }



    static public boolean getPrefBln(String aName)
    {
        boolean result = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getBoolean(aName, false);
        return result;
    }


    static public int getPrefInt(String aName)
    {
        int result = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getInt(aName, 0);
        return result;
    }


    static public String getPrefStr(String aName)
    {
        String result = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getString(aName, "");
        return result;
    }


    static public void makeAnim(Activity aActivity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        Boolean disabled = getPrefBln("disable_animations");
        if (disabled) return;
        self.mNextAnim++;
        if (self.mNextAnim >= self.mAnimList.size())
            self.mNextAnim = 0;

        int atag = self.mAnimList.get(self.mNextAnim);
        aActivity.overridePendingTransition(atag, 0);
    }


    static public String getOutputFolder() {
        if (null == self) return "";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
        String result = preferences.getString("output_folder", "");

        if ((result != null) && (result.length() > 0)) {
            File folder = new File(result);
            if (folder.isDirectory())
                return result;
        }

        result = getDefaultFolder();
        File folder = new File(result);
        folder.mkdirs();
        if (folder.isDirectory())
            return result;

        return "/";
    }


    static public String getDefaultFolder() {
        String result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        result += "/parallel_video_recorder";
        return result;
    }





    static public class MetaC {
        public Drawable icon1;
        public Long duration;
    }


}
