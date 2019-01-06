package com.rustero.mains;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.rustero.App;
import com.rustero.tools.Tools;
import com.rustero.units.MimeInfo;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class OutputActivity extends AppCompatActivity  implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG = "OutputActivity";
    private AdView mAdView;
    static public class FilmListC extends ArrayList<FilmItemC> {}
    private Toolbar mToolbar;
    private String mTitle;
    public long mTotalFiles = 0;
    public long mTotalBytes = 0;
    private ListView mListView;
    private LocalArrayAdapter mAdapter;
    private FilmListC mFilmList;
    private MetaTask mMetaTask = new MetaTask();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.output_activity);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        mTitle = "Output: " + App.getOutputFolder();
        setTitle(mTitle);

        mListView = (ListView) findViewById(R.id.loc_page_list);
        mListView.setOnItemClickListener(this);
        mAdView = (AdView) findViewById(R.id.output_adView);
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
                        updateStatus();
                    }
                });
            }
        };
        adThread.start();
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FilmItemC item = mAdapter.getItem(position);
        String path = item.pPath;
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/mp4");
        startActivity(intent);
    }





    // load the folder content from the disk with icons and dates
    public void pickupFolder()
    {
        mTotalFiles = 0;
        mTotalBytes = 0;
        mFilmList = new FilmListC();
        File folder = new File(App.getOutputFolder());
        File[] folderItems = folder.listFiles();
        try{
            for(File item: folderItems)
            {
                if (!item.isFile()) continue;
                String ext = Tools.getFileExt(item.getName());
                if (!ext.equals("mp4")) continue;
                Date lastModDate = new Date(item.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                FilmItemC fiit = new FilmItemC();
                mFilmList.add(fiit);
                fiit.pName = item.getName();
                fiit.pDetails = Tools.formatFileSize(item.length());
                fiit.pPath = item.getAbsolutePath();
                fiit.pDate = date_modify;
                fiit.pSize = item.length();
                mTotalFiles++;
                mTotalBytes += fiit.pSize;
            }
        } catch(Exception e) {}
        Collections.sort(mFilmList);
    }



    // update the folder view
    public void updateFolder() {
        mMetaTask.quit = true;
        pickupFolder();
        mAdapter = new LocalArrayAdapter(this, R.layout.out_row, mFilmList);
        mListView.setAdapter(mAdapter);
        updateStatus();

        mMetaTask = new MetaTask();
        mMetaTask.execute();
    }



    private void updateStatus() {
        TextView view = (TextView) findViewById(R.id.output_tevi_status);
        if (mTotalFiles == 0) {
            view.setText("No videos found");
        } else {
            String text;
            if (mTotalFiles == 1)
                text = "One video, size " + Tools.formatFileSize(mTotalBytes);
            else
                text = mTotalFiles + " videos, total size " + Tools.formatFileSize(mTotalBytes);
            view.setText(text);
        }
    }









    public class LocalArrayAdapter extends ArrayAdapter<FilmItemC> {

        private Context mContext;
        private int mLayout;
        List<FilmItemC> mItems;

        public LocalArrayAdapter(Context context, int resource, List<FilmItemC> objects) {
            super(context, resource, objects);
            this.mContext = context;
            this.mLayout = resource;
            this.mItems = objects;
        }


        public FilmItemC getItem(int i)
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
            final FilmItemC item = mItems.get(position);
            if (item == null) return null;

            ImageView iconView1 = (ImageView) rowView.findViewById(R.id.out_row_icon1);
            TextView tv_dur = (TextView) rowView.findViewById(R.id.out_row_duration);

            if (item.meta == null)
                iconView1.setImageDrawable(MimeInfo.GetIcon("mp4"));
            else {
                if (item.meta.icon1 != null)
                    iconView1.setImageDrawable(item.meta.icon1);
                if (item.meta.duration > 0)
                    tv_dur.setText( Tools.formatDuration(item.meta.duration));
            }

            TextView t1 = (TextView) rowView.findViewById(R.id.out_row_name);
            if(t1!=null) t1.setText(item.pName);

            TextView t2 = (TextView) rowView.findViewById(R.id.out_row_size);
            if(t2!=null) t2.setText(item.pDetails);

            TextView t3 = (TextView) rowView.findViewById(R.id.loc_row_date);
            if(t3!=null) t3.setText(item.pDate);

            return rowView;
        }
    }





    private void onShowActivity() {
        updateFolder();
        askBannerAd();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.log_menu, menu);
        onShowActivity();
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }















    public class FilmItemC implements Comparable<FilmItemC> {


        public boolean dated;
        public long pSize;
        public String pName;
        public String pDetails;
        public String pDate;
        public String pPath;
        public App.MetaC meta;


        public int compareTo(FilmItemC aFiit) {
            if (this.pName != null)
                return this.pName.toLowerCase().compareTo(aFiit.pName.toLowerCase());
            else
                throw new IllegalArgumentException();
        }
    }










    public class MetaTask extends AsyncTask<Void, Integer, String> {

        long mKick;
        private volatile boolean quit;
        private FilmListC list = new FilmListC();
        private App.MetaHeapC heap = new App.MetaHeapC();

        @Override
        protected String doInBackground(Void... params) {
            try {
                for (FilmItemC fi : mFilmList) {
                    if (quit) return "";
                    App.MetaC meta = App.gMetaHeap.get(fi.pName);
                    if (null == meta)
                        meta = getMeta(fi.pPath);
                    fi.meta = meta;
                    list.add(fi);
                    heap.put(fi.pName, meta);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }



        @Override
        protected void onPostExecute(String result) {
            if (quit) return;
            App.gMetaHeap = heap;
            mFilmList = list;
            mAdapter = new LocalArrayAdapter(OutputActivity.this, R.layout.out_row, mFilmList);
            mListView.setAdapter(mAdapter);
        }


        private void took(String aTag) {
            long took = System.currentTimeMillis() - mKick;
            Log.d(LOG_TAG, " * took " + aTag + ": " + took);
        }



        private App.MetaC getMeta(String aPath) {
            mKick = System.currentTimeMillis();
            final int ICON_SIZE = 96;
            App.MetaC result = new App.MetaC();
            Bitmap bitmap = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            took("11");
            try {
                retriever.setDataSource(aPath);
                String stime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                result.duration = Long.parseLong(stime) / 1000;
                bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC);
                took("22");
            } catch (IllegalArgumentException ex) {
                // Assume this is a corrupt video file
            } catch (RuntimeException ex) {
                // Assume this is a corrupt video file.
            } finally {
                try {
                    retriever.release();
                } catch (RuntimeException ex) {
                    // Ignore failures while cleaning up.
                }
            }
            if (null == bitmap) return result;

            took("77");
            float scale;
            if (bitmap.getWidth() < bitmap.getHeight()) {
                scale = ICON_SIZE / (float) bitmap.getWidth();
            } else {
                scale = ICON_SIZE / (float) bitmap.getHeight();
            }
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            bitmap = transform(matrix, bitmap, ICON_SIZE, ICON_SIZE);

            took("88");
            if (null != bitmap) {
                bitmap = roundedBitmap(bitmap);
                result.icon1 = new BitmapDrawable(OutputActivity.this.getResources(), bitmap);
            }

            took("99");
            return result;
        }





        private Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight) {
            boolean scaleUp = true;
            boolean recycle = true;

            int deltaX = source.getWidth() - targetWidth;
            int deltaY = source.getHeight() - targetHeight;
            if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
            * In this case the bitmap is smaller, at least in one dimension,
            * than the target.  Transform it by placing as much of the image
            * as possible into the target and leaving the top/bottom or
            * left/right (or both) black.
            */
                Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
                    Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b2);

                int deltaXHalf = Math.max(0, deltaX / 2);
                int deltaYHalf = Math.max(0, deltaY / 2);
                Rect src = new Rect(
                    deltaXHalf,
                    deltaYHalf,
                    deltaXHalf + Math.min(targetWidth, source.getWidth()),
                    deltaYHalf + Math.min(targetHeight, source.getHeight()));
                int dstX = (targetWidth  - src.width())  / 2;
                int dstY = (targetHeight - src.height()) / 2;
                Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
                c.drawBitmap(source, src, dst, null);
                if (recycle) {
                    source.recycle();
                }
                c.setBitmap(null);
                return b2;
            }
            float bitmapWidthF = source.getWidth();
            float bitmapHeightF = source.getHeight();

            float bitmapAspect = bitmapWidthF / bitmapHeightF;
            float viewAspect   = (float) targetWidth / targetHeight;

            if (bitmapAspect > viewAspect) {
                float scale = targetHeight / bitmapHeightF;
                if (scale < .9F || scale > 1F) {
                    scaler.setScale(scale, scale);
                } else {
                    scaler = null;
                }
            } else {
                float scale = targetWidth / bitmapWidthF;
                if (scale < .9F || scale > 1F) {
                    scaler.setScale(scale, scale);
                } else {
                    scaler = null;
                }
            }

            Bitmap b1;
            if (scaler != null) {
                // this is used for minithumb and crop, so we want to filter here.
                b1 = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(), source.getHeight(), scaler, true);
            } else {
                b1 = source;
            }

            if (recycle && b1 != source) {
                source.recycle();
            }

            int dx1 = Math.max(0, b1.getWidth() - targetWidth);
            int dy1 = Math.max(0, b1.getHeight() - targetHeight);

            Bitmap b2 = Bitmap.createBitmap(
                b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

            if (b2 != b1) {
                if (recycle || b1 != source) {
                    b1.recycle();
                }
            }

            return b2;
        }




        public Bitmap roundedBitmap(Bitmap bitmap) {
            Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);
            final float roundPx = 12;

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);

            return result;
        }





    }




}
