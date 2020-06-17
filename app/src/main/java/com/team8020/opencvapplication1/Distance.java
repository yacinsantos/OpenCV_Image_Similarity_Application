package com.team8020.opencvapplication1;


import android.os.Parcel;
import android.os.Parcelable;

public class Distance {

    private Cell pic;
    private Double distance;

    public Distance(Cell pic, Double distance) {
        this.pic = pic;
        this.distance = distance;
    }

    public Cell getPic() {
        return pic;
    }

    public Double getDistance() {
        return distance;
    }

    public void setPic(Cell pic) {
        this.pic = pic;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

}
