package com.coolweather.android.serivce;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;

import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.coolweather.android.WeatherActivity;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.internal.Util;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        //加一个定时器，实现定时刷新
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8*60*60*1000;//这是8小时的毫秒数
        Long triggerAtTime = SystemClock.elapsedRealtime()+anHour;//触发时间
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }

    private void updateWeather() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = pref.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://gulin.tech/api/weather?cityid="+weatherId+
                    "&key=762a02eb2261ac747c259cfd2408b4a1";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                  String responseText = response.body().string();
                  Weather weather = Utility.handleWeatherResponse(responseText);
                  if (weather != null && "ok".equals(weather.status)){
                      SharedPreferences.Editor editor = PreferenceManager
                              .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                      editor.putString("weather",responseText);
                      editor.apply();

                  }
                }
            });
        }
    }
    /**
     *更新必应每日一图
     */
    public void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
             String bingPic = response.body().string();
             SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
             editor.putString("bing_pic",bingPic);
             editor.apply();
            }
        });

    }
}
