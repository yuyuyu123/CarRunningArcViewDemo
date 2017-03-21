package com.yulin.carrunningarcviewdemo;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private CarRunningArcView mCustomArcView;
    private float mRatio = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCustomArcView = (CarRunningArcView) findViewById(R.id.id_car_running);
        mCustomArcView.setColors(getColors());
        mCustomArcView.setRatio(1,new boolean[]{false});
    }

    private int[] getColors() {
        return new int[]{getTheColor(R.color.color1),
                getTheColor(R.color.color2),
                getTheColor(R.color.color3),
                getTheColor(R.color.color4),
                getTheColor(R.color.color5)
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

    public void setRatio(View view) {
        mRatio -= 0.3f;
        if(mRatio <= 0) {
            mRatio = 0;
        }
        Toast.makeText(this, "current ratio:" + mRatio, Toast.LENGTH_LONG).show();
        mCustomArcView.setRatio(mRatio, new boolean[]{true});
    }
}
