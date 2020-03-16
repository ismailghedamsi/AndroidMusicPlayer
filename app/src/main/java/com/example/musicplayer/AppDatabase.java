package com.example.musicplayer;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Song.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SongDao userDao();
}