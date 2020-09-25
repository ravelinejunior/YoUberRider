package br.com.youberrider.model;

import com.firebase.geofire.GeoLocation;

public class DriverGeo {
    private String key;
    private GeoLocation geoLocation;
    private DriverInfo driverInfo;

    public DriverGeo() {
    }

    public DriverGeo(String key, GeoLocation geoLocation) {
        this.key = key;
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public DriverInfo getDriverInfo() {
        return driverInfo;
    }

    public void setDriverInfo(DriverInfo driverInfo) {
        this.driverInfo = driverInfo;
    }
}
