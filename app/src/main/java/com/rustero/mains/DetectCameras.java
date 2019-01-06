package com.rustero.mains;


import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.rustero.App;


public class DetectCameras extends AsyncTask<Void, Void, Void> {

        private ProgressDialog mDlg;

        @Override
        protected void onPreExecute() {
            mDlg = new ProgressDialog(App.gCurContext);
            mDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDlg.setMessage("Detecting cameras...");
            mDlg.setCanceledOnTouchOutside(false);
            mDlg.setCancelable(false);
            mDlg.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // Execute query here
            try {
                Thread.sleep(999);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mDlg.dismiss();
        }

}
