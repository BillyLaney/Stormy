package com.iksydk.stormy.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.iksydk.stormy.Constants;
import com.iksydk.stormy.FetchAddressIntentService;
import com.iksydk.stormy.R;
import com.iksydk.stormy.weather.Current;
import com.iksydk.stormy.weather.Day;
import com.iksydk.stormy.weather.Forecast;
import com.iksydk.stormy.weather.Hour;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class WeatherFragment extends Fragment
{

    public static final String TAG = WeatherFragment.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    public static final String CURRENT_LOCATION = "CURRENT_LOCATION";

    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;

    private Forecast mForecast;


    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_weather,
                container, false);
        ButterKnife.inject(this, rootView);

        Log.d(TAG, "Main UI code is running!");

        return rootView;
    }



    public void updateDisplay(Forecast forecast)
    {
        mForecast = forecast;
        Log.v(TAG, "Updating display");
        Current current = mForecast.getCurrent();

        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(current.getHumidity() + "");
        mPrecipValue.setText(current.getPrecipitationChance() + "%");
        mSummaryLabel.setText(current.getSummary());
        mLocationLabel.setText(current.getCityState());
        mLocationLabel.setText(mForecast.getLocation());

        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }



    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view)
    {
        Intent intent = new Intent(getActivity(), DailyForecastActivity.class);

        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        intent.putExtra(CURRENT_LOCATION, mForecast.getLocation());

        startActivity(intent);
    }

    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view)
    {
        Intent intent = new Intent(getActivity(), HourlyForecastActivity.class);

        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());

        startActivity(intent);
    }




}

