package com.iksydk.stormy.ui;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.iksydk.stormy.R;

import butterknife.ButterKnife;

public class AutoWeatherFragment extends Fragment {

    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_auto_weather,
                container, false);
        ButterKnife.inject(this, rootView);

        return rootView;
    }
}
