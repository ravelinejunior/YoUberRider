package br.com.youberrider.model;

import com.firebase.geofire.GeoQuery;

public class AnimationModel {

    private boolean isRun;
    private GeoQueryModel geoQuery;

    public AnimationModel() {
    }

    public AnimationModel(boolean isRun, GeoQueryModel geoQuery) {
        this.isRun = isRun;
        this.geoQuery = geoQuery;
    }

    public boolean isRun() {
        return isRun;
    }

    public void setRun(boolean run) {
        isRun = run;
    }

    public GeoQueryModel getGeoQuery() {
        return geoQuery;
    }

    public void setGeoQuery(GeoQueryModel geoQuery) {
        this.geoQuery = geoQuery;
    }
}
