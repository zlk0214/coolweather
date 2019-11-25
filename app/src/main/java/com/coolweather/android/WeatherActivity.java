package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
     private ScrollView weatherLayout;
     private TextView titleCity;
     private TextView titleUpdateTime;
     private TextView degreeText;
     private TextView weatherInfoText;
     private LinearLayout forecastLayout;
     private TextView aqiText;
     private TextView pm25Text;
     private TextView comfortText;
     private TextView carwashText;
     private TextView sportText;
     private ImageView bingpicmg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_weather);
      //初始化各组件
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView)  findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carwashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingpicmg = (ImageView) findViewById(R.id.bing_pic_img);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = pref.getString("weather",null);
        String bingPic = pref.getString("bing_pic",null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingpicmg);
        }else{
            loadBingPic();
        }
        if (weatherString != null){
            //有缓存的时候直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else{
            //无缓存的时候去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(weatherId);
        }
        if (Build.VERSION.SDK_INT >=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);}


    }

    /**
     *
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               final String bingPic = response.body().string();
               SharedPreferences.Editor editor = PreferenceManager
                       .getDefaultSharedPreferences(WeatherActivity.this).edit();
               editor.putString("bing_pic",bingPic);
               editor.apply();
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Glide.with(WeatherActivity.this).load(bingPic).into(bingpicmg);
                   }
               });
            }
        });
    }

    /**
     * 根据天气的id请求城市天气消息
     */
    private void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid="
                +weatherId+"&key=1229027861c24d9fb8dade1919dab511";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
             e.printStackTrace();
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     Toast.makeText(WeatherActivity.this,"天气信息加载失败",
                             Toast.LENGTH_SHORT).show();
                 }
             });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
              final String responseText = response.body().string();
              final Weather weather = Utility.handleWeatherResponse(responseText);
              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      if (weather != null&&"ok".equals(weather.status)){
                          SharedPreferences.Editor editor = PreferenceManager
                                  .getDefaultSharedPreferences(WeatherActivity.this).edit();
                          editor.putString("weather",responseText);
                          editor.apply();
                          showWeatherInfo(weather);
                          loadBingPic();
                      }else {
                          Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                          Toast.LENGTH_SHORT).show();
                      }
                  }
              });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        /**
         * 处理并展示Weather 类中的数据
         */
        String ctiyName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split("")[1];
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(ctiyName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList){
            View view  = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout,false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView minText  = (TextView) view.findViewById(R.id.min_text);
            TextView maxText  = (TextView) view.findViewById(R.id.max_text);
            String date = forecast.date;
            dateText.setText(date);
            Log.i("zlk",date);
            infoText.setText(forecast.more.info);
            minText.setText(forecast.temperature.min);
            maxText.setText(forecast.temperature.max);
            forecastLayout.addView(view);}
            if (weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);}
            String comfort = "舒适度"+weather.suggestion.comfort.info;
            String carwash = "洗车指数"+weather.suggestion.carwash.info;
            String sport = "运动建议"+weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carwashText.setText(carwash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);


    }
}
