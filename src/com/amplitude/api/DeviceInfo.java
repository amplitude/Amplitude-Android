package com.amplitude.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;

public class DeviceInfo {

    private Context context;
    
    private int versionCode;
    private String versionName;
    private int buildVersionSdk;
    private String buildVersionRelease;
    private String phoneBrand;
    private String phoneManufacturer;
    private String phoneModel;
    private String phoneCarrier;
    private String geocodeCountry;
    private String networkCountry;
    private String localeCountry;
    private String language;

    public DeviceInfo(Context context) {
        this.context = context;

        // Get version information
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
        }
        buildVersionSdk = Build.VERSION.SDK_INT;
        buildVersionRelease = Build.VERSION.RELEASE;
        
        // Device information
        phoneBrand = Build.BRAND;
        phoneManufacturer = Build.MANUFACTURER;
        phoneModel = Build.MODEL;
        
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        // Carrier
        phoneCarrier = manager.getNetworkOperatorName();
        
        // Country
        new AsyncGeocoder().execute();
        if (manager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            networkCountry = manager.getNetworkCountryIso();
            if (networkCountry != null) {
                networkCountry = networkCountry.toUpperCase(Locale.US);
            }
        }
        localeCountry = Locale.getDefault().getCountry();
        
        // Language
        language = Locale.getDefault().getLanguage();
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getBuildVersionSdk() {
        return buildVersionSdk;
    }

    public String getBuildVersionRelease() {
        return buildVersionRelease;
    }

    public String getPhoneBrand() {
        return phoneBrand;
    }

    public String getPhoneManufacturer() {
        return phoneManufacturer;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public String getPhoneCarrier() {
        return phoneCarrier;
    }

    public String getCountry() {
        //   Prioritize reverse geocode, but until we have a result from that,
        //   we try to grab the country from the network, and finally the locale
        if (geocodeCountry != null) {
            return geocodeCountry;
        }
        if (networkCountry != null) {
            return networkCountry;
        }
        return localeCountry;
    }

    public String getLanguage() {
        return language;
    }

    public Location getMostRecentLocation() {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        List<Location> locations = new ArrayList<Location>();
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                locations.add(location);
            }
        }

        long maximumTimestamp = -1;
        Location bestLocation = null;
        for (Location location : locations) {
            if (location.getTime() > maximumTimestamp) {
                maximumTimestamp = location.getTime();
                bestLocation = location;
            }
        }

        return bestLocation;
    }
    
    class AsyncGeocoder extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Location recent = getMostRecentLocation();
            if (recent != null) {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(
                            recent.getLatitude(), recent.getLongitude(), 1);
                    if (addresses != null) {
                        for (Address address : addresses) {
                            if (address != null) {
                                return address.getCountryCode();
                            }
                        }
                    }
                } catch (IOException e) {
                    // Failed to reverse geocode location
                }
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                geocodeCountry = result.toUpperCase(Locale.US);
            }
        }
        
    }
    
}
