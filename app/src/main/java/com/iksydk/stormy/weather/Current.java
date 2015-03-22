package com.iksydk.stormy.weather;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Billy on 3/21/2015.
 */
public class Current
{
    private String mIcon;
    private long mTime;
    private double mTemperature;
    private double mHumidity;
    private double mPrecipitationChance;
    private String mSummary;
    private String mTimeZone;

    public String getIcon()
    {
        return mIcon;
    }

    public void setIcon(String icon)
    {
        mIcon = icon;
    }

    public int getIconId()
    {
        return Forecast.getIconId(mIcon);
    }

    public long getTime()
    {
        return mTime;
    }

    public void setTime(long time)
    {
        mTime = time;
    }

    public String getFormattedTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        formatter.setTimeZone(TimeZone.getTimeZone(getTimeZone()));

        return formatter.format(new Date(getTime() * 1000));
    }

    public int getTemperature()
    {
        return (int) Math.round(mTemperature);
    }

    public void setTemperature(double temperature)
    {
        mTemperature = temperature;
    }

    public double getHumidity()
    {
        return mHumidity;
    }

    public void setHumidity(double humidity)
    {
        mHumidity = humidity;
    }

    public int getPrecipitationChance()
    {
        return (int) Math.round(mPrecipitationChance * 100);
    }

    public void setPrecipitationChance(double precipitationChance)
    {
        mPrecipitationChance = precipitationChance;
    }

    public String getSummary()
    {
        return mSummary;
    }

    public void setSummary(String summary)
    {
        mSummary = summary;
    }

    public String getTimeZone()
    {
        return mTimeZone;
    }

    public void setTimeZone(String timeZone)
    {
        mTimeZone = timeZone;
    }
}
