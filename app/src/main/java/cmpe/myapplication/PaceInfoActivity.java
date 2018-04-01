package cmpe.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PaceInfoActivity extends AppCompatActivity {

    //Bluetooth Service definitions
    private BluetoothService bluetoothService;
    private boolean bounded;
    private Handler handler;                //main handler that receives callback notifications


    //View variables
    private EditText numLapsEdit;
    private EditText lapTimeMsEdit;
    private EditText lapTimeSecEdit;
    private String numLaps;
    private String lapTimeMs;
    private String lapTimeSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pace_info);

        numLapsEdit = (EditText) findViewById(R.id.num_laps);
        lapTimeMsEdit = (EditText) findViewById(R.id.time_ms);
        lapTimeSecEdit = (EditText) findViewById(R.id.time_sec);
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
            Toast.makeText(PaceInfoActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            bounded = false;
            bluetoothService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(PaceInfoActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            bounded = true;
            BluetoothService.LocalBinder localBinder = (BluetoothService.LocalBinder)service;
            bluetoothService = localBinder.getServiceInstance();
            handler = bluetoothService.newHandler();
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

    public void startLaps(View view){
        numLaps = numLapsEdit.getText().toString();
        lapTimeMs = lapTimeMsEdit.getText().toString();
        lapTimeSec = lapTimeSecEdit.getText().toString();
        bluetoothService.sendData(numLaps, lapTimeSec, lapTimeMs);
    }
}
