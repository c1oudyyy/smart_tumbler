package com.gmail.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DrinkReceiverDialog extends DialogFragment implements View.OnClickListener{

    public static final String TAG_EVENT_DIALOG = "dialog_event";

    public DrinkReceiverDialog(){}

    public static DrinkReceiverDialog getInstance(){
        DrinkReceiverDialog drinkReceiverDialog = new DrinkReceiverDialog();
        return drinkReceiverDialog;
    }

    // DrinkReceiverDialog에 데이터를 넘겨주기 위한 interface
    public interface MyFragmentInterface{
        void onButtonClick(String input);
    }

    private MyFragmentInterface fragmentInterface;

    public void setFragmentInterface(MyFragmentInterface fragmentInterface){
        this.fragmentInterface = fragmentInterface;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.dialog, container);
        final Spinner mSpinner = (Spinner) v.findViewById(R.id.dialog_receiver);

        // Arraylist와 연동
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.drink));
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);

        Button mOkBtn = (Button) v.findViewById(R.id.dialog_positive);
        Button mCancelBtn = (Button) v.findViewById(R.id.dialog_negative);
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = mSpinner.getSelectedItem().toString();
                fragmentInterface.onButtonClick(input);
                getDialog().dismiss();
            }
        });
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        setCancelable(false);

        return v;
    }

    @Override
    public void onClick(View v){
        dismiss();
    }

    @Override
    public void onResume(){
        super.onResume();
        Window window = getDialog().getWindow();
        if(window == null) return;
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = 1000;
        params.height = 700;
        window.setAttributes(params);
    }
}
