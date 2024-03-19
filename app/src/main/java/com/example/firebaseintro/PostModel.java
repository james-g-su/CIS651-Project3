package com.example.firebaseintro;

import com.google.android.gms.maps.model.Marker;

public class PostModel{
    public String postKey;
    public String uid;
    public String description;
    public String url;
    public String date;
    public Marker m;
    public PostModel(String uid, String description, String url, String date, String key, Marker _m) {
        this.uid=uid;
        this.description=description;
        this.url=url;
        this.date=date;
        this.postKey=key;
        this.m = _m;
    }
}
