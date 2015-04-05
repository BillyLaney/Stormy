package com.iksydk.stormy.weather;

/**
 * Created by BLaney on 4/4/2015.
 */
public class AutoItem
{
    private double mAveragePrecipValue;
    private Day mDay;
    private Day[] mDailyForecast;

    public AutoItem(double precipValue, Day day, Day[] days)
    {
        mAveragePrecipValue = precipValue;
        mDay = day;
        mDailyForecast = days;
    }

    public int getAveragePrecipValue()
    {
        return (int) Math.round(mAveragePrecipValue * 100);
    }

    public int getDayCount()
    {
        return mDailyForecast.length;
    }

    public String getDayOfTheWeek()
    {
        return mDay.getDayOfTheWeek();
    }
}
