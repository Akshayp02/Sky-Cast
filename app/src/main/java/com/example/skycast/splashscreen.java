package com.example.skycast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.skycast.databinding.ActivitySplashscreenBinding;

public class splashscreen extends AppCompatActivity {
    ActivitySplashscreenBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivitySplashscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Thread thread=new Thread(){
            public void run(){
                try{
                    sleep(3000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    startActivity(new android.content.Intent(splashscreen.this,MainActivity.class));
                }
            }
        };thread.start();

    }
}