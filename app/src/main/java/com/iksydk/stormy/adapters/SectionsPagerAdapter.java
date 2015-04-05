package com.iksydk.stormy.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.iksydk.stormy.R;
import com.iksydk.stormy.ui.AutoWeatherFragment;
import com.iksydk.stormy.ui.WeatherFragment;

import java.util.Locale;

/**
 * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter
{
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
    protected Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm)
    {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position)
    {
        // getItem is called to instantiate the fragment for the given page.
        // Return a DummySectionFragment (defined as a static inner class
        // below) with the page number as its lone argument.
        switch (position)
        {
            case 0:
                return new WeatherFragment();
            case 1:
                return new AutoWeatherFragment();
        }

        return null;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position)
    {
        return registeredFragments.get(position);
    }

    @Override
    public int getCount()
    {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        Locale l = Locale.getDefault();
        switch (position)
        {
            case 0:
                return mContext.getString(R.string.title_section1)
                        .toUpperCase(l);
            case 1:
                return mContext.getString(R.string.title_section2)
                        .toUpperCase(l);
        }
        return null;
    }

//    public int getIcon(int position)
//    {
//        switch(position)
//        {
//            case 0:
//                return R.mipmap.ic_tab_inbox;
//            case 1:
//                return R.mipmap.ic_tab_friends;
//        }
//        return R.mipmap.ic_tab_inbox;
//    }
}