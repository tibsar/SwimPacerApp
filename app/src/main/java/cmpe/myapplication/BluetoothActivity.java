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
    private BluetoothSocket bluetoothSocket = null; // bi-directional client-to-client data path
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private ListView devicesListView;

    private final String TAG = BluetoothActivity.class.getSimpleName();
    private Handler handler;                //main handler that receives callback notifications
    private ConnectedThread connectedThread; // bluetooth background worker thread

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

        handler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
//                    readBuffer.setText(readMessage);
                }

            }
        };

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
            Toast.makeText(BluetoothActivity.this, "Service is disconnected", 1000).show();
            bounded = false;
            bluetoothService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(BluetoothActivity.this, "Service is connected", 1000).show();
            bounded = true;
            BluetoothService.LocalBinder mLocalBinder = (BluetoothService.LocalBinder)service;
            bluetoothService = mLocalBinder.getServiceInstance();
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

//    private void bluetoothOn(View view){
//
////        if(!bluetoothAdapter.isEnabled()){
////            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
////            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();
////
////        } else {
////            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
////
////        }
//    }

//    private void bluetoothOff(View view){
//        bluetoothAdapter.disable(); //Turn off
//        Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_SHORT).show();
//    }

    private void discover(View view){
        bluetoothArrayAdapter.clear(); //Clear items
        bluetoothService.discover(view);
//        //Check if the device is already discovering
//        if(bluetoothAdapter.isDiscovering()){
//            bluetoothAdapter.cancelDiscovery();
////            unregisterReceiver(broadcastReceiver);
//            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
//        } else {
//            if(bluetoothAdapter.isEnabled()){
//                bluetoothArrayAdapter.clear(); //Clear items
//                bluetoothAdapter.startDiscovery();
//                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
//                registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
//            } else {
//                Toast.makeText(getApplicationContext(), "Bluetooth is not on", Toast.LENGTH_SHORT).show();
//            }
//        }


    }

//    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if(BluetoothDevice.ACTION_FOUND.equals(action)){
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                // Add the name to the list
//                String deviceName = device.getName();
//                if(deviceName.equals("raspberrypi")){
//                    bluetoothArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                    bluetoothArrayAdapter.notifyDataSetChanged();
//                }
//
//            }
//        }
//    };

    //NEEDED?
//    private void listPairedDevices(View view){
//        bluetoothArrayAdapter.clear();
//        pairedDevices = bluetoothAdapter.getBondedDevices();
//        if(bluetoothAdapter.isEnabled()){
//            // Put its one to the adapter
//            for(BluetoothDevice device : pairedDevices){
//                String deviceName = device.getName();
//                if(deviceName.equals("raspberrypi")){
//                    bluetoothArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                }
//
//            }
//
//            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
//        }
//    }

    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View view, int position, long id) {
            if(!bluetoothService.bluetoothEnabled()){
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread(){
                public void run(){
                    boolean fail = false;

                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                    try {
                        bluetoothSocket = createBluetoothSocket(device);
                    } catch(IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }

                    // Establish the Bluetooth socket connection
                    try {
                        bluetoothSocket.connect();
                    } catch(IOException e){
                        try {
//                            // fallback method for android >= 4.2
                            bluetoothSocket = (BluetoothSocket)device.getClass().getMethod("createRfcommSocket", new Class[]{Integer.TYPE}).invoke(device, new Object[]{Integer.valueOf(1)});

                            try {
                                bluetoothSocket.connect();
                            } catch (IOException e2){
                                try{
                                    fail = true;
                                    bluetoothSocket.close();
                                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                                } catch (IOException e3){
                                    //@Todo Insert code to handle this
                                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                                }
                            }

                        } catch (NoSuchMethodException e2){
                            //@Todo Insert code to handle this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        } catch (IllegalAccessException e2){
                            //@Todo Insert code to handle this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        } catch (InvocationTargetException e2){
                            //@Todo Insert code to handle this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }


                    }

                    if(fail == false){
                        connectedThread = new ConnectedThread(bluetoothSocket);
                        connectedThread.start();

                        handler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        // Creates secure outgoing connection with Bluetooth device using UUID
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);

            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }

        return device.createInsecureRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket threadSocket;
        private InputStream threadInStream;
        private OutputStream threadOutStream;

        public ConnectedThread(BluetoothSocket socket){
            cancel(); //Cancel connection before trying to connect

            threadSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects
            // because member streams are final

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e){
                //@Todo handle this exception
            }

            threadInStream = tmpIn;
            threadOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024]; //buffer store for the stream
            int bytes; //bytes returned from read()

            //Keep listening to the InputStream until an exception occurs
            while(true){
                try{
                    //Read from the InputStream
                    bytes = threadInStream.available();
                    if(bytes != 0){
                        buffer = new byte[1024];

                        // Pause and wait for the rest of the data
                        SystemClock.sleep(100); // can be adjusted based on system speed
                        bytes = threadInStream.available(); // How many bytes are ready to be read?
                        bytes = threadInStream.read(buffer, 0, bytes); // Record how many bytes were read
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch(IOException e){
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Call this from the activity to send data to the remote device
        public void write(String input){
            byte[] bytes = input.getBytes();    //Converts entered string into bytes

            try {
                threadOutStream.write(bytes);
            } catch(IOException e){
                //@TODO handle exception
            }
        }

        // Call this from the activity to shutdown the connection
        public void cancel(){

            if (threadInStream != null) {
                try {threadInStream.close();} catch (Exception e) {}
                threadInStream = null;
            }

            if (threadOutStream != null) {
                try {threadOutStream.close();} catch (Exception e) {}
                threadOutStream = null;
            }

            if (threadSocket != null) {
                try {threadSocket.close();} catch (Exception e) {}
                threadSocket = null;
            }



        }
    }

}
