package com.example.android.drivesafe;

class DrivingData {
    private String type;
    private float xAverage;
    private float yAverage;
    private float zAverage;
    private float speed;
    private long time;
    double lastLatitude;
    double lastLongitude;
    private String date;

    DrivingData(String mType,
                float mXAverage,
                float mYAverage,
                float mZAverage,
                float mSpeed,
                long mTime,
                double mLastLatitude,
                double mLastLongitude,
                String mDate) {
        type = mType;
        xAverage = mXAverage;
        yAverage = mYAverage;
        zAverage = mZAverage;
        speed = mSpeed;
        time = mTime;
        lastLatitude = mLastLatitude;
        lastLongitude = mLastLongitude;
        date = mDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float getxAverage() {
        return xAverage;
    }

    public void setxAverage(float xAverage) {
        this.xAverage = xAverage;
    }

    public float getyAverage() {
        return yAverage;
    }

    public void setyAverage(float yAverage) {
        this.yAverage = yAverage;
    }

    public float getzAverage() {
        return zAverage;
    }

    public void setzAverage(float zAverage) {
        this.zAverage = zAverage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(double lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public double getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(double lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
