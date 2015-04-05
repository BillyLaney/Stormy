package com.iksydk.stormy.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.iksydk.stormy.R;
import com.iksydk.stormy.adapters.AutoAdapter;
import com.iksydk.stormy.weather.AutoItem;
import com.iksydk.stormy.weather.Day;
import com.iksydk.stormy.weather.Forecast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AutoWeatherFragment extends Fragment
{

    private static final String TAG = AutoWeatherFragment.class.getSimpleName();
    @InjectView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(android.R.id.list)
    ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_auto_weather,
                container, false);
        ButterKnife.inject(this, rootView);

        mSwipeRefreshLayout.setOnRefreshListener(getRefreshListener());

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
                ((MainActivity) getActivity()).getForecast();
            }
        };
    }

    public void toggleRefresh(boolean turnRefreshIndicatorOn)
    {
        if (turnRefreshIndicatorOn)
        {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        else
        {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void updateDispaly(Forecast forecast)
    {
        double maxPrecipTarget = .5;

        List<AutoItem> autoItems = new ArrayList<>();
        List<Day> daysWashWillLast = new ArrayList<>(); //which days are good in the current car wash set

        Day[] days = forecast.getDailyForecast();

        Day startDay = days[0];
        double totalPrecip = 0;

        for (int i = 0; i < days.length - 1; i++)
        {
            Day today = days[i];
            Day tomorrow = days[i + 1];

            totalPrecip += today.getPrecipitationChance();
            daysWashWillLast.add(today);

            double precipToday = today.getPrecipitationChance();
            double precipTomorrow = tomorrow.getPrecipitationChance();
            if (precipToday > maxPrecipTarget || precipTomorrow > maxPrecipTarget)
            {
                Day[] daysArray = new Day[daysWashWillLast.size()];
                for (int i2 = 0; i2 < daysArray.length; i2++)
                {
                    daysArray[i2] = daysWashWillLast.get(i2);
                }
                //this will start a new series
                autoItems.add(new AutoItem(totalPrecip / daysWashWillLast.size(), startDay, daysArray));

                daysWashWillLast.clear();
                startDay = tomorrow;
                totalPrecip = 0;
            }
        }


        AutoItem[] autoItemsArray = new AutoItem[autoItems.size()];
        for (int i = 0; i < autoItemsArray.length; i++)
        {
            autoItemsArray[i] = autoItems.get(i);
        }

        AutoAdapter adapter = new AutoAdapter(getActivity(), autoItemsArray);
        mListView.setAdapter(adapter);
    }
}
