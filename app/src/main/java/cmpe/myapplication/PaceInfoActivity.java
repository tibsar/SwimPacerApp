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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

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
    private Button submitLapsBtn;
    private EditText delayEdit;

    private ArrayList<ArrayList<EditText>> editTextList = new ArrayList<ArrayList<EditText>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pace_info);

        numLapsEdit = (EditText) findViewById(R.id.num_laps);

        lapTimingLayout = (LinearLayout) findViewById(R.id.lap_timing_layout);

        lapTimingLabel = (TextView) findViewById(R.id.timing_laps_label);

        submitLapsBtn = (Button) findViewById(R.id.start_laps_btn);

        delayEdit = (EditText) findViewById(R.id.delay_edit);

        numLapsEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0){
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
        JSONObject json = new JSONObject();
        String delay;

        ArrayList<Float> timingArray = new ArrayList<Float>();
        numLaps = numLapsEdit.getText().toString();
        delay = delayEdit.getText().toString();

        for(ArrayList<EditText> editArray : editTextList ){
            Float time;
            Integer min = Integer.parseInt(editArray.get(0).getText().toString());
            Integer sec = Integer.parseInt(editArray.get(1).getText().toString());
            Integer ms = Integer.parseInt(editArray.get(2).getText().toString());

            time = convertToAlgorithmTime(min, sec, ms);

            timingArray.add(time);
        }

        try{
            json.put("delay", delay);
            json.put("lap_times", timingArray);
        } catch (JSONException e){
            e.printStackTrace();
        }

        bluetoothService.sendData(json.toString());
    }

    private Float convertToAlgorithmTime(Integer minutes, Integer seconds, Integer milliseconds){
        return ((float) minutes*60) + (float) seconds + ((float) milliseconds/1000);
    }

    private void generateTimeFields(Integer n){


        lapTimingLabel.setVisibility(View.VISIBLE);
        submitLapsBtn.setVisibility(View.VISIBLE);

        for(int x = 0; x <= n-1; x++){
            ArrayList<EditText> timingEdits = new ArrayList<EditText>();

            //Create EditText for minutes
            EditText minuteEditText = new EditText(this);
            minuteEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            minuteEditText.setHint("Minutes");
            minuteEditText.setTag(String.format("lap_%o_min_edit", x+1));
            timingEdits.add(minuteEditText);

            //Create EditText for Seconds
            EditText secondEditText = new EditText(this);
            secondEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            secondEditText.setHint("Seconds");
            minuteEditText.setTag(String.format("lap_%o_sec_edit", x+1));
            timingEdits.add(secondEditText);

            //Create EditText for Milliseconds
            EditText msEditText = new EditText(this);
            msEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            msEditText.setHint("Milliseconds");
            minuteEditText.setTag(String.format("lap_%o_ms_edit", x+1));
            timingEdits.add(msEditText);

            //Add EditTexts to array
            editTextList.add(timingEdits);

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
