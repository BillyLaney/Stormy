package com.iksydk.stormy.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.iksydk.stormy.Constants;
import com.iksydk.stormy.FetchAddressIntentService;
import com.iksydk.stormy.R;
import com.iksydk.stormy.adapters.SectionsPagerAdapter;
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

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int LOCATION_UPDATES_SLOW = 1000 * 60; //60 seconds
    public static final int LOCATION_UPDATES_MINIMUM_DISTANCE = 50;//meters
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    protected Location mLastLocation;
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    private Forecast mForecast;
    private AddressResultReceiver mResultReceiver;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private String mLastLocationCityState;
    private boolean mUserRequestedRefresh = true;
    private boolean mHasLatestCityState = false;
    private boolean mAlertAboutGooglePlayServices = true;
    private boolean mUseStaticLocation = false;
    private String mStaticZipcode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);


        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setLogo(R.mipmap.ic_launcher);
        actionBar.setDisplayUseLogoEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
                .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
                {
                    @Override
                    public void onPageSelected(int position)
                    {
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
        {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                            //.setIcon(mSectionsPagerAdapter.getIcon(i))
                    .setTabListener(this));
        }

        toggleRefresh(true); //we do an update at the beginning by force so start the refresh indicator

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        getForecast();

        buildGoogleApiClient();

        startLocationListener();
    }

    private void toggleRefresh(boolean refreshing)
    {
        final WeatherFragment weatherFragment = (WeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(0);
        final AutoWeatherFragment autoWeatherFragment = (AutoWeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(1);
        if (weatherFragment != null)
        {
            weatherFragment.toggleRefresh(refreshing);
        }
        if(autoWeatherFragment != null)
        {
            autoWeatherFragment.toggleRefresh(refreshing);
        }
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
                if (isBetterLocation(location, mLastLocation))
                {
                    Log.v(TAG, "Location returned is better location");

                    mLastLocation = location;
                    mHasLatestCityState = false; //reset the city state as we have a new location
                    if (mUserRequestedRefresh)
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
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATES_SLOW, LOCATION_UPDATES_MINIMUM_DISTANCE, locationListener); //Cell network based location (android.permission.ACCESS_COARSE_LOCATION)
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATES_SLOW, LOCATION_UPDATES_MINIMUM_DISTANCE, locationListener); //GPS based location (android.permission.ACCESS_FINE_LOCATION)
        }
    }

    private Location getBestLastKnownLocation()
    {
        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation == null) //GPS not available
        {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        return lastKnownLocation;
    }

    public void getForecast()
    {
        mUserRequestedRefresh = true; //indicator that the user has requested this update, meaning we should use data for reverse geocoding the next gps

        mLastLocation = getBestLastKnownLocation();

        if (mLastLocation == null)
        {
            mLastLocation = new Location("MOCK");
            mLastLocation.setLongitude(-122.423);
            mLastLocation.setLatitude(37.8267);

            mLastLocationCityState = "Alcatraz Island CA, US";
            mHasLatestCityState = true;
        }

        getForecast(mLastLocation);
    }

    private void getForecast(Location location)
    {
        Log.v(TAG, "getForecast called");
        String apiKey = getApiKey();

        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + location.getLatitude() + "," + location.getLongitude();

        if (isNetworkAvailable())
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

                        if (response.isSuccessful())
                        {
                            mForecast = parseForecastDetails(jsonData);

                            if (!mHasLatestCityState)
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
                            if (mHasLatestCityState)
                            {
                                Log.v(TAG, "We have the latest city, updating display");
                                final WeatherFragment weatherFragment = (WeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(0);
                                final AutoWeatherFragment autoWeatherFragment = (AutoWeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(1);

                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (weatherFragment != null)
                                        {
                                            weatherFragment.updateDisplay(mForecast);
                                        }
                                        else
                                        {
                                            Log.v(TAG, "Unable to find fragment to update display");
                                        }

                                        if (autoWeatherFragment != null)
                                        {
                                            autoWeatherFragment.updateDispaly(mForecast);
                                        }
                                        else
                                        {
                                            Log.v(TAG, "Unable to find auto fragment to update display");
                                        }

                                        if (mUserRequestedRefresh)
                                        {
                                            mUserRequestedRefresh = false; //we have completed a user requested refresh
                                        }

                                        toggleRefresh(false);
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
                    catch (IOException | JSONException e)
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

        if (getApplicationContext()
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case R.id.action_set_location:
                ShowSetLocationWindow();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ShowSetLocationWindow()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_get_location, null))
                // Add action buttons
                .setPositiveButton(getString(R.string.set), null)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton("Use Current", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        SetUseCurrentLocation();
                    }
                });


        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // sign in the user ...
                        EditText zipcodeEditText = (EditText)dialog.findViewById(R.id.zipcodeEditText);
                        if(zipcodeEditText.getText().length() < 5)
                        {
                            zipcodeEditText.setError("Zipcode should be 5 digits");
                        }
                        else
                        {
                            SetStaticLocation(zipcodeEditText.getText().toString());
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void SetUseCurrentLocation()
    {
        mUseStaticLocation = false;
        mUserRequestedRefresh = true;
        mHasLatestCityState = false;
        toggleRefresh(true);
        getForecast();
    }

    private void SetStaticLocation(String zipcode)
    {
        mUseStaticLocation = true;
        mStaticZipcode = zipcode;
        mUserRequestedRefresh = true;
        toggleRefresh(true);
        startReverseGeocodeIntentService();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft)
    {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft)
    {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft)
    {

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
            String[] results = TextUtils.split(result, System.getProperty("line.separator"));

            //displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT)
            {
                Log.v(TAG, "AddressResultReceiver reverse geocoded successfully");
                mLastLocationCityState = results[0];
                mHasLatestCityState = true; //update saying for the latest gps we have the updated the latest city state

                if(mUseStaticLocation)
                {
                    mLastLocation.setLongitude(Double.parseDouble(results[1]));
                    mLastLocation.setLongitude(Double.parseDouble(results[2]));
                    getForecast(mLastLocation);
                }
                else
                //this method gets called each time we get better gps, but we only want to update the forecast and UI if the user requested a new forecast
                if (mUserRequestedRefresh && mForecast != null)
                {
                    mForecast.setLocation(mLastLocationCityState);
                    Log.v(TAG, "User requested update and we received a geocode response, updating display");
                    final WeatherFragment weatherFragment = (WeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(0);
                    final AutoWeatherFragment autoWeatherFragment = (AutoWeatherFragment) mSectionsPagerAdapter.getRegisteredFragment(1);
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {

                            if (weatherFragment != null)
                            {
                                weatherFragment.updateDisplay(mForecast);
                            }
                            else
                            {
                                Log.v(TAG, "Unable to find fragment to update display");
                            }

                            if (autoWeatherFragment != null)
                            {
                                autoWeatherFragment.updateDispaly(mForecast);
                            }
                            else
                            {
                                Log.v(TAG, "Unable to find auto fragment to update display");
                            }
                            if (mUserRequestedRefresh)
                            {
                                mUserRequestedRefresh = false; //we have completed a user requested refresh
                            }
                        }
                    });

                    toggleRefresh(false);
                }
            }
            else
            {
                //we received some kind of error
            }
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

        for (int i = 0; i < data.length(); i++)
        {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();

            day.setSummary(jsonDay.getString("summary"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);
            day.setPrecipitationChance(jsonDay.getDouble("precipProbability"));

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

        for (int i = 0; i < data.length(); i++)
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

        if (networkInfo != null && networkInfo.isConnected())
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

    @Override
    public void onConnected(Bundle bundle)
    {
        if (mLastLocation != null)
        {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent())
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
        switch (errorCode)
        {
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                if (mAlertAboutGooglePlayServices)
                {
                    GooglePlayServicesUtil.getErrorDialog(errorCode, this, 1)
                            .show();
                    mAlertAboutGooglePlayServices = false;//alert only once per run
                }
        }
    }

    public void onDisconnected()
    {
        Log.i(TAG, "Disconnected");
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startReverseGeocodeIntentService()
    {
        Log.v(TAG, "Reverse geocoding requested");
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        if (mResultReceiver == null)
        {
            //declare a new results receiver
            mResultReceiver = new AddressResultReceiver(new Handler());
        }
        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        if(!mUseStaticLocation)
        {
            intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        }
        else
        {
            intent.putExtra(Constants.ZIPCODE_DATA_EXTRA, mStaticZipcode);
        }

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
        if (currentBestLocation == null)
        {
            // A new location is always better than no location
            return true;
        }

        if (location == null)
        {
            //we have no location so it's not better it's total crap
            return false;
        }

        // Check whether the location is the geographically the same, this is not better
        // probably only happens in debug mode anyway but WHAT! EVER!
        if (currentBestLocation.getLatitude() == location.getLatitude()
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
        if (isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if (isSignificantlyOlder)
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
        if (isMoreAccurate)
        {
            return true;
        }
        else if (isNewer && !isLessAccurate)
        {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
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
        if (provider1 == null)
        {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
