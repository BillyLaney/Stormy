package com.iksydk.stormy.weather;

/**
 * Created by Billy on 3/21/2015.
 */
public class Hour
{
    private long mTime;
    private String mSummary;
    private double mTemperature;
    private String mIcon;

    public long getTime()
    {
        return mTime;
    }

    public void setTime(long time)
    {
        mTime = time;
    }

    public String getSummary()
    {
        return mSummary;
    }

    public void setSummary(String summary)
    {
        mSummary = summary;
    }

    public double getTemperature()
    {
        return mTemperature;
    }

    public void setTemperature(double temperature)
    {
        mTemperature = temperature;
    }

    public String getIcon()
    {
        return mIcon;
    }

    public void setIcon(String icon)
    {
        mIcon = icon;
    }

    public String getTimezone()
    {
        return mTimezone;
    }

    public void setTimezone(String timezone)
    {
        mTimezone = timezone;
    }

    private String mTimezone;
}
