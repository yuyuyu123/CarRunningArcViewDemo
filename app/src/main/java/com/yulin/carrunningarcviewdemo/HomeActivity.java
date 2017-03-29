package com.yulin.carrunningarcviewdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by YuLin on 2017/3/29 0029.
 */
public class HomeActivity extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    private Intent getIntent(Class<?> clz) {
        Intent intent = new Intent(this, clz);
        return intent;
    }

    public void carRunningArcView(View view) {
        startActivity(getIntent(CarRunningArcViewActivity.class));
    }

    public void carLoadingViewOne(View view) {
        startActivity(getIntent(CarLoadingViewOneActivity.class));
    }

    public void carLoadingViewTwo(View view) {
        startActivity(getIntent(CarLoadingViewTwoActivity.class));
    }
}
