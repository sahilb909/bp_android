package com.bp.measurement.app;

import static com.bp.measurement.app.MainActivity.Agg;
import static com.bp.measurement.app.MainActivity.Gen;
import static com.bp.measurement.app.MainActivity.Hei;
import static com.bp.measurement.app.MainActivity.Q;
import static com.bp.measurement.app.MainActivity.Wei;
import static com.bp.measurement.app.MainActivity.act_dbp;
import static com.bp.measurement.app.MainActivity.act_hr;
import static com.bp.measurement.app.MainActivity.act_sbp;
import static com.bp.measurement.app.MainActivity.hand;
import static com.bp.measurement.app.MainActivity.med;
import static com.bp.measurement.app.MainActivity.name;
import static com.bp.measurement.app.MainActivity.save;

import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.TextureView;
import android.view.View;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

class OutputAnalyzer {
    private final MainActivity activity;

    private final ChartDrawer chartDrawer;

    private MeasureStore store;

    private final int measurementInterval = 45;
    private final int measurementLength = 15000;
    private final int clipLength = 3500;

    private int detectedValleys = 0;
    private int ticksPassed = 0;

    private final CopyOnWriteArrayList<Long> valleys = new CopyOnWriteArrayList<>();

    private CountDownTimer timer;

    private final Handler mainHandler;

    OutputAnalyzer(MainActivity activity, TextureView graphTextureView, Handler mainHandler) {
        this.activity = activity;
        this.chartDrawer = new ChartDrawer(graphTextureView);
        this.mainHandler = mainHandler;
    }

    private boolean detectValley() {
        final int valleyDetectionWindowSize = 13;
        CopyOnWriteArrayList<Measurement<Integer>> subList = store.getLastStdValues(valleyDetectionWindowSize);
        if (subList.size() < valleyDetectionWindowSize) {
            return false;
        } else {
            Integer referenceValue = subList.get((int) Math.ceil(valleyDetectionWindowSize / 2f)).measurement;

            for (Measurement<Integer> measurement : subList) {
                if (measurement.measurement < referenceValue) return false;
            }

            return (!subList.get((int) Math.ceil(valleyDetectionWindowSize / 2f)).measurement.equals(
                    subList.get((int) Math.ceil(valleyDetectionWindowSize / 2f) - 1).measurement));
        }
    }

    void measurePulse(TextureView textureView, CameraService cameraService) {


        store = new MeasureStore();

        detectedValleys = 0;

        timer = new CountDownTimer(measurementLength, measurementInterval) {

            @Override
            public void onTick(long millisUntilFinished) {

                if (clipLength > (++ticksPassed * measurementInterval)) return;

                Thread thread = new Thread(() -> {
                    Bitmap currentBitmap = textureView.getBitmap();
                    int pixelCount = textureView.getWidth() * textureView.getHeight();
                    int measurement = 0;
                    int[] pixels = new int[pixelCount];

                    currentBitmap.getPixels(pixels, 0, textureView.getWidth(), 0, 0, textureView.getWidth(), textureView.getHeight());

                    for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
                        measurement += (pixels[pixelIndex] >> 16) & 0xff;
                    }


                    store.add(measurement);



                    if (detectValley()) {
                        detectedValleys = detectedValleys + 1;
                        valleys.add(store.getLastTimestamp().getTime());

                        String currentValue = String.format(
                                Locale.getDefault(),
                                activity.getResources().getQuantityString(R.plurals.measurement_output_template, detectedValleys),
                                (valleys.size() == 1)
                                        ? (60f * (detectedValleys) / (Math.max(1, (measurementLength - millisUntilFinished - clipLength) / 1000f)))
                                        : (60f * (detectedValleys - 1) / (Math.max(1, (valleys.get(valleys.size() - 1) - valleys.get(0)) / 1000f))),
                                detectedValleys,
                                1f * (measurementLength - millisUntilFinished - clipLength) / 1000f);

                        sendMessage(MainActivity.MESSAGE_UPDATE_REALTIME, currentValue);
                    }


                    Thread chartDrawerThread = new Thread(() -> chartDrawer.draw(store.getStdValues()));
                    chartDrawerThread.start();
                });
                thread.start();
            }

            @Override
            public void onFinish() {
                CopyOnWriteArrayList<Measurement<Float>> stdValues = store.getStdValues();

                if (valleys.size() == 0) {
                    mainHandler.sendMessage(Message.obtain(
                            mainHandler,
                            MainActivity.MESSAGE_CAMERA_NOT_AVAILABLE,
                            "No valleys detected - there may be an issue when accessing the camera."));
                    return;
                }

                String currentValue = String.format(
                        Locale.getDefault(),
                        activity.getResources().getQuantityString(R.plurals.measurement_output_template, detectedValleys - 1),
                        60f * (detectedValleys - 1) / (Math.max(1, (valleys.get(valleys.size() - 1) - valleys.get(0)) / 1000f)),
                        detectedValleys - 1,
                        1f * (valleys.get(valleys.size() - 1) - valleys.get(0)) / 1000f);

                double Beats = 60f * (detectedValleys - 1) / (Math.max(1, (valleys.get(valleys.size() - 1) - valleys.get(0)) / 1000f));




                sendMessage(MainActivity.MESSAGE_UPDATE_REALTIME, currentValue);

                StringBuilder returnValueSb1 = new StringBuilder();

                returnValueSb1.append(activity.getString(R.string.row_separator));
                returnValueSb1.append(activity.getString(R.string.raw_values));
                returnValueSb1.append(activity.getString(R.string.row_separator));

                for (int stdValueIdx = 0; stdValueIdx < stdValues.size(); stdValueIdx++) {

                    Measurement<Float> value = stdValues.get(stdValueIdx);
                    String timeStampString =
                            new SimpleDateFormat(
                                    activity.getString(R.string.dateFormatGranular),
                                    Locale.getDefault()
                            ).format(value.timestamp);
                    returnValueSb1.append(timeStampString);
                    returnValueSb1.append(activity.getString(R.string.separator));
                    returnValueSb1.append(value.measurement);
                    returnValueSb1.append(activity.getString(R.string.row_separator));
                }












                StringBuilder returnValueSb = new StringBuilder();
                returnValueSb.append(currentValue);
                String storeHR = currentValue;
                returnValueSb.append(" BPM");

                double ROB = 18.5;
                double ET = (364.5 - 1.23 * Beats);
                double BSA = 0.007184 * (Math.pow(Wei, 0.425)) * (Math.pow(Hei, 0.725));
                double SV = (-6.6 + (0.25 * (ET - 35)) - (0.62 * Beats) + (40.4 * BSA) - (0.51 * Agg));
                double PP = SV / ((0.013 * Wei - 0.007 * Agg - 0.004 * Beats) + 1.307);
                double MPP = Q * ROB;
                double SP = (MPP + (2 / 3 * PP));
                double DP = (MPP - PP / 3);


                save.setVisibility(View.VISIBLE);


                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String fileName = activity.getApplicationContext().getExternalFilesDir(null) + "/Records";
                        File file = new File(fileName +".csv");
                        try {

                            if (!file.exists()) {
                                file.createNewFile();
                                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                                BufferedWriter bw = new BufferedWriter(fw);
                                bw.write("Timestamp, Name, Age, Height, Weight, Medical Conditions, Hand, Gender, Est_HR, Est_SBP, Est_DBP, Act_HR, Act_SBP, Act_DBP, PPG_Rec");
                                bw.close();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            Date currentTime = Calendar.getInstance().getTime();

                            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            bw.newLine();
                            String ppg_rec = createPPGfile(returnValueSb1.toString());
                            bw.write(currentTime + ", " + name + ", " + Agg + ", " + Hei + ", " + Wei + ", " + med + ", " + hand + ", " + Gen + ", " + storeHR + ", " + SP + ", " + DP + ", " + act_hr + ", " + act_sbp + ", " + act_dbp + ", " + ppg_rec);
                            bw.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }



                    }
                });




                String TBP = Integer.toString((int)SP) + " / " + Integer.toString((int)DP);

                sendMessage(MainActivity.MESSAGE_UPDATE_FINAL, returnValueSb.toString());
                sendMessage(MainActivity.MESSAGE_UPDATE_BP, TBP);
                cameraService.stop();
            }
        };

        activity.setViewState(MainActivity.VIEW_STATE.MEASUREMENT);
        timer.start();
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    void sendMessage(int what, Object message) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = message;
        mainHandler.sendMessage(msg);
    }

    String createPPGfile(String s){
        int n = 0;
        String baseName = "/" + name + "_PPGrec_" + Integer.toString(n);
        String fileName = activity.getApplicationContext().getExternalFilesDir(null) + baseName;
        File file = new File(fileName +".csv");
        while (file.exists()){
            n += 1;
            baseName = "/" + name + "_PPGrec_" + Integer.toString(n);
            fileName = activity.getApplicationContext().getExternalFilesDir(null) + baseName;
            file = new File(fileName +".csv");
        }

        try {
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.newLine();
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return baseName;




    }


}

