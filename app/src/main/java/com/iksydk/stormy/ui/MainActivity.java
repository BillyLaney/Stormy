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
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
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


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    public static final String CURRENT_LOCATION = "CURRENT_LOCATION";

    public static final int LOCATION_UPDATES_SLOW = 1000 * 60; //60 seconds
    public static final int LOCATION_UPDATES_MINIMUM_DISTANCE = 50;//meters


    private static final int TWO_MINUTES = 1000 * 60 * 2;

    protected Location mLastLocation;

    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.refreshImageView) ImageView mRefreshImageView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;

    private Forecast mForecast;
    private AddressResultReceiver mResultReceiver;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private String mLastLocationCityState;
    private boolean mUserRequestedRefresh = true;
    private boolean mHasLatestCityState = false;
    private boolean mFirstLoad = true;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        toggleRefresh(true); //we do an update at the beginning by force so start the refresh indicator

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        mLastLocation = getBestLastKnownLocation();

        if(mLastLocation == null)
        {
            mLastLocation = new Location("MOCK");
            mLastLocation.setLongitude(-122.423);
            mLastLocation.setLatitude(37.8267);

            mLastLocationCityState = "Alcatraz Island CA, US";
            mHasLatestCityState = true;
        }


        mRefreshImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.v(TAG, "Refresh button click");
                mUserRequestedRefresh = true; //indicator that the user has requested this update, meaning we should use data for reverse geocoding the next gps
                toggleRefresh(true);
                getForecast(mLastLocation);
            }
        });

        getForecast(mLastLocation);

        Log.d(TAG, "Main UI code is running!");

        buildGoogleApiClient();

        startLocationListener();
    }

    private void startLocationListener()
    {
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener()
        {
            public void onLocationChanged(Location location)
            {
                // Called when a new location is found by the location provider.
                Log.v(TAG, "Location returned: " + location.getProvider());
                if(isBetterLocation(location, mLastLocation))
                {
                    Log.v(TAG, "Location returned is better location");

                    mLastLocation = location;
                    mHasLatestCityState = false; //reset the city state as we have a new location
                    if(mUserRequestedRefresh)
                    {
                        //only if it just happens to be that the user has requested a refresh
                        // and we have a better location now we should start the reverse geocoding service
                        //this will only happen if we get a new location between the time the user clicks, but before the api has returned.
                        //it might save the user whole milliseconds at some point
                        Log.v(TAG, "Location Received, Is Better, and User requested a refresh, SAVE THE MILLISECONDS!!");
                        startReverseGeocodeIntentService();
                    }
                }

            }

            public void onStatusChanged(String provider, int status, Bundle extras)
            {
            }

            public void onProviderEnabled(String provider)
            {
            }

            public void onProviderDisabled(String provider)
            {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        //Call twice to receive updates from both NETWORK (cell network and wifi) and GPS
        if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATES_SLOW, LOCATION_UPDATES_MINIMUM_DISTANCE, locationListener); //Cell network based location (android.permission.ACCESS_COARSE_LOCATION)
        }
        if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATES_SLOW, LOCATION_UPDATES_MINIMUM_DISTANCE, locationListener); //GPS based location (android.permission.ACCESS_FINE_LOCATION)
        }
    }

    private Location getBestLastKnownLocation()
    {
        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnownLocation == null) //GPS not available
        {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        return lastKnownLocation;
    }

    private void getForecast(Location location)
    {
        Log.v(TAG, "getForecast called");
        String apiKey = getApiKey();

        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + location.getLatitude() + "," + location.getLongitude();

        if(isNetworkAvailable())
        {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback()
            {
                @Override
                public void onFailure(Request request, IOException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            toggleRefresh(false);
                        }
                    });

                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException
                {
                    try
                    {
                        String jsonData = response.body()
                                .string();
                        Log.v(TAG, jsonData);

                        if(response.isSuccessful())
                        {
                            mForecast = parseForecastDetails(jsonData);

                            if(!mHasLatestCityState)
                            {
                                //if we get here it means that we received new location details between the last refresh
                                startReverseGeocodeIntentService();
                            }
                            else
                            {
                                mForecast.setLocation(mLastLocationCityState);
                            }
                            //We only update the display once we have a city and state to go with it.
                            //this will prevent updating the temp but not the city together
                            if(mHasLatestCityState)
                            {
                                Log.v(TAG, "We have the latest city, updating display");
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        updateDisplay();
                                    }
                                });
                            }

//                            //This is mainly for those without google play services and emulator it will at least show defaults
//                            //even if it can't get subsequent results for the address through geocoder.
//                            if(mForecast.getLocation() == null)
//                            {
//                                runOnUiThread(new Runnable()
//                                {
//                                    @Override
//                                    public void run()
//                                    {
//                                        updateDisplay();
//                                    }
//                                });
//                            }

                        }
                        else
                        {
                            alertUserAboutError();
                        }
                    }
                    catch(IOException | JSONException e)
                    {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });


        }
        else
        {
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private String getApiKey()
    {
        String apiKey;

        if(getApplicationContext()
                .getResources()
                .getIdentifier("protected_forecast_io_api_key", "string", getPackageName()) == 0)
        {
            apiKey = getString(R.string.forecast_io_api_key);
            Log.v(TAG, "Unprotected api key used");
        }
        else
        {
            apiKey = getString(getApplicationContext()
                    .getResources()
                    .getIdentifier("protected_forecast_io_api_key", "string", getPackageName()));
            Log.v(TAG, "Protected api key used");
        }

        Log.v(TAG, "ApiKey value: " + apiKey);

        return apiKey;
    }

    private void toggleRefresh(boolean turnRefreshIndicatorOn)
    {
        if(turnRefreshIndicatorOn)
        {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay()
    {
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

        toggleRefresh(false);

        if(mUserRequestedRefresh)
        {
            mUserRequestedRefresh = false; //we have completed a user requested refresh
        }
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException
    {
        Forecast forecast = new Forecast();

        forecast.setCurrent(parseCurrentDetails(jsonData));
        forecast.setHourlyForecast(parseHourlyDetails(jsonData));
        forecast.setDailyForecast(parseDailyDetails(jsonData));

        return forecast;
    }

    private Day[] parseDailyDetails(String jsonData) throws JSONException
    {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];

        for(int i = 0; i < data.length(); i++)
        {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();

            day.setSummary(jsonDay.getString("summary"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);

            days[i] = day;
        }

        return days;
    }

    private Hour[] parseHourlyDetails(String jsonData) throws JSONException
    {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for(int i = 0; i < data.length(); i++)
        {
            Hour hour = new Hour();

            JSONObject jsonHour = data.getJSONObject(i);

            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimezone(timezone);

            hours[i] = hour;
        }

        return hours;
    }

    private Current parseCurrentDetails(String jsonData) throws JSONException
    {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.v(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        Current current = new Current();

        current.setIcon(currently.getString("icon"));
        current.setTime(currently.getLong("time"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setHumidity(currently.getDouble("humidity"));
        current.setPrecipitationChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTimeZone(timezone);

        Log.d(TAG, current.getFormattedTime());

        return current;
    }

    private boolean isNetworkAvailable()
    {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;

        if(networkInfo != null && networkInfo.isConnected())
        {
            isAvailable = true;
        }

        return isAvailable;
    }

    private void alertUserAboutError()
    {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view)
    {
        Intent intent = new Intent(this, DailyForecastActivity.class);

        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        intent.putExtra(CURRENT_LOCATION, mForecast.getLocation());

        startActivity(intent);
    }

    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view)
    {
        Intent intent = new Intent(this, HourlyForecastActivity.class);

        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());

        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        if(mLastLocation != null)
        {
            // Determine whether a Geocoder is available.
            if(!Geocoder.isPresent())
            {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG)
                        .show();
                return;
            }

            startReverseGeocodeIntentService();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        int errorCode = connectionResult.getErrorCode();
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + errorCode);
        switch(errorCode)
        {
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                GooglePlayServicesUtil.getErrorDialog(errorCode, this, 1)
                        .show();
        }
    }

    public void onDisconnected()
    {
        Log.i(TAG, "Disconnected");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if(mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startReverseGeocodeIntentService()
    {
        Log.v(TAG, "Reverse geocoding requested");
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        if(mResultReceiver == null)
        {
            //declare a new results receiver
            mResultReceiver = new AddressResultReceiver(new Handler());
        }
        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation)
    {
        if(currentBestLocation == null)
        {
            // A new location is always better than no location
            return true;
        }

        if(location == null)
        {
            //we have no location so it's not better it's total crap
            return false;
        }

        // Check whether the location is the geographically the same, this is not better
        // probably only happens in debug mode anyway but WHAT! EVER!
        if(currentBestLocation.getLatitude() == location.getLatitude()
                && currentBestLocation.getLongitude() == location.getLongitude())
        {
            return false;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if(isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if(isSignificantlyOlder)
        {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if(isMoreAccurate)
        {
            return true;
        }
        else if(isNewer && !isLessAccurate)
        {
            return true;
        }
        else if(isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
        {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if(provider1 == null)
        {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    class AddressResultReceiver extends ResultReceiver
    {
        public AddressResultReceiver(Handler handler)
        {
            super(handler);
        }

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData)
        {
            Log.v(TAG, "AddressResultReceiver receive results");
            // Display the address string or an error message sent from the intent service.
            String result = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();

            // Show a toast message if an address was found.
            if(resultCode == Constants.SUCCESS_RESULT)
            {
                Log.v(TAG, "AddressResultReceiver reverse geocoded successfully");
                mLastLocationCityState = result;
                mHasLatestCityState = true; //update saying for the latest gps we have the updated the latest city state

                //this method gets called each time we get better gps, but we only want to update the forecast and UI if the user requested a new forecast
                if(mUserRequestedRefresh && mForecast != null)
                {
                    mForecast.setLocation(mLastLocationCityState);
                    Log.v(TAG, "User requested update and we received a geocode response, updating display");
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateDisplay();
                        }
                    });
                }
            }
            else
            {
                //we received some kind of error
            }
        }
    }
}

