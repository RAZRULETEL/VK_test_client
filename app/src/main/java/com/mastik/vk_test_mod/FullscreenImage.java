package com.mastik.vk_test_mod;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.mastik.vk_test_mod.dataTypes.VKImage;
import com.mastik.vk_test_mod.dataTypes.attachments.Photo;
import com.mastik.vk_test_mod.dataTypes.attachments.PhotoSize;
import com.mastik.vk_test_mod.databinding.ActivityFullscreenImageBinding;
import com.mastik.vk_test_mod.db.AppDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import timber.log.Timber;

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
    float mEventInitialRotation = 0f;
    private boolean isZoomAndRotate;
    private boolean isOutSide;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private final PointF mEventStartPosition = new PointF();
    float mEventPointersOffset = 1f;
    private float xCoordinate, yCoordinate;
    private Photo mCurrentPhoto;
    private float mImageRectangleAngle = 0;
    private int mScreenWidth, mScreenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;

        MainActivity.BACKGROUND_THREADS.execute(() -> {
            mCurrentPhoto = Photo.getFromEntity(AppDatabase.getInstance(getApplicationContext()).getPhotoDAO().getPhotoById(getIntent().getIntExtra("id", 0)));
            PhotoSize maxCacheSize = mCurrentPhoto.getCachedPaths().keySet().stream().max(Comparator.comparingInt(PhotoSize::getMaxSideSize)).get();
            renderBitmapContent(readBitmap(mCurrentPhoto.getCachedPaths().get(maxCacheSize)));
        });


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
        binding.backButton.setOnTouchListener(mDelayHideTouchListener);

        ImageButton imageView = binding.backButton;
        ImageButton reset = binding.resetTransform;

        imageView.setOnClickListener(view -> finish());
        reset.setOnClickListener(view -> {
            mContentView.setPivotX(mContentView.getWidth() / 2f);
            mContentView.setPivotY(mContentView.getHeight() / 2f);
            mContentView
                    .animate().translationX(0).translationY(0).scaleX(1).scaleY(1).rotation(0)
                    .setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
        });

        binding.settingsButton.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(getApplicationContext(), binding.settingsButton);
            menu.inflate(R.menu.fullscreen_settings);
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.sizes) {
                        AlertDialog.Builder b = new AlertDialog.Builder(FullscreenImage.this);
                        PhotoSize[] sortedSizes = Arrays.stream(mCurrentPhoto.getVKSizes())
                                .sorted((vkSize1, vkSize2) -> Integer.compare(mCurrentPhoto.getWidth(vkSize1), mCurrentPhoto.getWidth(vkSize2)))
                                .toArray(PhotoSize[]::new);
                        String[] types = Arrays.stream(sortedSizes)
                                .map(vkSize -> vkSize.name() + " - " + mCurrentPhoto.getWidth(vkSize) + " x " + mCurrentPhoto.getHeight(vkSize) + (vkSize.isCropping() ? " cropped" : "") + (mCurrentPhoto.getCachedPaths().get(vkSize) != null ? " - current" : ""))
                                .toArray(String[]::new);
                        b.setItems(types, (dialog, chosenIndex) -> {
                            if (mCurrentPhoto.getCachedPaths().get(sortedSizes[chosenIndex]) == null)
                                dialog.dismiss();
                            else
                                return;

                            String url = mCurrentPhoto.getUrl(sortedSizes[chosenIndex]);

                            MainActivity.BACKGROUND_THREADS.execute(() -> {
                                VKImage.get(url, mCurrentPhoto.getId(), getApplicationContext())
                                        .clearInitListeners()
                                        .forceInit(getApplicationContext())
                                        .addOnInitListener(image -> {
                                            renderBitmapContent(image.getImg());
                                            MainActivity.BACKGROUND_THREADS.execute(() -> mCurrentPhoto.setCachedSize(sortedSizes[chosenIndex], image.getSavePath()));
                                        });
                            });
                        });

                        b.show();

                        return true;
                    }
                    if (item.getItemId() == R.id.download) {
                        
                        return true;
                    }
                    if (item.getItemId() == R.id.copy_link){
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("VK Photo link", mCurrentPhoto.getUrl(mCurrentPhoto.getCachedPaths().keySet().stream().max(Comparator.comparingInt(PhotoSize::getMaxSideSize)).get())));
                        return true;
                    }
                    if(item.getItemId() == R.id.share){
                        Intent imageIntent = new Intent(android.content.Intent.ACTION_SEND);
//                        imageIntent.putExtra(android.content.Intent.EXTRA_TEXT, "test");
                        imageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        imageIntent.setType("image/jpeg");
                        Uri image = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", new File(getCacheDir(), mCurrentPhoto.getCachedPaths().values().stream().filter(path -> path != null).findAny().get()));
                        imageIntent.putExtra(Intent.EXTRA_STREAM, image);
                        startActivity(Intent.createChooser(imageIntent, "Send image"));
                        return true;
                    }
                    return false;
                }
            });
            menu.show();

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

    private Bitmap readBitmap(String name) {
        Bitmap bitmap;
        try {
            File f = new File(getCacheDir(), name);
            FileInputStream fin = new FileInputStream(f);
            bitmap = BitmapFactory.decodeStream(fin);
            fin.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void viewTransformation(View view, MotionEvent event) {
        Timber.v("translationX: %f, translationY: %f, pivotX: %f, pivotY: %f, eventX: %f, rawEventX: %f", view.getTranslationX(), view.getTranslationY(), view.getPivotX(), view.getPivotY(), event.getX(), event.getRawX());
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xCoordinate = view.getTranslationX() - event.getRawX();
                yCoordinate = view.getTranslationY() - event.getRawY();

                mEventStartPosition.set(view.getTranslationX(), view.getTranslationY());
                isOutSide = false;
                mode = DRAG;
                PointF mRotationCenter = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mEventPointersOffset = spacing(event);
                if (mEventPointersOffset > 10f) {
                    mode = ZOOM;
//                    PointF point = centerPoint(event);//Pivot recalculates scale and rotation and jumps, need to find rotating offset to make this works correctly
//                    view.setTranslationX(view.getTranslationX() + (view.getPivotX() - point.x) * (1 - view.getScaleX()));
//                    view.setTranslationY(view.getTranslationY() + (view.getPivotY() - point.y) * (1 - view.getScaleY()));
//                    view.setPivotX(point.x);
//                    view.setPivotY(point.y);
                }


                mEventInitialRotation = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
                isZoomAndRotate = false;
                if (view.getScaleX() < 1 || view.getScaleY() < 1)
                    view.animate().translationX(0).translationY(0).scaleX(1).scaleY(1).setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                else {
                    double currentAngle = (view.getRotation() % 360) / 180 * Math.PI;

                    double diagonal = Math.sqrt(Math.pow(view.getWidth() * view.getScaleX(), 2) + Math.pow(view.getHeight() * view.getScaleY(), 2));
                    float rotatedWidth = (float) Math.abs(diagonal * Math.cos((Math.abs(currentAngle) < Math.PI / 2 ? currentAngle : Math.PI - currentAngle) - mImageRectangleAngle));
                    float rotatedHeight = (float) Math.abs(diagonal * Math.sin((Math.abs(currentAngle) > Math.PI / 2 ? currentAngle : Math.PI - currentAngle) - mImageRectangleAngle));


                    binding.textView7.getLayoutParams().width = ((int) rotatedWidth);

                    float borderX = Math.max((rotatedWidth - mScreenWidth) / 2, 0);
                    float borderY = Math.max((rotatedHeight - mScreenHeight) / 2, 0);
                    ViewPropertyAnimator animator = view.animate();
                    if (Math.abs(view.getTranslationX()) > borderX)
                        animator.translationX(view.getTranslationX() > 0 ? borderX : -borderX);
                    if (Math.abs(view.getTranslationY()) > borderY)
                        animator.translationY(view.getTranslationY() > 0 ? borderY : -borderY);
                    animator.setDuration(300).setInterpolator(new LinearOutSlowInInterpolator()).start();
                }
                if (mode == DRAG) {
                    float y = view.getTranslationY();
                    if (mEventStartPosition.y - y > 800 && view.getScaleY() <= 1.1f) {
                        finish();
                    }
                }
            case MotionEvent.ACTION_OUTSIDE:
                isOutSide = true;
                mode = NONE;
            case MotionEvent.ACTION_POINTER_UP:
//                view.setTranslationX(view.getTranslationX() + (view.getPivotX() - view.getWidth() / 2f) * (1 - view.getScaleX()));
//                view.setTranslationY(view.getTranslationY() + (view.getPivotY() - view.getHeight() / 2f) * (1 - view.getScaleY()));
//                view.setPivotX(view.getWidth() / 2f);
//                view.setPivotY(view.getHeight() / 2f);
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isOutSide) {
                    if (mode == DRAG) {
                        isZoomAndRotate = false;
                        view.animate().translationX(event.getRawX() + xCoordinate).translationY(event.getRawY() + yCoordinate).setDuration(0).start();
                        float y = view.getTranslationY();
                        if (mEventStartPosition.y - y > 400 && view.getScaleY() <= 1.1f) {
                            finish();
                        }
                    }
                    if (mode == ZOOM && event.getPointerCount() == 2) {
                        float newOffset = spacing(event);
                        if (newOffset > 10f) {
                            float scale = newOffset / mEventPointersOffset * view.getScaleX();
                            view.setScaleX(scale);
                            view.setScaleY(scale);
                        }
                        view.setRotation((float) (view.getRotation() + (rotation(event) - mEventInitialRotation)));

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

    private PointF centerPoint(MotionEvent event) {
        PointF center = new PointF();
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        center.set(x / 2, y / 2);
        return center;
    }

    private void renderBitmapContent(Bitmap image) {
        if (image == null)
            return;
        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        binding.fullscreenContent.post(() -> {
            binding.fullscreenContent.setImageBitmap(image);
            binding.fullscreenContent.getLayoutParams().height = (int) (image.getHeight() * ((float) display.widthPixels / image.getWidth()));
        });
        mImageRectangleAngle = (float) (Math.atan2(image.getHeight(), image.getWidth()));

        mScreenWidth = display.widthPixels;
        mScreenHeight = display.heightPixels;
    }
}