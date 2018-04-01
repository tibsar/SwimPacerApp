package cmpe.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.icu.util.Output;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BluetoothActivity extends AppCompatActivity {

    //GUI components
    private Button bluetoothDiscoverBtn;
//    private BluetoothAdapter bluetoothAdapter;
//    private BluetoothSocket bluetoothSocket = null; // bi-directional client-to-client data path
//    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private ListView devicesListView;

    private final String TAG = BluetoothActivity.class.getSimpleName();
    private Handler handler;                //main handler that receives callback notifications
//    private ConnectedThread connectedThread; // bluetooth background worker thread

    // #defines for indentifying shared types between calling functions
    private final static int REQUEST_ENABLE_BLUETOOTH = 1; //used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; //used in Bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; //used in bluetooth handler to identify message status 
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    //Bluetooth Service definitions
    private BluetoothService bluetoothService;
    private boolean bounded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        bluetoothDiscoverBtn = (Button)findViewById(R.id.discover);

        bluetoothArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // get a handle on the bluetooth radio

        devicesListView = (ListView)findViewById(R.id.devicesListView);
        devicesListView.setAdapter(bluetoothArrayAdapter);  // assign model to view
        devicesListView.setOnItemClickListener(deviceClickListener);

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

//        handler = bluetoothService.newHandler();

        if(bluetoothArrayAdapter == null){
            //Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            bluetoothDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    };

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(BluetoothActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            bounded = false;
            bluetoothService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(BluetoothActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            bounded = true;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(bounded) {
            unbindService(connection);
            bounded = false;
        }
    };

    private void discover(View view){
        bluetoothArrayAdapter.clear(); //Clear items
        //Check if the device is already discovering
        if(bluetoothService.isDiscovering()){
            bluetoothService.cancelDiscovery();
            unregisterReceiver(broadcastReceiver);
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if(bluetoothService.isEnabled()){
                bluetoothArrayAdapter.clear(); //Clear items
                bluetoothService.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth is not on", Toast.LENGTH_SHORT).show();
            }
        }


    }

    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add the name to the list
                String deviceName = device.getName();
                if(deviceName.equals("raspberrypi")){
                    bluetoothArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    bluetoothArrayAdapter.notifyDataSetChanged();
                }

            }
        }
    };


    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View view, int position, long id) {
            if(!bluetoothService.bluetoothEnabled()){
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();

            bluetoothService.createConnection(info);
        }
    };


}
