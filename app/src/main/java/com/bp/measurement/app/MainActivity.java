package com.bp.measurement.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;



import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private OutputAnalyzer analyzer;

    private final int REQUEST_CODE_CAMERA = 0;
    public static final int MESSAGE_UPDATE_REALTIME = 1;
    public static final int MESSAGE_UPDATE_FINAL = 2;
    public static final int MESSAGE_CAMERA_NOT_AVAILABLE = 3;
    public static final int MESSAGE_UPDATE_BP = 4;

    private static final int MENU_INDEX_NEW_MEASUREMENT = 0;
    private static final int MENU_INDEX_EXPORT_RESULT = 1;
    private static final int MENU_INDEX_EXPORT_DETAILS = 2;

    public static String name, med, hand, Gen, act_hr, act_sbp, act_dbp;
    public static double  Agg, Hei, Wei;
    public static double Q = 4.5;
    private static double SP = 0, DP = 0;

    public static Button save;

    public enum VIEW_STATE {
        MEASUREMENT,
        SHOW_RESULTS
    }

    private boolean justShared = false;

    @SuppressLint("HandlerLeak")
    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {



            super.handleMessage(msg);
            save = findViewById(R.id.save);
            if (msg.what ==  MESSAGE_UPDATE_REALTIME) {
                ((TextView) findViewById(R.id.textView)).setText("F");
            }

            if (msg.what == MESSAGE_UPDATE_FINAL) {
                ((TextView) findViewById(R.id.editText)).setText(msg.obj.toString());


                Menu appMenu = ((Toolbar) findViewById(R.id.toolbar)).getMenu();

                setViewState(VIEW_STATE.SHOW_RESULTS);
            }

            if (msg.what == MESSAGE_UPDATE_BP) {
                ((TextView) findViewById(R.id.bpView)).setText(msg.obj.toString());

                Menu appMenu = ((Toolbar) findViewById(R.id.toolbar)).getMenu();

                setViewState(VIEW_STATE.SHOW_RESULTS);
            }



            if (msg.what == MESSAGE_CAMERA_NOT_AVAILABLE) {
                Log.println(Log.WARN, "camera", msg.obj.toString());

                ((TextView) findViewById(R.id.textView)).setText(
                        R.string.camera_not_found
                );
                analyzer.stop();
            }
        }
    };

    private final CameraService cameraService = new CameraService(this, mainHandler);

    @Override
    protected void onResume() {
        super.onResume();

        analyzer = new OutputAnalyzer(this, findViewById(R.id.graphTextureView), mainHandler);

        TextureView cameraTextureView = findViewById(R.id.textureView2);
        SurfaceTexture previewSurfaceTexture = cameraTextureView.getSurfaceTexture();

        if ((previewSurfaceTexture != null) && !justShared) {

            Surface previewSurface = new Surface(previewSurfaceTexture);


            if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Snackbar.make(
                        findViewById(R.id.constraintLayout),
                        getString(R.string.noFlashWarning),
                        Snackbar.LENGTH_LONG
                ).show();
            }


            ((Toolbar) findViewById(R.id.toolbar)).getMenu().getItem(MENU_INDEX_NEW_MEASUREMENT).setVisible(false);

            cameraService.start(previewSurface);
            analyzer.measurePulse(cameraTextureView, cameraService);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraService.stop();
        if (analyzer != null) analyzer.stop();
        analyzer = new OutputAnalyzer(this, findViewById(R.id.graphTextureView), mainHandler);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        Bundle extras = getIntent().getExtras();
        name = extras.getString("name");
        Hei = Double.valueOf(extras.getString("height"));
        Wei = Double.valueOf(extras.getString("weight"));
        Agg = Double.valueOf(extras.getString("age"));
        med = extras.getString("med");
        hand = extras.getString("hand");
        Gen = extras.getString("gender");
        act_hr = extras.getString("act_hr");
        act_sbp = extras.getString("act_sbp");
        act_dbp = extras.getString("act_dbp");

        if (Gen == "Male") {
            Q = 5;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Snackbar.make(
                        findViewById(R.id.constraintLayout),
                        getString(R.string.cameraPermissionRequired),
                        Snackbar.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.i("MENU", "menu is being prepared");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return super.onPrepareOptionsMenu(menu);
    }

    public void setViewState(VIEW_STATE state) {
        Menu appMenu = ((Toolbar) findViewById(R.id.toolbar)).getMenu();
        switch (state) {
            case MEASUREMENT:
                appMenu.getItem(MENU_INDEX_NEW_MEASUREMENT).setVisible(false);
                appMenu.getItem(MENU_INDEX_EXPORT_RESULT).setVisible(false);
                appMenu.getItem(MENU_INDEX_EXPORT_DETAILS).setVisible(false);
                findViewById(R.id.floatingActionButton).setVisibility(View.INVISIBLE);
                break;
            case SHOW_RESULTS:
                findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
                appMenu.getItem(MENU_INDEX_EXPORT_RESULT).setVisible(true);
                appMenu.getItem(MENU_INDEX_EXPORT_DETAILS).setVisible(true);
                appMenu.getItem(MENU_INDEX_NEW_MEASUREMENT).setVisible(true);
                break;
        }
    }

    public void onClickNewMeasurement(MenuItem item) {
        onClickNewMeasurement();
    }

    public void onClickNewMeasurement(View view) {
        onClickNewMeasurement();
    }

    public void onClickNewMeasurement() {

        save.setVisibility(View.INVISIBLE);


        analyzer = new OutputAnalyzer(this, findViewById(R.id.graphTextureView), mainHandler);


        char[] empty = new char[0];
        ((TextView) findViewById(R.id.editText)).setText(empty, 0, 0);
        ((TextView) findViewById(R.id.textView)).setText(empty, 0, 0);


        setViewState(VIEW_STATE.MEASUREMENT);

        TextureView cameraTextureView = findViewById(R.id.textureView2);
        SurfaceTexture previewSurfaceTexture = cameraTextureView.getSurfaceTexture();

        if (previewSurfaceTexture != null) {

            Surface previewSurface = new Surface(previewSurfaceTexture);
            cameraService.start(previewSurface);
            analyzer.measurePulse(cameraTextureView, cameraService);
        }
    }

    public void onClickExportResult(MenuItem item) {
        final Intent intent = getTextIntent((String) ((TextView) findViewById(R.id.textView)).getText());
        justShared = true;
        startActivity(Intent.createChooser(intent, getString(R.string.send_output_to)));
    }

    public void onClickExportDetails(MenuItem item) {
        final Intent intent = getTextIntent(((TextView) findViewById(R.id.editText)).getText().toString());
        justShared = true;
        startActivity(Intent.createChooser(intent, getString(R.string.send_output_to)));
    }

    private Intent getTextIntent(String intentText) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(
                Intent.EXTRA_SUBJECT,
                String.format(
                        getString(R.string.output_header_template),
                        new SimpleDateFormat(
                                getString(R.string.dateFormat),
                                Locale.getDefault()
                        ).format(new Date())
                ));
        intent.putExtra(Intent.EXTRA_TEXT, intentText);
        return intent;
    }
}
