package com.example.bletester;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class HTData {
    boolean flag;
    double  temp; // degree (°C)
    double  rh;   // percentage
    HTData(byte[] bytes){
        flag = bytes[7] == 16; // たまに20になることがある。問い合わせ案件？
        temp = bytes[14] + (bytes[15] / 256.0);
        rh   = bytes[16] + (bytes[17] / 256.0);
    }
}

public class MainActivity extends AppCompatActivity {
    public  static final int REQUEST_ENABLE_BT = 1;
    private static final String DEV_ADDRESS = "AC:23:3F:A1:82:8F";
    private static Locale locale = Locale.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(result.getScanRecord() == null){
                    Log.d(this.getClass().getName(), "Null Record");
                    return;
                }
                HTData ht = new HTData(result.getScanRecord().getBytes());
                if(ht.flag){
                    Log.d(this.getClass().getName(), "Data Collected");
                    int rssi = result.getRssi();
                    double dist = Math.pow(10.0, (-60.0 - rssi)/20.0); // TxPowerはデフォルト値のままだと全然ダメなので、キャリブレーションを行い、60と決めた。
                    ((TextView)findViewById(R.id.address_view)).setText(result.getDevice().getAddress());
                    ((TextView)findViewById(R.id.temp_view)).setText(String.format(locale, "%.2f°C", ht.temp));
                    ((TextView)findViewById(R.id.rh_view)).setText(String.format(locale, "%.2f%%", ht.rh));
                    ((TextView)findViewById(R.id.rssi_view)).setText(String.format(locale, "%ddBm (dist: %.1fm)", rssi, dist));
                }
                else{
                    Log.d(this.getClass().getName(), "WRONG Data");
                }
            }
        };
        final BluetoothLeScanner scanner = getScanner();

        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((TextView)findViewById(R.id.address_view)).setText(R.string.scanning);
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(DEV_ADDRESS).build();
                List<ScanFilter> filter_list = new ArrayList<>();
                filter_list.add(filter);
                ScanSettings settings = new ScanSettings.Builder().build();
                scanner.startScan(filter_list, settings, scanCallback);
            }
        });

        findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                ((TextView)findViewById(R.id.address_view)).setText("");
                ((TextView)findViewById(R.id.temp_view)).setText("");
                ((TextView)findViewById(R.id.rh_view)).setText("");
                ((TextView)findViewById(R.id.rssi_view)).setText("");
                scanner.stopScan(scanCallback);
            }
        });
    }

    public BluetoothLeScanner getScanner(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return getScanner();
        }
        return bluetoothAdapter.getBluetoothLeScanner();
    }
}
