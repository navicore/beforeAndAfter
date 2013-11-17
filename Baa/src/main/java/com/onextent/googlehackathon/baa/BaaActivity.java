package com.onextent.googlehackathon.baa;

import com.onextent.googlehackathon.baa.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class BaaActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.baa_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
        final View picContentView = findViewById(R.id.fullscreen_pic);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                            if (visible) {
                                getActionBar().show();
                            } else {
                                getActionBar().hide();
                            }
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });
        picContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.camera_button).setOnTouchListener(mDelayHideTouchListener);

        findViewById(R.id.camera_button).setOnClickListener(mCameraButtonListener);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri beforeFileUri;
    private Uri afterFileUri;
    private Uri finalFileUri;

    private void setUri(Uri uri) {
        switch (state) {
            case BEFORE:
                beforeFileUri = uri;
            case AFTER:
                afterFileUri = uri;
            default:
                finalFileUri = uri;
        }
    }

    private Uri getUri() {
        switch (state) {
            case BEFORE:
                return beforeFileUri;
            case AFTER:
                return afterFileUri;
            default:
                return finalFileUri;
        }
    }

    private View.OnClickListener mCameraButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            setUri(getOutputMediaFileUri()); // create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getUri()); // set the image file name

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    };

    private static enum STATE {BEFORE, AFTER, DONE}

    ;

    private STATE state = STATE.BEFORE;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                Object obj = null;
                if (data != null)
                    obj = data.getData();
                switch (state) {

                    case AFTER:
                        Toast.makeText(this, "after image saved", Toast.LENGTH_LONG).show();
                        displayPic();
                        break;
                    default:
                        Toast.makeText(this, "before image saved", Toast.LENGTH_LONG).show();
                        displayPic();
                        state = STATE.BEFORE;
                        break;
                }

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
    }

    private static final int PIC_DISPLAY_INTERVAL = 2000;
    private static final int PIC_PROCESSING_INTERVAL = 200;

    private void displayPic() {

        ImageView imageView = (ImageView) findViewById(R.id.fullscreen_pic);
        View textView = findViewById(R.id.fullscreen_text);
        Bitmap image = (Bitmap) BitmapFactory.decodeFile(getUri().getEncodedPath());
        textView.setVisibility(View.INVISIBLE);
        imageView.setImageBitmap(image);
        imageView.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                processState();
            }
        }, PIC_DISPLAY_INTERVAL);
    }

    private void processState() {

        ImageView imageView = (ImageView) findViewById(R.id.fullscreen_pic);
        TextView textView = (TextView) findViewById(R.id.fullscreen_text);
        switch (state) {
            case BEFORE:
                Log.d("ejs", "processState before");
                imageView.setVisibility(View.INVISIBLE);
                textView.setVisibility(View.VISIBLE);
                textView.setText("AFTER\nPICTURE");
                state = STATE.AFTER;
                break;
            case AFTER:
                Log.d("ejs", "processState after");
                imageView.setVisibility(View.INVISIBLE);
                textView.setVisibility(View.VISIBLE);
                textView.setText("PROCESSING...");
                state = STATE.DONE;
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        createFinalPic();
                    }
                }, PIC_PROCESSING_INTERVAL);
                break;
            case DONE:
                Log.d("ejs", "processState done");
                state = STATE.BEFORE;
                break;
            default:
                break;
        }

    }

    public Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom

        int w_margin = c.getWidth() / 15;
        //int h_margin = c.getHeight() / 15;
        int h_margin = w_margin;

        Bitmap cs = null;

        int width, height = 0;

        if(c.getWidth() > s.getWidth()) {
            width = c.getWidth() + s.getWidth();
            height = c.getHeight();
        } else {
            width = s.getWidth() + s.getWidth();
            height = c.getHeight();
        }

        cs = Bitmap.createBitmap(width + (3 * w_margin), height + (2 * h_margin), Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);
        comboImage.drawColor(0xffffffff);

        comboImage.drawBitmap(c, w_margin, h_margin, null);
        comboImage.drawBitmap(s, c.getWidth() + (2 * w_margin), h_margin, null);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(w_margin * 3);

        if (c.getHeight() > s.getHeight()) {

            //merging a landcape vs a portrait
            comboImage.drawText("AFTER", c.getWidth() + w_margin * 3, w_margin * 4, paint);
            comboImage.drawText("BEFORE", w_margin * 2, c.getHeight(), paint);

        } else {

            comboImage.drawText("AFTER", c.getWidth() + w_margin * 3, c.getHeight(), paint);
            comboImage.drawText("BEFORE", w_margin * 2, w_margin * 4, paint);
        }

        return cs;
    }

    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate main resource file.
        getMenuInflater().inflate(R.menu.main, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        // Return true to display main
        return super.onCreateOptionsMenu(menu);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Log.d("ejs", "menu clicked!");
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }
     */

    private void createFinalPic() {

        Log.d("ejs", "createFinalPic 1");

        ImageView imageView = (ImageView) findViewById(R.id.fullscreen_pic);
        TextView textView = (TextView) findViewById(R.id.fullscreen_text);
        textView.setText("ALMOST DONE...");

        Bitmap beforeImage = (Bitmap) BitmapFactory.decodeFile(beforeFileUri.getEncodedPath());
        beforeImage =  Bitmap.createScaledBitmap(beforeImage, beforeImage.getWidth() / 2, beforeImage.getHeight() / 2, false);
        Bitmap afterImage = (Bitmap) BitmapFactory.decodeFile(afterFileUri.getEncodedPath());
        afterImage =  Bitmap.createScaledBitmap(afterImage, afterImage.getWidth() / 2, afterImage.getHeight() / 2, false);
        final Bitmap baaImage = combineImages(beforeImage, afterImage);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        baaImage.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

        finalFileUri = getOutputMediaFileUri();
        Log.d("ejs", "createFinalPic 2 " + finalFileUri);
        try {

            FileOutputStream fo = new FileOutputStream(finalFileUri.getEncodedPath());
            fo.write(bytes.toByteArray());

            fo.close();
            File f = new File(beforeFileUri.getEncodedPath());
            if (f.exists()) f.delete();
            f = new File(afterFileUri.getEncodedPath());
            if (f.exists()) f.delete();

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(finalFileUri);
            sendBroadcast(mediaScanIntent);

        } catch (IOException e) {
            e.printStackTrace();
        }

        int b_width = beforeImage.getWidth();
        int b_height = beforeImage.getHeight();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                Log.d("ejs", "createFinalPic 3");
                displayPic();
            }
        }, PIC_PROCESSING_INTERVAL);
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_PICTURES), "BeforeAndAfter");
        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                throw new NullPointerException("can not create dir");
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String pref;
        switch (state) {
            case BEFORE:
                pref = "before_";
                break;
            case AFTER:
                pref = "after";
                break;
            default:
                pref = "before_and_after_";
        }
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                pref + timeStamp + ".jpg");

        if (mediaFile == null)
            throw new NullPointerException("can not create file");

        return mediaFile;
    }

    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

}

