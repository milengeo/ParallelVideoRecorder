package com.rustero.mains;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.rustero.App;
import com.rustero.tools.Caminf;
import com.rustero.tools.Tools;


public class ProfileActivity extends Activity {

    private boolean mModified = false;
    private ProfileC mProf = null;
    private View.OnKeyListener mKeyListener;
    private Spinner spnResolution, /*mFps,*/ spnBitrate;
    private Caminf mCaminf;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_layout);
        mProf = new ProfileC();

        final View scvi = findViewById(R.id.profile_ScrollView);
        ViewTreeObserver vto = scvi.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    scvi.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    scvi.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                int w1 = scvi.getWidth();
                View tola = findViewById(R.id.new_host_TopLayout);
                int w2 = tola.getWidth();
                int pa = (w1-w2)/2;
                scvi.setPadding(pa, 44, 0, 66);
            }
        });

        mKeyListener = new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // your custom implementation
                if (event.getAction() == KeyEvent.ACTION_DOWN &&  keyCode == KeyEvent.KEYCODE_ENTER) {
                    clickOk(v);
                    return true; // indicate that we handled event, won't propagate it
                }
                return false; // when we don't handle other keys, propagate event further
            }
        };
        findViewById(R.id.profile_edit_name).setOnKeyListener(mKeyListener);

        spnResolution = (Spinner) findViewById(R.id.profile_spn_resolution);
        //mFps = (Spinner) findViewById(R.id.new_prof_spn_fps);
        spnBitrate = (Spinner) findViewById(R.id.profile_spn_bitrate);

        setupForm();
        populateForm();
    }



    private void setupForm() {

        String name;

        if (App.gNowFront) {
            name = "fp";
            mCaminf = App.gFrontCam;
        } else {
            name = "bp";
            mCaminf = App.gBackCam;
        }
        int idx = mCaminf.resolutions.findByHeight(480);
        name += "_" + mCaminf.resolutions.get(idx).height;

        ArrayAdapter<String> resoAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item);
        for (int i=0; i< mCaminf.resolutions.length(); i++) {
            String str = mCaminf.resolutions.get(i).getResolutio();
            resoAdapter.add(str);
        }
        spnResolution.setAdapter(resoAdapter);
        spnResolution.setSelection(idx);

        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, getResources().getStringArray(R.array.new_bitrate_list));
        spnBitrate.setAdapter(bitrateAdapter);

        idx = Tools.getAboveInt(App.gBitrateList, 1000);
        spnBitrate.setSelection(idx);

        EditText edit;
        edit = (EditText) findViewById(R.id.profile_edit_name);
        edit.setText(name);
    }




    @Override
    public void onBackPressed() {
        collectForm();
        askCancel();
    }



    private void sayOk(String aTitle, String aText) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setPositiveButton("Ok", null);
        alertDialog.setMessage(aText);
        alertDialog.setTitle(aTitle);
        alertDialog.show();
    }



    public void clickCancel(View button) {
        collectForm();
        askCancel();
    }



    public void clickOk(View button) {
        collectForm();

        if (mProf.name.isEmpty()) {
            sayOk("New profile", "Profile name cannot be empty!");
            return;
        }

        App.pNewProfOuter = mProf;
        Intent result = new Intent();
        setResult(Activity.RESULT_OK, result);
        finish();
    }



    private void doCancel() {
        Intent result = new Intent();
        setResult(Activity.RESULT_CANCELED, result);
        finish();
    }



    private void askCancel() {

        mModified = false;
        if (!mModified) {
            doCancel();
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doCancel();
            }
        });
        alertDialog.setNegativeButton("No", null);
        alertDialog.setMessage("Typed data will be lost, do you really want to exit?");
        alertDialog.setTitle("New host");
        alertDialog.show();
    }




    private void populateForm() {
        if (null == App.pNewProfInner) return;
        EditText edit;
        String str;
        int idx;

        edit = (EditText) findViewById(R.id.profile_edit_name);
        edit.setText(App.pNewProfInner.name);

        idx = mCaminf.resolutions.findSize(App.pNewProfInner.width, App.pNewProfInner.height);
        spnResolution.setSelection(idx);

        idx = Tools.getAboveInt(App.gBitrateList, App.pNewProfInner.bitrate);
        spnBitrate.setSelection(idx);


//        edit.setText(Integer.toString(App.pNewProfInner.outRate));
    }



    private void collectForm() {
        mModified = false;
        String str;
        EditText edit;

        edit = (EditText) findViewById(R.id.profile_edit_name);
        mProf.name = edit.getText().toString();

        if (null == App.pNewProfInner) {
            if (!mProf.name.isEmpty())
                mModified = true;
        } else {
            if (!mProf.name.equals(App.pNewProfInner.name))
                mModified = true;
        }

        str = spnResolution.getSelectedItem().toString();
        mProf.parseResolution(str);

//        str = mFps.getSelectedItem().toString();
//        mProf.fps = Tools.parseInt(str);

        str = spnBitrate.getSelectedItem().toString();
        mProf.bitrate = Tools.parseInt(str);
    }


}



