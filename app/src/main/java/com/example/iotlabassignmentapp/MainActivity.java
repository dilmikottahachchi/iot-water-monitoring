package com.example.iotlabassignmentapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<String[]> sensorDataLines = new ArrayList<>();
    private int currentLineIdx = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sensorRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        readCSVData();
    }

    protected void readCSVData() {
        // (the same parsing logic but instead of logging, add to sensorDataLines)
        InputStream is = getResources().openRawResource(R.raw.sensor_reading);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        boolean sensorDataSection = false;
        try {
            while ((line = reader.readLine()) != null) {
                if (!sensorDataSection) {
                    if (line.startsWith("TIMESTAMP,DN,D_Water")) {
                        sensorDataSection = true;
                    }
                } else {
                    String[] tokens = line.split(",");
                    // (Optional) Ignore lines not expected length:
                    if(tokens.length < 14) continue;
                    sensorDataLines.add(tokens);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { reader.close(); is.close(); } catch (IOException e) { e.printStackTrace(); }
        }
        // Start sensor simulation
        startFakeSensor();
    }

    private void startFakeSensor() {
        sensorRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentLineIdx < sensorDataLines.size()) {
                    String[] tokens = sensorDataLines.get(currentLineIdx);
                    showSensorReading(tokens);
                    currentLineIdx++;
                    handler.postDelayed(this, 1500); // next "reading" in 1.5 seconds
                }
            }
        };
        handler.post(sensorRunnable);
    }

    private void showSensorReading(String[] tokens) {
        // Find correct columns: TIMESTAMP,DN,D_Water,TW,EC,SCOND,pH,ORP,TURBF,CHLF,O2SAT,O2,BGAPC,FDOM
        String timestamp = tokens[0];
        String ph = tokens[6];
        String turbidity = tokens[8];
        String o2 = tokens[11]; // O2 in mg/L

        // update UI
        runOnUiThread(() -> {
            ((TextView)findViewById(R.id.textPh)).setText(ph);
            ((TextView)findViewById(R.id.textConductivity)).setText(tokens[4] + " μS/cm");
            ((TextView)findViewById(R.id.textTemp)).setText(tokens[3] + " ℃");
            ((TextView)findViewById(R.id.textOxygen)).setText(o2 + " mg/L");
            // Add alert if values out of range
            checkThresholdAndAlert(ph, turbidity, o2);
        });
    }

    private void checkThresholdAndAlert(String phStr, String turbidityStr, String o2Str) {
        try {
            double ph = Double.parseDouble(phStr);
            double turbidity = Double.parseDouble(turbidityStr);
            double o2 = Double.parseDouble(o2Str);

            if(ph < 6.5 || ph > 8.5) {
                Toast.makeText(this, "Warning: pH out of range!", Toast.LENGTH_SHORT).show();
            }
            if(turbidity > 5) {
                Toast.makeText(this, "Warning: Turbidity high!", Toast.LENGTH_SHORT).show();
            }
            if(o2 < 5) {
                Toast.makeText(this, "Warning: Dissolved Oxygen is low!", Toast.LENGTH_SHORT).show();
            }
        } catch(NumberFormatException e) {
            // Handle NaN or missing data
        }
    }
}