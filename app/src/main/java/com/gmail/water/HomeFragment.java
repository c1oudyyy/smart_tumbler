package com.gmail.water;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {
    View v;
    public static final String TAG = "HomeFragment";
    public String rData;
    public String temData;
    public String weiData;

    TextView current_state; //현재음료 용량 txt
    TextView current_Drink; //현재음료 종류 txt
    FloatingActionButton fab; //온도버튼

    EditText total;

    TextView tvDrink;
    public static String drink_name;

    /*Map<String, Double> coffe_map = new HashMap<String, Double>() {{
        put("나트륨(mg)", 0.0);
        put("단백질(g)", 0.3);
        put("당류(g)", 0.0);
        put("카페인(mg)", 42.3);
        put("칼로리(kcal)", 2.8);
        put("포화지방(g)", 1.4);
    }};*/

    public HomeFragment(){

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_home, container, false);

        super.onCreate(savedInstanceState);

        //이미지 애니메이션
        LottieAnimationView animationView = v.findViewById(R.id.lottieview);
        animationView.playAnimation();

        //목표설정
        total = v.findViewById(R.id.total);

        //eventBus
        try {
            EventBus.getDefault().register(this);
        }catch (Exception e){}

        //현재 음료량
        current_state = v.findViewById(R.id.current_state);
        //현재음료
        current_Drink = v.findViewById(R.id.current_Drink);

        // 음료 선택
        final String[] drinkInput = new String[1];
        final Button[] mAddBtn = {(Button) v.findViewById(R.id.btn_dialog)};
        tvDrink = (TextView)v.findViewById(R.id.current_Drink);

        mAddBtn[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DrinkReceiverDialog mDrinkReceiverDialog = DrinkReceiverDialog.getInstance();
                mDrinkReceiverDialog.setFragmentInterface(new DrinkReceiverDialog.MyFragmentInterface() {
                    @Override
                    public void onButtonClick(String input) {
                        tvDrink.setText(input);
                        drink_name = input;
                    }
                });
                mDrinkReceiverDialog.show(getFragmentManager(), DrinkReceiverDialog.TAG_EVENT_DIALOG);
            }

        });

        current_Drink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drink_name.equals("커피")){
                    Snackbar.make(v,"아메리카노(100ml 당)\n칼로리 0kcal 당류 0g\n나트륨 0mg 단백질 0g\n포화지방 0g 카페인 0mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name.equals("콜라")){
                    Snackbar.make(v,"콜라(100ml 당)\n칼로리 42kcal 당류 11g\n나트륨 0mg 단백질 0g\n포화지방 0g 카페인 10mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name.equals("물")){
                    Snackbar.make(v,"물(100ml 당)\n칼로리 0kcal 당류 0g\n나트륨 0mg 단백질 0g\n포화지방 0g 카페인 0mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name.equals("에너지 음료")){
                    Snackbar.make(v,"에너지 음료(100ml 당)\n칼로리 46kcal 당류 11.6g\n나트륨 30mg 단백질 0g\n포화지방 0g 카페인 24mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name.equals("오렌지 주스")){
                    Snackbar.make(v,"오렌지 주스(100ml 당)\n칼로리 31kcal 당류 8g\n나트륨 17mg 단백질 0g\n포화지방 0g 카페인 0mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name.equals("이온음료")){
                    Snackbar.make(v,"이온음료(100ml 당)\n칼로리 10.8kcal 당류 2.7g\n나트륨 53.1mg 단백질 0g\n포화지방 0g 카페인 0mg",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                } else if(drink_name == null){
                    Snackbar.make(v,"음료를 선택해주세요!",Snackbar.LENGTH_LONG).setTextMaxLines(5).show();
                }

            }
        });

        //온도버튼
        fab = v.findViewById(R.id.temperature_btn);


        return v;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void callEventBus(MainActivity.DataEvent event){
        Log.d(TAG, event.eventBus);

        rData = event.eventBus;

        String[] splitData = rData.split(",");
        temData = splitData[0];
        weiData = splitData[1];

        Log.d(TAG, "온도: " + temData + " 무게: " + weiData);

        Double weiDouble = Double.parseDouble(weiData);
        //int weight = (int) (weiDouble+0.01)*1000) ;
        current_state.setText(weiDouble+"L"); //무게 text
        fab.setOnClickListener(new View.OnClickListener() { //온도 btn
            @Override
            public void onClick(View view) {
                Snackbar.make(view, temData+"℃", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.temperature_btn)
                        .setAction("Action", null)
                        .show();
            }
        });
    }

}