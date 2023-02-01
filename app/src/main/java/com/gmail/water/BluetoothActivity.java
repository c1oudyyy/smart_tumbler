package com.gmail.water;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothActivity extends AppCompatActivity {
    private BluetoothAdapter m_BluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;

    private TextView m_EmptyList;
    public static final String TAG = "BluetoothActivity";

    List<BluetoothDevice> m_DeviceList;
    private DeviceAdapter m_DeviceAdapter;
    private ServiceConnection onService = null;
    Map<String, Integer> m_DevRssiValues;
    private static final long SCAN_PERIOD = 10000; //scanning for 10 seconds
    private Handler m_Handler;
    private boolean m_Scanning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        //타이틀에 특정 화면 아이디를 셋한다
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.device_list);
        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity = Gravity.TOP;
        layoutParams.y = 200;
        m_Handler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        m_BluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (m_BluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBLEScanner = m_BluetoothAdapter.getBluetoothLeScanner();
        // Checks if Bluetooth LE Scanner is available.
        if (mBLEScanner == null) {
            Toast.makeText(this, "Can not find BLE Scanner", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateList();
        m_EmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (m_Scanning == false) scanLeDevice(true);
                else finish();
            }
        });

    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        m_DeviceList = new ArrayList<BluetoothDevice>();
        m_DeviceAdapter = new DeviceAdapter(this, m_DeviceList);  //어댑터의 list에 데이터 추가
        m_DevRssiValues = new HashMap<String, Integer>();

        ListView newDevicesListView = findViewById(R.id.new_devices);
        //리스트뷰에 어댑터를 붙여준다
        newDevicesListView.setAdapter(m_DeviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        scanLeDevice(true);
    }

    //  get permission example : ACCESS_COARSE_LOCATION
    // https://stackoverflow.com/questions/38431587/error-client-must-have-access-coarse-location-or-access-fine-location
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
    }

    /**
     *
     * @brief 블루투스 디바이스를 스켄한다.
     * @details 구분자를 통해 블루투스의 장치 scan의 여부를 판단하고 블루투스 장치를 스캔하거나 스켄을 중단한다. 해당 구분자를 통해 장치 List화면에서 Button의 Text값을 변경해준다.
     * @param
     * @return
     * @throws
     */
    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        Log.d(TAG, "scanLeDevice 호출");
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            m_Handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    m_Scanning = false;
                    //-- m_BluetoothAdapter.stopLeScan(mLeScanCallback);
                    //bluetoothLeScanner.stopScan(mLeScanCallback);
                    if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    }
                    mBLEScanner.stopScan(mLeScanCallback);

                    cancelButton.setText(R.string.scan);

                }
            }, SCAN_PERIOD);

            m_Scanning = true;
            //- boolean result = m_BluetoothAdapter.startLeScan(mLeScanCallback);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            } //mBLEScanner permission
            mBLEScanner.startScan(mLeScanCallback);
            ;
            //Log.e(TAG, "Unable to initialize Bluetooth" + result);

            cancelButton.setText(R.string.cancel);
        } else {
            m_Scanning = false;
            //- m_BluetoothAdapter.stopLeScan(mLeScanCallback);
            mBLEScanner.stopScan(mLeScanCallback);
            cancelButton.setText(R.string.scan);
        }

    }

    /**
     *
     * @brief 디바이스 정보를 Setting 하는 Adapter
     * @details
     * @param
     * @return
     * @throws
     */
    //-
    private BluetoothAdapter.LeScanCallback mLeScanCallback_Old = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(device, rssi);
                }
            });
        }
    };

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        private void processResult(final ScanResult result) {
            runOnUiThread(() -> {
                addDevice(result.getDevice(), result.getRssi());

                //mLeDeviceListAdapter.addDevice(result.getDevice());
                //mLeDeviceListAdapter.notifyDataSetChanged();
            });
        }
    };

    /**
     *
     * @brief BluetoothAdapter.LeScanCallback 에서 호출되는 함수
     * @details List의 블루투스 장치들의 Mac? 주소를 통해 장치를 찾고 해당 디바이스의 정보를 함수에 저장 > Mac? 주소는 가져오는데
     * @param
     * @return
     * @throws
     */
    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;
        Log.d(TAG, "addDevice");

        for (BluetoothDevice listDev : m_DeviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        m_DevRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
            m_DeviceList.add(device);
            m_EmptyList.setVisibility(View.GONE);

            m_DeviceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        //- m_BluetoothAdapter.stopLeScan(mLeScanCallback);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        }
        mBLEScanner.stopScan(mLeScanCallback);
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //- m_BluetoothAdapter.stopLeScan(mLeScanCallback);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        }
        mBLEScanner.stopScan(mLeScanCallback);
        Log.d(TAG, "onDestroy");
    }

    /**
     *
     * @brief 블루투스 리스트에서 장비를 선택했을 시 동작하는 함수
     * @details 선택된 디바이스 정보를 Setting
     * @param
     * @return
     * @throws
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "device click!");
            BluetoothDevice device = m_DeviceList.get(position);
            //- m_BluetoothAdapter.stopLeScan(mLeScanCallback);
            checkPermission();
            mBLEScanner.stopScan(mLeScanCallback);

            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, m_DeviceList.get(position).getAddress());
            //click까진 되고 connect가 안되나..,.,.

            Intent result = new Intent();
            result.putExtras(b);
            setResult(AppCompatActivity.RESULT_OK, result);
            Log.d(TAG, "setResult!");
            finish();
        }
    };


    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    /**
     *
     * @details
     * @author Marveldex
     * @date 2017-03-17
     * @version 0.0.1
     * @li list1
     * @li list2
     *
     */

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;
            Log.d(TAG, "getView()");

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);
            final TextView tvlastdevice = (TextView) vg.findViewById(R.id.lastdevice);

            tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) m_DevRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("Rssi = " + String.valueOf(rssival));
            }

            checkPermission();
            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            //최근 연결 디바이스와 비교하여 표시
            SharedPreferences pref = getSharedPreferences("MacAddr", Activity.MODE_PRIVATE);
            String lastAddr = pref.getString("MacAddr", "00");

            if(lastAddr.equals(device.getAddress())){
                tvlastdevice.setTextColor(Color.BLUE);
                tvlastdevice.setText("Last Device");

            }else{
                tvlastdevice.setText("");
            }

            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::"+device.getName());
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.BLACK);
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLACK);

            } else {
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.BLACK);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLACK);
            }
            return vg;
        }
    }
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
