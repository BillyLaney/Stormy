package com.iksydk.stormy.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.iksydk.stormy.R;
import com.iksydk.stormy.weather.Current;
import com.iksydk.stormy.weather.Forecast;

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
    @InjectView(R.id.averagePrecipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;
    @InjectView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;

    private Forecast mForecast;


    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_weather,
                container, false);
        ButterKnife.inject(this, rootView);

        mSwipeRefreshLayout.setOnRefreshListener(getRefreshListener());

        Log.d(TAG, "Main UI code is running!");

        return rootView;
    }

    private SwipeRefreshLayout.OnRefreshListener getRefreshListener()
    {
        return new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                Log.v(TAG, "Refresh swipe action");
                ((MainActivity)getActivity()).getForecast();
            }
        };
    }

    public void toggleRefresh(boolean turnRefreshIndicatorOn)
    {
        if(turnRefreshIndicatorOn)
        {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        else
        {
            mSwipeRefreshLayout.setRefreshing(false);
        }
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

