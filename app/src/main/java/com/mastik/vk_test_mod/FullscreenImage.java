package com.mastik.vk_test_mod;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.mastik.vk_test_mod.databinding.ActivityFullscreenImageBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenImage extends AppCompatActivity {
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
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            /*if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars()|
                                WindowInsets.Type.navigationBars()
                );
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                );
            }*/
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
            mControlsView.startAnimation(animation);

        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    private ActivityFullscreenImageBinding binding;
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;

    float[] lastEvent = null;
    float d = 0f;
    float newRot = 0f;
    private boolean isZoomAndRotate;
    private boolean isOutSide;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    float oldDist = 1f, raw_x, raw_y;
    private float xCoOrdinate, yCoOrdinate;
    private float border_y_correction, border_x_correction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });


        mContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView) v;
                view.bringToFront();
                viewTransformation(view, event);
                return true;
            }
        });


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);
        ImageButton imageView = findViewById(R.id.dummy_button);
        ImageButton reset = findViewById(R.id.setDefault);
        ImageView fsc_cont = findViewById(R.id.fullscreenContent);
        Bitmap image = readBitmap(getIntent().getStringExtra("img"));
        System.out.println(getIntent().getStringExtra("img"));
        fsc_cont.setImageBitmap(image);
        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        border_y_correction = (float)display.heightPixels/(image.getHeight()*((float)display.widthPixels/image.getWidth()));
        border_x_correction = (float)display.widthPixels/(image.getWidth()*((float)display.heightPixels/image.getHeight()));

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContentView.animate().x(0).y(0).scaleX(1).scaleY(1).rotation(0).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        mControlsView.startAnimation(animation);

        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars()
                            //| WindowInsets.Type.navigationBars()
            );
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    private Bitmap readBitmap(String name)
    {
        Bitmap bitmap;
        try
        {
            File f = new File(getCacheDir(),name + ".png");
            FileInputStream fin = new FileInputStream(f);
            bitmap = BitmapFactory.decodeStream(fin);
            fin.close();
            return bitmap;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    float last_p_x, last_p_y;
    private void viewTransformation(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xCoOrdinate = view.getX() - event.getRawX();
                yCoOrdinate = view.getY() - event.getRawY();

                start.set(view.getX(), view.getY());
                isOutSide = false;
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    midPoint(mid, event);
                    mode = ZOOM;

                    if(isOutSide){
                        view.setPivotX(last_p_x);
                        view.setPivotY(last_p_y);

                    }else{
                        view.setPivotX((event.getX(0)+event.getX(1))/2);
                        view.setPivotY((event.getY(0)+event.getY(1))/2);
                    }
                    last_p_x = view.getPivotX();
                    last_p_y = view.getPivotY();
                }


                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
                //view.performClick();
                isZoomAndRotate = false;
                //while(view.getX() != src_x && view.getY() != src_y)
                if(view.getScaleX() < 1 || view.getScaleY() < 1)
                    view.animate().x(raw_x).y(raw_y).scaleX(1).scaleY(1).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                else {
                    float nearest_border_x = view.getWidth()*(view.getScaleX()-1);
                    float nearest_border_y = view.getHeight()*(view.getScaleY()-1)/(border_y_correction*view.getScaleY()*view.getScaleY());
                    //Toast.makeText(getApplicationContext(),nearest_border_x+" "+nearest_border_y+"\n"+view.getX()+" "+view.getY(), Toast.LENGTH_SHORT).show();
                    if(view.getX() >= nearest_border_x || view.getX() <= -nearest_border_x){
                        if(view.getY() >= nearest_border_y || view.getY() <= -nearest_border_y){
                            view.animate().x(nearest_border_x*plusMinus(view.getX())).y(nearest_border_y*plusMinus(view.getY())).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                        }else
                            view.animate().x(nearest_border_x*plusMinus(view.getX())).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                    }else{
                        if(view.getY() >= nearest_border_y || view.getY() <= -nearest_border_y)
                            view.animate().y(nearest_border_y*plusMinus(view.getY())).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                    }
                }
                if(view.getRotation() > 360)
                    view.animate().rotation(view.getRotation()-360).setDuration(300);
                if (mode == DRAG) {
                    float x = event.getX();
                    float y = view.getY();
                    if(start.y - y > 800  && view.getScaleY() <= 1.1f){
                        finish();
                    }
                }
            case MotionEvent.ACTION_OUTSIDE:
                view.setPivotX(last_p_x);
                view.setPivotY(last_p_y);
                isOutSide = true;
                mode = NONE;
                lastEvent = null;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isOutSide) {
                    if (mode == DRAG) {
                        isZoomAndRotate = false;
                        view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                        float y = view.getY();
                        if(start.y - y > 400 && view.getScaleY() <= 1.1f){
                            finish();
                        }
                    }
                    if (mode == ZOOM && event.getPointerCount() == 2) {

                        float newDist1 = spacing(event);
                        if (newDist1 > 10f) {
                            float scale = newDist1 / oldDist * view.getScaleX();
                            view.setScaleX(scale);
                            view.setScaleY(scale);
                        }
                        if (lastEvent != null) {
                            newRot = rotation(event);
                            view.setRotation((float) (view.getRotation() + (newRot - d)));
                        }
                    }
                }
                break;
        }
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (int) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    private int plusMinus(float number){
        if(number<0)
            return -1;
        else
            return 1;
    }
}