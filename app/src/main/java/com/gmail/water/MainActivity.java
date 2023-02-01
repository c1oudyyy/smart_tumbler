package com.gmail.water;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = "mainActivity";



    //네비게이션바
    BottomNavigationView bottomNav;
    HomeFragment homeFragment = new HomeFragment();
    LogFragment logFragment = new LogFragment();
    //SetFragment settingFragment = new SetFragment();
    PreferenceFragmentCompat settingFragment = new SettingFragment();

    //블루투스
    public BluetoothAdapter mBluetoothAdapter;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private int m_State = UART_PROFILE_DISCONNECTED;
    SharedPreferences pref; //mac 주소 저장
    private BleService m_BleService = null;
    private BluetoothDevice m_Device = null;

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    //아두이노에서 받은 값
    public static String sensorDATA = null;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //권한 확인
        checkPermissionSDWrite();

        // 위치 권한 Get
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

       /* //파이어베이스
        FirebaseFirestore db;
        Map<String, Object> data = new HashMap<>();
        db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("ediyaData").document("68d24d30-1eda-11ed-9118-753944bd54d1");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    data.putAll(document.getData());
                    //System.out.println("칼로리: " + data.get("칼로리(kcal)"));
                    String kcal = String.valueOf(data.get("칼로리(kcal)"));
                    System.out.println("kcal:: " + kcal);

                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                } else {
                    Log.d(TAG, "No such document");
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        });*/

        //블루투스
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) { //장치가 블루투스를 지원하지 않는 경우.
            Toast.makeText(getApplicationContext(), "Bluetooth 지원을 하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //mac 주소를 저장하는 SharedPreference (key, 프리퍼런스 저장모드　[MODE_PRIVATE : 이 앱안에서 데이터 공유])
        pref = getSharedPreferences("MacAddr", Activity.MODE_PRIVATE);

        service_init();

        if (!mBluetoothAdapter.isEnabled()) { // 블루투스를 지원하지만 비활성 상태인 경우
            // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                //블루투스 권한
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_ADVERTISE,
                                    Manifest.permission.BLUETOOTH_CONNECT
                            },
                            1);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH
                            },
                            1);
                }
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //REQUEST_ENABLE_BT는 사용자 정의상수로, 블루투스 활성 상태의 변경 결과를 앱으로 알려줄 때 식별자로 사용되어 0보다 큰 수로 정의해야 한다.
        }else{ // 블루투스를 지원하며 활성 상태인 경우
            //BluetoothActivity 시작
            Intent newIntent = new Intent(MainActivity.this, com.gmail.water.BluetoothActivity.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        }

        /*//네비게이션
        bottomNav = findViewById(R.id.bottom_nav);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, homeFragment).commit();

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nev_home:
                        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, homeFragment).commit();
                        return true;
                    case R.id.nev_log:
                        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, logFragment).commit();
                        return true;
                    case R.id.nev_setting:
                        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, new SettingFragment()).commit();
                        //getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, settingFragment).commit();
                        return true;
                }
                return false;
            }
        });*/

        //네비게이션
        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction().replace(R.id.main_fragment, homeFragment).commit();

        fragmentManager.beginTransaction().add(R.id.main_fragment, logFragment).commit();
        fragmentManager.beginTransaction().hide(logFragment).commit();

        fragmentManager.beginTransaction().add(R.id.main_fragment, settingFragment).commit();
        fragmentManager.beginTransaction().hide(settingFragment).commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nev_home);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.nev_home:
                        if(homeFragment != null) fragmentManager.beginTransaction().show(homeFragment).commit();
                        if(logFragment != null) fragmentManager.beginTransaction().hide(logFragment).commit();
                        if(settingFragment != null) fragmentManager.beginTransaction().hide(settingFragment).commit();
                        return true;

                    case R.id.nev_log:
                        if(homeFragment != null) fragmentManager.beginTransaction().hide(homeFragment).commit();
                        if(logFragment != null) fragmentManager.beginTransaction().show(logFragment).commit();
                        if(settingFragment != null) fragmentManager.beginTransaction().hide(settingFragment).commit();
                        return true;

                    case R.id.nev_setting:
                        if(homeFragment != null) fragmentManager.beginTransaction().hide(homeFragment).commit();
                        if(logFragment != null) fragmentManager.beginTransaction().hide(logFragment).commit();
                        if(settingFragment != null) fragmentManager.beginTransaction().show(settingFragment).commit();
                        return true;
                }
                return false;
            }
        });

    } //oncreate()

    //권한 확인 함수
    public void checkPermissionSDWrite() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//Can add more as per requirement
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1203);
        }
    }

    //evnetBus, 데이터 fragment 이동
    public static class DataEvent {
        public final String eventBus;
        public DataEvent(String eventBus) {
            this.eventBus = eventBus;
        }
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(com.gmail.water.BleService.ACTION_GATT_CONNECTED)) { //연결성공
                runOnUiThread(() -> {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    String time_str = dateFormat.format(cal.getTime());

                    Log.d(TAG, "ACTION_GATT_CONNECTED " + time_str);


                    //연결 완료  - 맥어드레스 저장
                    //업데이트
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("MacAddr", m_Device.getAddress());
                    editor.commit();
                    m_State = UART_PROFILE_CONNECTED;
                });
            }

            //*********************//
            if (action.equals(com.gmail.water.BleService.ACTION_GATT_DISCONNECTED)) { //연결실패
                runOnUiThread(() -> {
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                    //mbtn_ConnectDisconnect.setText("Connect");
                    //medt_Message.setEnabled(false);
                    //mbtn_Send.setEnabled(false);
                    //((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
/*
                         listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ m_Device.getName());
*/
                    m_State = UART_PROFILE_DISCONNECTED;
                    m_BleService.close();
                });
            }


            //*********************//
            if (action.equals(com.gmail.water.BleService.ACTION_GATT_SERVICES_DISCOVERED)) { //GATT Service 발견
                m_BleService.enableTXNotification();
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            }
            //-----------------------------------------------------
            // HERE RECEIVE RAW BLE DATA AND PARSE 여기가 데이터 받아오는 곳
            //-----------------------------------------------------
            if (action.equals(com.gmail.water.BleService.ACTION_DATA_AVAILABLE)) { //BLE 장치에서 받은 데이터가 사용가능
                //Toast.makeText(getApplicationContext(), "연결 완료!", Toast.LENGTH_SHORT).show();
                final byte[] rData = intent.getByteArrayExtra(com.gmail.water.BleService.EXTRA_DATA);
                if(rData == null) {
                    Log.e(TAG, "데이터 없음");
                    return;
                }
                else{
                    String converted = new String(rData, StandardCharsets.UTF_8);
                    Log.d(TAG, "데이터 값: "+ converted);

                    sensorDATA = converted;
                    EventBus.getDefault().post(new DataEvent(sensorDATA));
                }

                /*runOnUiThread(() -> {
                    try {
                         *//*String text = new String(packetVenus2Phone, "UTF-8");
                         String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                         listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                         messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);*//*
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }

                    if(rData.length < m_PacketParser.def_PACKET_LENGTH){
                        Log.e(TAG, rData.toString());
                    }
                    else {
                        // receive data and parse
                        m_PacketParser.onReceiveRawPacket(rData);

                        // update sensor data to TextView
                        //UI_updateTextView();

                        // draw center of mass image
                        //UI_drawImage();

                        // save CSV file
                        //UI_saveCSVProc();
                    }
                });*/
            }
            //*********************//
            if (action.equals(com.gmail.water.BleService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                m_BleService.disconnect();
            }
        }
    };

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            m_BleService = ((com.gmail.water.BleService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected m_UartService= " + m_BleService); //여기까진 됨
            //Mainactivity log들을 일단 문제 없음 > bluetoothActivity log 확인 필요 > BleService 로그는 deviceName 클릭 후에 확인 가능
            if (!m_BleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            if (m_BleService != null) {
                //m_BleService.disconnect(m_Device);
                m_BleService.disconnect();
                //m_BleService = null;
            }
            //m_BleService = null;
        }
    };


    //서비스 연결 함수
    private void service_init() {
        Intent bindIntent = new Intent(this, com.gmail.water.BleService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        //브로드캐스트는 모든 앱이 수신할 수 있는 메시지
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
        Log.d(TAG, "service_init()");
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.gmail.water.BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(com.gmail.water.BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(com.gmail.water.BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(com.gmail.water.BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(com.gmail.water.BleService.DEVICE_DOES_NOT_SUPPORT_UART);

        //      gap messages
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        return intentFilter;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        m_BleService.stopSelf();
        m_BleService = null;

        /*if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }*/
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        /*if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }*/
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    m_Device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + m_Device + "mserviceValue" + m_BleService);
                    //((TextView) findViewById(R.id.deviceName)).setText(m_Device.getName()+ " - connecting");
                    //mbtn_ConnectDisconnect.setText("Connecting...");

                    m_BleService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }
    @Override
    public void onBackPressed() {
        if (m_State == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("warter running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Quit Water")
                    .setMessage("Do you want to quit this Application?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("NO", null)
                    .show();
        }
    }

    //txt 출력 함수
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
    }
}