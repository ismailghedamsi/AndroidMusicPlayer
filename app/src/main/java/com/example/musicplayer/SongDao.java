package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM songs")
    List<Song> getAll();

    @Query("SELECT * FROM songs WHERE songId IN (:userIds)")
    List<Song> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM songs WHERE title LIKE :title AND " +
            "artistName LIKE :artist LIMIT 1")
    Song findByTitleArtist(String title,String artist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertUsers(List<Song> songs);

    @Delete
    void delete(Song song);

    @Update
    int updateSong(Song song);
}
