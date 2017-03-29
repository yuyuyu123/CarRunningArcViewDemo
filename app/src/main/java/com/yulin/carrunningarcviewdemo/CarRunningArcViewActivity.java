package com.yulin.carrunningarcviewdemo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class CarRunningArcViewActivity extends AppCompatActivity {
    private CarRunningArcView mCarRunningArcView;
    private float mRatio = 1.0f;

    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if(mCarRunningArcView.getCarState() == CarRunningArcView.CarState.ROTATED_CLOCKWISE //Finished rotating clockwise.
                         || mCarRunningArcView.getCarState() == CarRunningArcView.CarState.MOVED_ANTICLOCKWISE//Finished moving clockwise.
                            ) {
                        mRatio -= 0.1;
                        if(mRatio < 0) {
                            mRatio = 0;
                        }
                        mCarRunningArcView.setCarState(CarRunningArcView.CarState.MOVING_ANTICLOCKWISE);
                        mCarRunningArcView.setRatio(mRatio, mCarRunningArcView.getCarState());
                    }

                    if(mRatio > 0) {
                        mHandler.sendEmptyMessageDelayed(1, 1000);
                    } else {
                        mHandler.removeCallbacksAndMessages(null);
                    }
                    break;
                default:
                    break;
            }

        }
    };

    public void handler(View view) {
        mHandler.sendEmptyMessage(1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_running_arc_view);
        mCarRunningArcView = (CarRunningArcView) findViewById(R.id.id_car_running);
        mCarRunningArcView.setColors(getColors());
        mCarRunningArcView.setRatio(1, CarRunningArcView.CarState.MOVING_CLOCKWISE);


    }


    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mHandler.removeCallbacksAndMessages(null);
        outState.putFloat("ratio", mRatio);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    private int[] getColors() {
    return new int[]{getTheColor(R.color.color1),
            getTheColor(R.color.color2),
            getTheColor(R.color.color3),
            getTheColor(R.color.color4)
    };
}

    @SuppressWarnings("deprecation")
    private int getTheColor(int res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.getResources().getColor(res, null);
        } else {
            return this.getResources().getColor(res);
        }
    }

    public void ratioBack(View view) {
        if(mCarRunningArcView.getCarState() == CarRunningArcView.CarState.ROTATED_CLOCKWISE
                || mCarRunningArcView.getCarState() == CarRunningArcView.CarState.MOVED_ANTICLOCKWISE) {
            mCarRunningArcView.setCarState(CarRunningArcView.CarState.MOVING_ANTICLOCKWISE);
            mRatio -= 0.3;
            if(mRatio < 0) {
                mRatio = 0;
            }
            mCarRunningArcView.setRatio(mRatio, mCarRunningArcView.getCarState());
        } else {
            Toast.makeText(this, "Not valid state", Toast.LENGTH_SHORT).show();
        }
    }

    public void  forward(View view) {
        resetCarRunningArcView();
    }

    private void resetCarRunningArcView() {
        mHandler.removeCallbacksAndMessages(null);
        mRatio = 0.8f;
        if(mCarRunningArcView.getProgress() > 0) {
            mCarRunningArcView.setReInitialing(true);
            mCarRunningArcView.setCarState(CarRunningArcView.CarState.ROTATING_ANTICLOCKWISE);
        } else {
            mCarRunningArcView.setCarState(CarRunningArcView.CarState.MOVING_CLOCKWISE);
        }
        mCarRunningArcView.setRatio(mRatio, mCarRunningArcView.getCarState());
        mHandler.sendEmptyMessage(1);
    }

    public void reCreate(View view) {
        recreate();
    }
}
