package com.iksydk.stormy.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iksydk.stormy.R;
import com.iksydk.stormy.adapters.DayAdapter;
import com.iksydk.stormy.weather.Day;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DailyForecastActivity extends Activity
{
    @InjectView(android.R.id.list)
    ListView mListView;
    @InjectView(android.R.id.empty)
    TextView mEmptyTextView;
    @InjectView(R.id.locationLabel)
    TextView mLocationLabel;
    private Day[] mDays;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(WeatherFragment.DAILY_FORECAST);
        mDays = Arrays.copyOf(parcelables, parcelables.length, Day[].class);

        mLocation = intent.getStringExtra(WeatherFragment.CURRENT_LOCATION);

        DayAdapter adapter = new DayAdapter(this, mDays);
        mListView.setAdapter(adapter);
        mListView.setEmptyView(mEmptyTextView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String dayOfTheWeek = mDays[position].getDayOfTheWeek();
                String conditions = mDays[position].getSummary();
                String highTemp = mDays[position].getTemperatureMax() + "";

                String message = String.format("On %s the high will be %s and it will be %s",
                        dayOfTheWeek,
                        highTemp,
                        conditions);
                Toast.makeText(DailyForecastActivity.this, message, Toast.LENGTH_LONG)
                        .show();
            }
        });

        mLocationLabel.setText(mLocation);
    }
}
