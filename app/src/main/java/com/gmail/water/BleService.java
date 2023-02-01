package com.gmail.water;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.UUID;

public class BleService extends Service {
    private final static String TAG = "BleService";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothManager m_BluetoothManager;
    private BluetoothAdapter m_BluetoothAdapter;
    private String m_BluetoothDeviceAddress;
    private BluetoothGatt m_BluetoothGatt;
    BluetoothGattCharacteristic characteristic;
    private static int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_GATT_CONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.nordicsemi.nrfUART.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // ARDUINO_NANO_SERVICE_UUID
    public static final UUID RX_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"); // NUS : Nordic Uart Service
    public static final UUID RX_CHAR_UUID = UUID.fromString("00002a9d-0000-1000-8000-00805f9b34fb"); // RX : This phone(Peri) to BLE device(Central)
    //public static final UUID TX_CHAR_UUID = UUID.fromString("00002a98-0000-1000-8000-00805f9b34fb"); // TX : BLE device(Central) to this phone(Peri)
    public static final UUID Temp_CHAR_UUID = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb"); //received uuid

    public static boolean m_is_Disconnect_Intentional = false;

    public static boolean getIsDisconnIntentional() {
        return m_is_Disconnect_Intentional;
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //연결 상태 변경시 호출
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) { //현재 연결상태 == 연결
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }
                Log.i(TAG, "Attempting to start service discovery:" + m_BluetoothGatt.discoverServices());
                Toast.makeText(getApplicationContext(), "연결 완료!", Toast.LENGTH_SHORT).show();

                m_is_Disconnect_Intentional = false;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { //현재 연결상태 == 연결끊김
                if (m_is_Disconnect_Intentional == false) {
                    connect(m_BluetoothDeviceAddress);
                } else {
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction);
                }
            }
        }
        //BLE 장치에서 GATT 서비스들이 발견되면 호출, gatt 서비스들을 이용가능한지 status로 확인
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "m_BluetoothGatt = " + m_BluetoothGatt);

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        //서비스안의 특성을 읽었을 때 호출
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        //서비스안의 특성 값이 바뀔 때 호출
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    //인자가 action 하나 : 연결상태를 방송하거나 서비스가 발견되었을 때 상태 알림
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    //인자가 action + 특성값 : BLE 장치로부터 받아온 데이터를 BluetoothActivity로 넘겨주기 위해 이 함수를 사용한다.
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (Temp_CHAR_UUID.equals(characteristic.getUuid())) { //온도 uuid
            int flag = characteristic.getProperties();
            int format = -1;
            if((flag & 0x01) != 0){
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "format UIN16");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "format UIN8");
            }
            final byte[] dataTemp = characteristic.getValue();
            if (dataTemp!=null){
                Log.d(TAG, String.format("받아온 온도 값= " + dataTemp ));
            } else {
                Log.d(TAG, "data null");
            }

            intent.putExtra(EXTRA_DATA, dataTemp);

            // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            //intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }  else {

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BleService getService() {
            Log.d(TAG, "getService()");
            return BleService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()"); return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    //bluetooth adapter 형성/초기화, adapter는 bluetoothManager로 만들 수 있다.
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (m_BluetoothManager == null) {
            m_BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (m_BluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        m_BluetoothAdapter = m_BluetoothManager.getAdapter();
        if (m_BluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    //null검사 > 디바이스와 연결
    public boolean connect(final String address) {
        Log.d(TAG, "connect()호출");
        if (m_BluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (m_BluetoothDeviceAddress != null && address.equals(m_BluetoothDeviceAddress) && m_BluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing m_BluetoothGatt for connection.");

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }//checkPermission
            if (m_BluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = m_BluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        //m_BluetoothGatt = device.connectGatt(this, false, mGattCallback);
        m_BluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new auto connection.");
        m_BluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (m_BluetoothAdapter == null || m_BluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        m_is_Disconnect_Intentional = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        m_BluetoothGatt.disconnect();
        // m_BluetoothGatt.close();
    }

    public void close() {
        if (m_BluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "m_BluetoothGatt closed");
        m_BluetoothDeviceAddress = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        m_BluetoothGatt.close();
        m_BluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (m_BluetoothAdapter == null || m_BluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        m_BluetoothGatt.readCharacteristic(characteristic);
    }

    public void enableTXNotification() {
    	/*if (m_BluetoothGatt == null) {
    		showMessage("m_BluetoothGatt null" + m_BluetoothGatt);
    		broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
    		return;
    	}*/
        BluetoothGattService RxService = m_BluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(Temp_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        m_BluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        m_BluetoothGatt.writeDescriptor(descriptor);

    }

    public void writeRXCharacteristic(byte[] value) {
        BluetoothGattService RxService = m_BluetoothGatt.getService(RX_SERVICE_UUID);
        showMessage("m_BluetoothGatt null" + m_BluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        RxChar.setValue(value);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        boolean status = m_BluetoothGatt.writeCharacteristic(RxChar);

        Log.d(TAG, "write TXchar - status=" + status);
    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (m_BluetoothGatt == null) return null;

        return m_BluetoothGatt.getServices();
    }

}
