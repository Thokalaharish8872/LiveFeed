package com.example.livefeed;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    protected void onCreate(Bundle SavedInstances){
        super.onCreate(SavedInstances);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.drone_logo);
        logo.setAlpha(0f);

        Animation animation = AnimationUtils.loadAnimation(SplashScreen.this, android.R.anim.fade_in);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                logo.setAlpha(1f);
                logo.startAnimation(animation);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashScreen.this, SearchAndVerifyDevice.class));
                        finish();
                    }
                },2000);
            }
        },1000);
    }
}
