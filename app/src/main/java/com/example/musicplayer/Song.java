package com.example.musicplayer;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song   {
    @PrimaryKey
    @NonNull
    private String songId;
    private String artistName;
    private String title;
    private String path;
    private String displayName;
    private String duration;
    private int numberOfPLay;


    public Song(String albumId, String artistName, String title, String data, String displayName, String duration) {
        this.songId = albumId;
        this.artistName = artistName;
        this.title = title;
        this.displayName = displayName;
        this.path = data;
        this.duration = duration;
    }

    public Song(){

    }


    public String getSongId() {
        return songId;
    }

    public void setSongId(String albumId) {
        this.songId = albumId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String data) {
        this.path = data;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }


    public int getNumberOfPLay() {
        return numberOfPLay;
    }

    public void setNumberOfPLay(int numberOfPLay) {
        this.numberOfPLay = numberOfPLay;
    }
}
