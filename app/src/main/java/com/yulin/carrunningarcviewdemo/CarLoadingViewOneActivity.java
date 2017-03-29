package com.yulin.carrunningarcviewdemo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by YuLin on 2017/3/28 0028.
 */

public class CarLoadingViewOneActivity extends AppCompatActivity {

    private CarLoadingViewOne mCarLoadingView;
    private double ratio = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if(ratio <= 1) {
                        ratio += 0.01;
                        mCarLoadingView.setRatio(ratio);
                        mHandler.sendEmptyMessageDelayed(1, 150);
                    }
                    break;
            }
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_car_loading_view_one);

        mCarLoadingView = (CarLoadingViewOne) findViewById(R.id.id_car_loading);
        mCarLoadingView.setColors(getColors());
        mHandler.sendEmptyMessage(1);
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
    }
