package com.iksydk.stormy.weather;

/**
 * Created by BLaney on 4/4/2015.
 */
public class AutoItem {
    private double mPrecipValue;
    private Day mDay;
    private Day[] mDailyForecast;

    public AutoItem(double precipValue, Day day, Day[] days)
    {
        mPrecipValue = precipValue;
        mDay = day;
        mDailyForecast = days;
    }

    public double getPrecipValue() {
        return mPrecipValue;
    }

    public int getDayCount() {
        return mDailyForecast.length;
    }

    public String getDayOfTheWeek()
    {
        return mDay.getDayOfTheWeek();
    }
}
