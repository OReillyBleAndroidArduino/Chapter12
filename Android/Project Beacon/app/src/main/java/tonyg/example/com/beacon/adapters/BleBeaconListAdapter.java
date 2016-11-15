package tonyg.example.com.beacon.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.util.ArrayList;

import tonyg.example.com.beacon.ble.BleBeacon;
import tonyg.example.com.beacon.R;

/**
 * Manages the BLEDeviceListItems so that we can populate the list
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-17
 */

public class BleBeaconListAdapter extends BaseAdapter {
    private static final String TAG = BleBeaconListAdapter.class.getSimpleName();

    private Context mContext;
    private ArrayList<BleBeacon> mBeaconList;

    public BleBeaconListAdapter(Context context, ArrayList<BleBeacon> beaconList) {
        this.mContext = context;
        this.mBeaconList = beaconList;
    }

    public int getCount() {
        if (mBeaconList.size()<=0)  return 1;
        return mBeaconList.size();
    }

    public void clear() {
        this.mBeaconList.clear();
    }

    public BleBeacon getItem(int position) {
        return mBeaconList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * What UI stuff is contained in this List Item
     */
    public static class ViewHolder{
        public TextView mAddress;
        public TextView mDistance;
        public TextView mRssi;
        public TextView mReferenceRssi;
        public TextView mLocation;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            // convert list_item_beacon_beacon.xml to a View
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.list_item_beacon, null);

            // match the UI stuff in the list Item to what's in the xml file
            holder = new ViewHolder();
            holder.mAddress = (TextView) v.findViewById(R.id.address);
            holder.mDistance = (TextView) v.findViewById(R.id.distance);
            holder.mRssi = (TextView) v.findViewById(R.id.rssi);
            holder.mReferenceRssi = (TextView) v.findViewById(R.id.reference_rssi);
            holder.mLocation = (TextView) v.findViewById(R.id.location);

            v.setTag( holder );
        } else {
            holder = (ViewHolder) v.getTag();
        }

        if (mBeaconList.size() <= 0) {
            holder.mAddress.setText(R.string.no_data);
        } else {
            BleBeacon item = (BleBeacon) mBeaconList.get(position);

            Resources resources = mContext.getResources();

            holder.mAddress.setText(item.getAddress());
            holder.mRssi.setText(String.format(resources.getString(R.string.rssi), item.getRssi()));


            String distance_m = "";
            try {
                distance_m = String.format(resources.getString(R.string.distance), String.format("%.1f",item.getDistance()));
            } catch (Exception e) {
                Log.d(TAG, "Could not convert distance to string");
            }
            holder.mDistance.setText(distance_m);


            String location = "";
            try {
                String xLocation = String.format("%.1f", item.getXLocation());
                String yLocation = String.format("%.1f", item.getYLocation());
                location = String.format( resources.getString(R.string.location), xLocation, yLocation);
            } catch (Exception e) {
                Log.d(TAG, "Could not convert location to string");
            }
            holder.mLocation.setText(location);


            String referenceRssi = "";
            try {
                referenceRssi = String.format(resources.getString(R.string.reference_rssi), item.getReferenceRssi());

            } catch (Exception e) {
                Log.d(TAG, "Could not convert reference rssi to string");
            }
            holder.mReferenceRssi.setText(referenceRssi);

        }
        return v;
    }
}
