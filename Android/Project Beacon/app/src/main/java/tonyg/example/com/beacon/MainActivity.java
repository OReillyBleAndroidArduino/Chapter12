

package tonyg.example.com.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import tonyg.example.com.beacon.ble.BleBeacon;
import tonyg.example.com.beacon.ble.BleCommManager;
import tonyg.example.com.beacon.ble.callbacks.BleScanCallbackv21;
import tonyg.example.com.beacon.utilities.BeaconLocator;
import tonyg.example.com.beacon.models.BeaconMapLayout;
import tonyg.example.com.beacon.adapters.BleBeaconListAdapter;
import tonyg.example.com.beacon.ble.callbacks.BleScanCallbackv18;

/**
 * Connect to a BLE Device, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class MainActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private ArrayList<BleBeacon> mBeaconList = new ArrayList<>();
    private int mNumUsableBeacons = 0;
    private int mBeaconIndex = 0;
    private static final int MAX_BEACONS = 3;

    /** UI Stuff **/
    private MenuItem mProgressSpinner;
    private MenuItem mStartScanItem, mStopScanItem;
    private TextView mCentralPosition;
    private ListView mBeaconsList;
    private BleBeaconListAdapter mBeaconListAdapter;
    private BeaconMapLayout mBeaconMap;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScan();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void loadUI() {
        mCentralPosition = (TextView) findViewById(R.id.central_position);
        mBeaconListAdapter = new BleBeaconListAdapter(this, mBeaconList);
        mBeaconsList = (ListView) findViewById(R.id.beacons_list);
        mBeaconsList.setAdapter(mBeaconListAdapter);

        mBeaconMap = (BeaconMapLayout)findViewById(R.id.beacon_map);

    }

    /**
     * Create the menu
     *
     * @param menu
     * @return <b>true</b> if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mStartScanItem = menu.findItem(R.id.action_start_scan);
        mStopScanItem =  menu.findItem(R.id.action_stop_scan);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        initializeBluetooth();

        return true;
    }


    /**
     * User clicked a menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_scan:
                // User chose the "Scan" item
                startScan();
                return true;

            case R.id.action_stop_scan:
                // User chose the "Stop" item
                stopScan();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Turn on Bluetooth radio
     */
    public void initializeBluetooth() {
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Log.d(TAG, "Could not initialize bluetooth");
            Log.d(TAG, e.getMessage());
            finish();
        }

        // should prompt user to open settings if Bluetooth is not enabled.
        if (!mBleCommManager.getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    /**
     *  Begin scanning for Beacons
     */
    private void startScan() {

        mStartScanItem.setVisible(false);
        mStopScanItem.setVisible(true);
        mProgressSpinner.setVisible(true);
        mBeaconList.clear();
        mBeaconListAdapter.clear();

        try {
            mBleCommManager.scanForPeripherals(mScanCallbackv18, mScanCallbackv21);
        } catch (Exception e) {
            Log.d(TAG, "Can't create Ble Device Scanner");
        }

    }

    /**
     *  Stop Scanning for Beacons
     */
    public void stopScan() {
        mBleCommManager.stopScanning(mScanCallbackv18, mScanCallbackv21);
    }

    /**
     * Bluetooth scanning has stopped
     */
    public void onBleScanStopped() {
        Log.v(TAG, "Scan complete");

        // we need 3 beacons to triangulate.
        // if we don't have 3 available, that's ok
        mNumUsableBeacons = mBeaconList.size();
        if (mBeaconList.size() > MAX_BEACONS) {
            mNumUsableBeacons = MAX_BEACONS;
        }
        getBeaconData(mBeaconIndex);

    }

    /**
     * Retrieve beacon data from a beacon in a populated beacon list
     * @param index
     */
    private void getBeaconData(int index) {;
        if (index < mNumUsableBeacons) {
            BleBeacon beacon = mBeaconList.get(index);
            Log.v(TAG, "Asking beacon "+beacon.getAddress()+" for data");
            if (!beacon.isFake) {
                try {
                    beacon.connect(beacon.getBluetoothDevice(), mGattCallback);
                } catch (Exception e) {
                    Log.d(TAG, "Could not connect to beacon: " + beacon.getAddress());
                }
            }
        }
    }

    /**
     * Beacon Data has been retrieved.
     *
     * @param beacon
     */
    // done getting beacon data.  Ask the next one for data
    private void onBeaconUpdateComplete(BleBeacon beacon) {

        mStopScanItem.setVisible(false);
        mProgressSpinner.setVisible(false);
        mStartScanItem.setVisible(true);

        mBeaconListAdapter.notifyDataSetChanged();
        mBeaconMap.addBeacon(beacon);
        mBeaconMap.draw();
    }


    /**
     * Event trigger when new Peripheral is discovered
     */
    public void onBlePeripheralDiscovered(BluetoothDevice bluetoothDevice, int rssi) {
        boolean addBeacon = false;
        //mNumBeaconsIsolated = 3;
        // add first three found beacons.
        if (mBeaconList.size() < 3) {
            if (bluetoothDevice.getName() != null) {
                Log.d(TAG, "found a beacon: "+bluetoothDevice.getName());
                // only if they match myBeaconName
                if (bluetoothDevice.getName().equals(BleBeacon.BROADCAST_NAME)) {
                    addBeacon = true;
                    // Don't add if we already have this beacon in our list
                    for (BleBeacon beacon : mBeaconList) {
                        if (beacon.getAddress().equals(bluetoothDevice.getAddress())) {
                            addBeacon = false;
                        }
                    }
                }
            }
        }
        if (addBeacon) {
            BleBeacon newBeacon = new BleBeacon(getApplicationContext(), bluetoothDevice, rssi);
            mBeaconList.add(newBeacon);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBeaconListAdapter.notifyDataSetChanged();
                }
            });
        }
    }




    /**
     * Use this callback for Android API 21 (Lollipop) or greater
     */
    private final BleScanCallbackv21 mScanCallbackv21 = new BleScanCallbackv21() {
        /**
         * New Peripheral discovered
         *
         * @param callbackType int: Determines how this callback was triggered. Could be one of CALLBACK_TYPE_ALL_MATCHES, CALLBACK_TYPE_FIRST_MATCH or CALLBACK_TYPE_MATCH_LOST
         * @param result a Bluetooth Low Energy Scan Result, containing the Bluetooth Device, RSSI, and other information
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            int rssi = result.getRssi();

            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        /**
         * Several peripherals discovered when scanning in low power mode
         *
         * @param results List: List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                int rssi = result.getRssi();

                onBlePeripheralDiscovered(bluetoothDevice, rssi);
            }
        }

        /**
         * Scan failed to initialize
         *
         * @param errorCode	int: Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Fails to start scan as BLE scan with the same settings is already started by the app.");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "Fails to start scan as app cannot be registered.");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "Fails to start power optimized scan as this feature is not supported.");
                    break;
                default: // SCAN_FAILED_INTERNAL_ERROR
                    Log.e(TAG, "Fails to start scan due an internal error");

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }

        /**
         * Scan completed
         */
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });

        }
    };



    /**
     * Use this callback for Android API 18, 19, and 20 (before Lollipop)
     */
    private BleScanCallbackv18 mScanCallbackv18 = new BleScanCallbackv18() {
        /**
         *  Bluetooth LE Scan complete - timer expired out while searching for bluetooth devices
         */
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {

            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        @Override
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }
    };

    /**
     * Each Beacon in a set has the same name.  This function matches a beacon against the one
     * currently connected
     *
     * @param gatt
     * @return
     * @throws Exception
     */
    public BleBeacon getBeaconFromGatt(BluetoothGatt gatt) throws Exception {
        for (BleBeacon beacon:mBeaconList) {
            if (beacon.getGatt().equals(gatt)) {
                return beacon;
            }
        }
        throw new Exception("Matching Gatt could not be found");
    }

    /**
     * Locate the Central
     */
    public void triangulateCentral() {
        try {
            double[] centralPosition = BeaconLocator.trilaterate(mBeaconList);
            Log.d(TAG, "Central at "+centralPosition[0]+", "+centralPosition[1]);


            String centralPositionString = "";
            try {
                String xPosition = String.format("%.1f", centralPosition[0]);
                String yPosition = String.format("%.1f", centralPosition[1]);
                centralPositionString = String.format( getResources().getString(R.string.central_position), xPosition, yPosition);
            } catch (Exception e) {
                Log.d(TAG, "Could not convert central location to string");
            }
            mCentralPosition.setText(centralPositionString);

            mBeaconMap.setCentralPosition(centralPosition[0], centralPosition[1]);
            mBeaconMap.draw();
        } catch (Exception e) {
            Log.d(TAG, "Not enough Beacons to perform a triangulation.  Found " + mBeaconList.size());
        }
    }

    /**
     * BluetoothGattCallback handles connections, state changes, reads, writes, and GATT profile listings to a Peripheral
     *
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Charactersitic successfuly read
         *
         * @param gatt connection to GATT
         * @param characteristic The charactersitic that was read
         * @param status the status of the operation
         */
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleBeacon beacon;
                try {
                    beacon = getBeaconFromGatt(gatt);
                    Log.v(TAG, "beacon " + beacon.getAddress() + " responded");
                    // disconnect this beacon for others to ping

                    // read more at http://stackoverflow.com/a/5616241
                    final byte[] data = characteristic.getValue();

                    ByteBuffer byteBuffer = ByteBuffer.wrap(data); // big-endian by default
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    int value = byteBuffer.getInt();

                    // here we determine which device, which characteristic
                    if (characteristic.getUuid().equals(BleBeacon.RSSI_CHARACTERISTIC_UUID)) {
                        Log.d(TAG, "Reference RSSI of " + value + " found");
                        beacon.setReferenceRssi(value);
                        beacon.setDistance(BleBeacon.getDistanceFromRSSI(value, BleBeacon.RADIO_PROPAGATION_CONSTANT, beacon.getRssi()));
                        Log.d(TAG, "distance: " + beacon.getDistance());
                        BluetoothGattCharacteristic xCharacteristic = gatt.getService(BleBeacon.SERVICE_UUID).getCharacteristic(BleBeacon.X_CHARACTERISTIC_UUID);
                        gatt.readCharacteristic(xCharacteristic);

                    }
                    if (characteristic.getUuid().equals(BleBeacon.X_CHARACTERISTIC_UUID)) {
                        double xLocation = value / 100.0; // centimeters to meters
                        Log.d(TAG, "x Location of " + xLocation + " cm found");
                        beacon.setXLocation(xLocation);

                        BluetoothGattCharacteristic yCharacteristic = gatt.getService(BleBeacon.SERVICE_UUID).getCharacteristic(BleBeacon.Y_CHARACTERISTIC_UUID);
                        gatt.readCharacteristic(yCharacteristic);
                    }
                    if (characteristic.getUuid().equals(BleBeacon.Y_CHARACTERISTIC_UUID)) {
                        double yLocation = value / 100.0; // centimeters to meters
                        Log.d(TAG, "y Location of " + yLocation + " meters found");
                        beacon.setYLocation(yLocation);
                        beacon.disconnect();
                        final BleBeacon finalBeacon = beacon;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onBeaconUpdateComplete(finalBeacon);
                            }
                        });
                    }


                } catch (Exception e) {
                    Log.d(TAG, "Could not find matching Gatt");
                }



            }

        }

        /**
         * Characteristic was written successfully.  update the UI
         *
         * @param gatt Connection to the GATT
         * @param characteristic The Characteristic that was written
         * @param status write status
         */
        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        /**
         * Charactersitic value changed.  Read new value.
         * @param gatt Connection to the GATT
         * @param characteristic The Characterstic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        }

        /**
         * Peripheral connected or disconnected.  Update UI
         * @param bluetoothGatt Connection to GATT
         * @param status status of the operation
         * @param newState new connection state
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device");

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                try {
                    bluetoothGatt.close();
                } catch (Exception e) {
                    Log.d(TAG, "close ignoring: " + e);
                }

                Log.d(TAG, "Disconnected from device");
                mBeaconIndex++;
                Log.d(TAG, "beaconIndex: "+mBeaconIndex+"/"+mNumUsableBeacons);
                if (mBeaconIndex < mNumUsableBeacons) {
                    getBeaconData(mBeaconIndex);
                } else {
                    Log.d(TAG, "Triangulating Central...");
                    triangulateCentral();
                }

            }
        }

        /**
         * GATT Profile discovered.  Update UI
         * @param bluetoothGatt connection to GATT
         * @param status status of operation
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            // if services were discovered, We need to read them and record the values
            if (status == BluetoothGatt.GATT_SUCCESS) {

                BluetoothGattCharacteristic rssiCharacteristic = gatt.getService(BleBeacon.SERVICE_UUID).getCharacteristic(BleBeacon.RSSI_CHARACTERISTIC_UUID);
                if (!BleBeacon.isCharacteristicReadable(rssiCharacteristic)) {
                    Log.d(TAG, "rssi characteristic was not readable");
                } else {
                    Log.d(TAG, "rssi characteristic is readable");
                    gatt.readCharacteristic(rssiCharacteristic);
                }
                BluetoothGattCharacteristic xCharacteristic = gatt.getService(BleBeacon.SERVICE_UUID).getCharacteristic(BleBeacon.X_CHARACTERISTIC_UUID);
                if (!BleBeacon.isCharacteristicReadable(xCharacteristic)) {
                    Log.d(TAG, "x location characteristic was not readable");
                } else {
                    Log.d(TAG, "x location characteristic is readable");
                }
                BluetoothGattCharacteristic yCharacteristic = gatt.getService(BleBeacon.SERVICE_UUID).getCharacteristic(BleBeacon.Y_CHARACTERISTIC_UUID);
                if (!BleBeacon.isCharacteristicReadable(yCharacteristic)) {
                    Log.d(TAG, "y location characteristic was not readable");
                } else {
                    Log.d(TAG, "y location characteristic is readable");
                }
            } else {
                Log.d(TAG, "Something went wrong while discovering GATT services from this device");
            }

        }
    };


}
