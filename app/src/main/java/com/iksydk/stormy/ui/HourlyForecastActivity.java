package com.iksydk.stormy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.iksydk.stormy.R;
import com.iksydk.stormy.adapters.HourAdapter;
import com.iksydk.stormy.weather.Hour;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class HourlyForecastActivity extends ActionBarActivity
{
    @InjectView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    private Hour[] mHours;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hourly_forecast);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(WeatherFragment.HOURLY_FORECAST);

        mHours = Arrays.copyOf(parcelables, parcelables.length, Hour[].class);

        HourAdapter adapter = new HourAdapter(this, mHours);
        mRecyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);
    }


}