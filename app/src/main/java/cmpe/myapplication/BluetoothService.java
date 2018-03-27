package cmpe.myapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

public class BluetoothService extends Service {
    IBinder binder = new LocalBinder();

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // get a handle on the bluetooth radio


    private final static int REQUEST_ENABLE_BLUETOOTH = 1; //used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; //used in Bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; //used in bluetooth handler to identify message status
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    @Override
    public IBinder onBind(Intent intent) {
       return binder;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getServiceInstance(){
            return BluetoothService.this;
        }
    }

    public void discover(View view){
        //Check if the device is already discovering
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
//            unregisterReceiver(broadcastReceiver);
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if(bluetoothAdapter.isEnabled()){
//                bluetoothArrayAdapter.clear(); //Clear items
                bluetoothAdapter.startDiscovery();
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

    public boolean bluetoothEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    //NEEDED?
//    public void bluetoothOn(View view){
//        if(!bluetoothAdapter.isEnabled()){
//            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
//            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();
//
//        } else {
//            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
//
//        }
//    }


}
