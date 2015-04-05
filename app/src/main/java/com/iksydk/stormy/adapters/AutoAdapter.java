package com.iksydk.stormy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iksydk.stormy.R;
import com.iksydk.stormy.weather.AutoItem;

/**
 * Created by Billy on 3/21/2015.
 */
public class AutoAdapter extends BaseAdapter
{
    private Context mContext;
    private AutoItem[] mItems;

    public AutoAdapter(Context context, AutoItem[] items)
    {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount()
    {
        return mItems.length;
    }

    @Override
    public Object getItem(int position)
    {
        return mItems[position];
    }

    @Override
    public long getItemId(int position)
    {
        return 0; //use for tagging items, don't need
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;

        if (convertView == null)
        {
            //brand new, create everything
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.auto_list_item, null);

            holder = new ViewHolder();

            holder.dayCountLabel = (TextView) convertView.findViewById(R.id.dayCountLabel);
            holder.dayNameLabel = (TextView) convertView.findViewById(R.id.dayNameLabel);
            holder.averagePrecipValue = (TextView) convertView.findViewById(R.id.averagePrecipValue);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        AutoItem item = mItems[position];

        holder.averagePrecipValue.setText(item.getAveragePrecipValue() + "%");
        holder.dayCountLabel.setText(item.getDayCount() + "");

        if (position == 0)
        {
            holder.dayNameLabel.setText("Today");
        }
        else
        {
            holder.dayNameLabel.setText(item.getDayOfTheWeek());
        }

        return convertView;
    }

    private static class ViewHolder
    {
        TextView dayCountLabel;
        TextView averagePrecipValue;
        TextView dayNameLabel;
    }
}
