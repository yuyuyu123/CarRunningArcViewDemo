package com.yulin.carrunningarcviewdemo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by YuLin on 2017/3/28 0028.
 */
public class CarLoadingViewTwoActivity extends AppCompatActivity{

    private CarLoadingViewTwo mCarAnotherLoadingView;

    private double mRatio = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if(mRatio <= 1) {
                        mRatio += 0.01;
                        mCarAnotherLoadingView.setRatio(mRatio);
                        mHandler.sendEmptyMessageDelayed(1, 300);
                    }
                    break;
                case 2:
                    txtContinue.setText("续订中.");
                    mHandler.sendEmptyMessageDelayed(3,500);
                    break;
                case 3:
                    txtContinue.setText("续订中..");
                    mHandler.sendEmptyMessageDelayed(4,500);
                    break;
                case 4:
                    txtContinue.setText("续订中...");
                    mHandler.sendEmptyMessageDelayed(2,500);
                    break;
            }
        }
    };

    TextView txtContinue;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_loading_view_two);
        mCarAnotherLoadingView = (CarLoadingViewTwo) findViewById(R.id.id_car_another_loading);
        mCarAnotherLoadingView.setColors(getColors());
        mHandler.sendEmptyMessage(1);
        txtContinue = (TextView) findViewById(R.id.id_continue_tip);

    }

    private int[] getColors () {
        return new int[]{getTheColor(R.color.color1),
                getTheColor(R.color.color2),
                getTheColor(R.color.color3),
                getTheColor(R.color.color4)
        };
    }

    @SuppressWarnings("deprecation")
    private int getTheColor ( int res){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.getResources().getColor(res, null);
        } else {
            return this.getResources().getColor(res);
        }
    }

    public void ratioDown(View view) {
        txtContinue.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessage(2);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRatio = 0.2;
                mHandler.sendEmptyMessage(1);
                txtContinue.setText("续订成功");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        txtContinue.setText("续订中...");
                        txtContinue.setVisibility(View.GONE);
                    }
                },1000);
            }
        },5000);
    }
}
