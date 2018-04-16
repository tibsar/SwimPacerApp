package cmpe.myapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothService extends Service {
    IBinder binder = new LocalBinder();

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  // get a handle on the bluetooth radio
    private BluetoothSocket bluetoothSocket = null; // bi-directional client-to-client data path
    private ConnectedThread connectedThread; // bluetooth background worker thread

    private final static int REQUEST_ENABLE_BLUETOOTH = 1; //used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; //used in Bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; //used in bluetooth handler to identify message status
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    private Handler handler;

    private final String TAG = BluetoothActivity.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {
       return binder;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getServiceInstance(){
            return BluetoothService.this;
        }
    }

    public Boolean isDiscovering(){
        return bluetoothAdapter.isDiscovering();
    }

    public void cancelDiscovery(){
        bluetoothAdapter.cancelDiscovery();
    }

    public Boolean isEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public void startDiscovery(){
        bluetoothAdapter.startDiscovery();
    }

    public Handler newHandler(){
        handler = new IncomingHandler();

        return handler;
    }

    static class IncomingHandler extends Handler {
        @Override
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
    }

    public boolean bluetoothEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public void createConnection(String info) {

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
                    connectedThread = new BluetoothService.ConnectedThread(bluetoothSocket);
                    connectedThread.start();

                    handler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                }
            }
        }.start();
    }

    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        // Creates secure outgoing connection with Bluetooth device using UUID
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);

            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }

        return device.createInsecureRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public class ConnectedThread extends Thread {
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

    public void sendData(String json){
        if(connectedThread != null) //First check to make sure thread created
            connectedThread.write(json);
    }

}
