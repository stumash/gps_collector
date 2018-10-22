package com.example.stumash.gpscollector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Spinner m_gpsSpinner; // so the user can choose from a list of lat long
    double[][] m_gpsLatLongList = {
        {0.0,0.0},
        {40.803291,-73.958688},
        {40.803319,-73.959197},
        {40.801999,-73.959572},
        {40.802373,-73.960232},
        {40.802300,-73.960511},
        {40.803668,-73.959857},
        {40.804223,-73.959519},
        {40.805098,-73.959406},
        {40.804794,-73.959332},
        {40.804070,-73.958548}
    };
    volatile int m_currentSpinnerSelection = 0;

    FileWriter m_fileWriter;
    String m_csv = "GPS_data.csv";

    boolean m_shouldRecordGPS = false;

    TextView m_gpsStatusTextView;
    int m_gpsWriteCounter_forTextView = 0;

    /**
     * "main method"
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // populate the spinner with gps lat long
        m_gpsSpinner = findViewById(R.id.spinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                latLongListToStringList(m_gpsLatLongList)
        );
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_gpsSpinner.setAdapter(arrayAdapter);

        // add spinner listener to respond to user selection
        m_gpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                m_currentSpinnerSelection = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { /*do nothing*/ }
        });

        m_gpsStatusTextView = findViewById(R.id.gpsStatusTextView);
        int textColor = ContextCompat.getColor(MainActivity.this, R.color.darkBlue);
        m_gpsStatusTextView.setTextColor(textColor);
        m_gpsStatusTextView.setBackgroundResource(R.color.red);

        // add button listeners to enable/disable recording gps to file
        Button startGPSButton = findViewById(R.id.startGPSbutton);
        startGPSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_gpsStatusTextView.setBackgroundResource(R.color.lightGreen);
                m_shouldRecordGPS = true;
            }
        });
        Button stopGPSButton = findViewById(R.id.stopGPSbutton);
        stopGPSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_shouldRecordGPS = false;
                m_gpsWriteCounter_forTextView = 0;
                m_gpsStatusTextView.setText("");
                m_gpsStatusTextView.setBackgroundResource(R.color.red);
            }
        });

        // get permissions to write to external storage if not already granted by user
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1010);
        }

        // open file writer
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = directory + File.separator + m_csv;
        File file = new File(path);
        try {
            m_fileWriter = new FileWriter(file, false);
        }
        catch (IOException e){
            e.printStackTrace();
            makeShortToast("file writer instantiation failed");
        }

        // write the csv header
        try {
            m_fileWriter.append("coordNumber,expectedLat,expectedLong,lat,long\n");
            m_fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            makeShortToast("write csv header failed");
        }

        // get permissions to access fine location (gps) if not already granted by user
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    10101);
        }

        // set up a listener for gps
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // do nothing if m_shouldRecordGPS is false
                if (!m_shouldRecordGPS) return;

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                double expectedLatitude = m_gpsLatLongList[m_currentSpinnerSelection][0];
                double expectedLongitude = m_gpsLatLongList[m_currentSpinnerSelection][1];

                String strToWrite = m_currentSpinnerSelection
                        +","+expectedLatitude
                        +","+expectedLongitude
                        +","+latitude
                        +","+longitude
                        +"\n";
                try {
                    m_fileWriter.append(strToWrite);
                    m_fileWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    makeShortToast( "write coord failed");
                }

                m_gpsWriteCounter_forTextView++;
                try {
                    m_gpsStatusTextView.setText(String.valueOf(m_gpsWriteCounter_forTextView));
                } catch (Exception e) {
                    makeShortToast("fuck");
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) { makeShortToast( "gps status changed"); }
            @Override
            public void onProviderEnabled(String s) {
                makeShortToast( "gps provider enabled");
            }
            @Override
            public void onProviderDisabled(String s) {
                makeShortToast( "gps provider disabled");
            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, ll);
        // lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 50, 0, ll); // TODO: SHOULD KEEP?
    }

    /**
     * Convert each pair of lat long to a String
     *
     * @param latLongList list of lat long pairs
     * @return array of strings representing list of lat long pairs
     *
     * @pre latLongList[0].length == 2 // pairs of doubles (lat and long)
     */
    private String[] latLongListToStringList(double[][] latLongList) {
        String[] latLongList_asStrings = new String[latLongList.length];
        for (int i = 0; i < latLongList_asStrings.length; i++) {
            latLongList_asStrings[i] = i+": ("+latLongList[i][0]+", "+latLongList[i][1]+")";
        }
        return latLongList_asStrings;
    }

    private void makeShortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void makeLongToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
