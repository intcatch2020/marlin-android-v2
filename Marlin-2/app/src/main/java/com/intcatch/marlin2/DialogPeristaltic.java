package com.intcatch.marlin2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class DialogPeristaltic extends DialogFragment {

    public static boolean pump_active = false;
    private static int mode = 0;

    public static Socket mSocket;

    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        View view = inflater.inflate((R.layout.popup_peristaltic), null);
        final Switch activeSwitch = view.findViewById(R.id.peristaltic_switch);
        Spinner spinner = view.findViewById(R.id.peristaltic_spinner);
        final TextView text = view.findViewById(R.id.peristaltic_time);

        activeSwitch.setChecked(pump_active);
        spinner.setSelection(mode);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        text.setText("Necessary time: 30m 00s");
                        if(mode != 0) activeSwitch.setChecked(false);
                        mode = 0;
                        break;
                    case 1:
                        text.setText("Necessary time: 4m 30s");
                        if(mode != 1) activeSwitch.setChecked(false);
                        mode = 1;
                        break;
                    case 2:
                        text.setText("Necessary time: infinity");
                        if(mode != 2) activeSwitch.setChecked(false);
                        mode = 2;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        activeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                double necessaryTime;
                switch (mode){
                    case 0:
                        necessaryTime = 1800; //Normal mode (30m)
                        break;
                    case 1:
                        necessaryTime = 270; //Fill mode
                        break;
                    case 2:
                        necessaryTime = 0; //Continuous
                        break;
                    default:
                        necessaryTime = -1;
                }

                // TODO: send the pump mode!
                JSONObject toSend = createPumpJSON(isChecked, necessaryTime);
                Log.d("SocketTest", "Sending pump stuff");
                mSocket.emit("set_pump", toSend);
            }
        });

        builder.setView(view);
        return builder.create();
    }

    private static JSONObject createPumpJSON(boolean pumpOn, double pumpTime){
        JSONObject mainObject = new JSONObject();

        try {
            mainObject.put("pump_time", pumpTime); //Time in seconds
            mainObject.put("pump_on", pumpOn); //True or False
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mainObject;
    }

}
