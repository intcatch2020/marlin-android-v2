package com.intcatch.marlin2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.LoginFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class DialogSpeed extends DialogFragment {
    public static int selectedSpeed = 50;
    public static Socket mSocket;

    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        View view = inflater.inflate((R.layout.popup_speed), null);
        SeekBar seekBarSpeed = view.findViewById(R.id.sb_speed);
        final TextView textViewSpeed = view.findViewById(R.id.tv_speed);

        seekBarSpeed.setProgress(selectedSpeed);
        textViewSpeed.setText(selectedSpeed + "%");

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                textViewSpeed.setText("" + progress + "%");
                selectedSpeed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO: send the speed!
                Log.d("SocketTest", "Sending speed");
            }
        });

        builder.setView(view);
        return builder.create();
    }

    private static JSONObject createSpeedJSON(){
        JSONObject mainObject = new JSONObject();

        try {
            mainObject.put("speed", selectedSpeed);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mainObject;
    }
}
