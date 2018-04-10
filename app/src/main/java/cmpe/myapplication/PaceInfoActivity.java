package cmpe.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PaceInfoActivity extends AppCompatActivity {

    //Bluetooth Service definitions
    private BluetoothService bluetoothService;
    private boolean bounded;
    private Handler handler;                //main handler that receives callback notifications


    //View variables
    private EditText numLapsEdit;
    private String numLaps;
    private String lapTimeMs;
    private String lapTimeSec;
    private LinearLayout lapTimingLayout;
    private TextView lapTimingLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pace_info);

        numLapsEdit = (EditText) findViewById(R.id.num_laps);

        lapTimingLayout = (LinearLayout) findViewById(R.id.lap_timing_layout);

        lapTimingLabel = (TextView) findViewById(R.id.timing_laps_label);

        numLapsEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString() != ""){
                    int n = Integer.parseInt(s.toString());
                    generateTimeFields(n);
                }
            }
        });
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
//        lapTimeMs = lapTimeMsEdit.getText().toString();
//        lapTimeSec = lapTimeSecEdit.getText().toString();
        lapTimeSec = "0";
        lapTimeMs = "0";
        bluetoothService.sendData(numLaps, lapTimeSec, lapTimeMs);
    }

    private void generateTimeFields(Integer n){
        lapTimingLabel.setVisibility(View.VISIBLE);

        for(int x = 0; x <= n-1; x++){
            //Create EditText for minutes
            EditText minuteEditText = new EditText(this);
            minuteEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            minuteEditText.setHint("Minutes");

            //Create EditText for Seconds
            EditText secondEditText = new EditText(this);
            secondEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            secondEditText.setHint("Seconds");

            //Create EditText for Milliseconds
            EditText msEditText = new EditText(this);
            msEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            msEditText.setHint("Milliseconds");

            //Create label and add to view
            TextView label = new TextView(this);
            label.setText(String.format("Lap %o Timing:", x+1));
            lapTimingLayout.addView(label);

            lapTimingLayout.addView(minuteEditText);
            lapTimingLayout.addView(secondEditText);
            lapTimingLayout.addView(msEditText);


        }
    }
}
