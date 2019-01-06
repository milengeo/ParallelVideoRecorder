package com.rustero.mains;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.rustero.App;

import java.io.File;
import java.io.FileOutputStream;


public class LogActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_activity);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        mTitle = getString(R.string.app_name) + " log";
        setTitle(mTitle);

        TextView view = (TextView) findViewById(R.id.log_view);
        view.append(App.gLogText);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log_menu, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.log_meit_copy) {
            copyToFile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    private void copyToFile() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        File file = new File(path + "/log.txt");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(App.gLogText.getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(App.LOG_TAG, e.getMessage(), e);
        }
        App.showLongToast("Log is copied to " + path);
    }



    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(mTitle, App.gLogText);
        clipboard.setPrimaryClip(clip);
        App.showLongToast("Log is copied to the clipboard");
    }
}
