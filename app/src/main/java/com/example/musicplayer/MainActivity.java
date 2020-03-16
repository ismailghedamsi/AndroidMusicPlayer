package com.example.musicplayer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.pixplicity.easyprefs.library.Prefs;


import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import needle.Needle;


public class MainActivity extends AppCompatActivity {

    private SimpleExoPlayer  player ;
    List<Song> songs;
    private int positionClick;
    MediaSource audioSource;
    PlayerView playerView ;
    DefaultDataSourceFactory dataSourceFactory;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 225);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),AppDatabase.class, "database-name").build();
        updateNumberOfPlay(db);
        playerView = findViewById(R.id.video_view);
        player = ExoPlayerFactory.newSimpleInstance(this,new DefaultTrackSelector());
     dataSourceFactory = new DefaultDataSourceFactory(this,Util.getUserAgent(this,"audio demo"));

        listView = (ListView) findViewById(R.id.songsList);
        CustomAdapter adapter=
                new CustomAdapter(this, R.layout.song_item, getMusicLibrary());
        listView.setAdapter(adapter);
     setSongsListView(adapter);
        new Prefs.Builder()
                .setContext(getApplicationContext())
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName("lastPlayedSong")
                .setUseDefaultSharedPreference(true)
                .build();
        Toast.makeText(this,Prefs.getInt("lastPlayedSong",0)+"",Toast.LENGTH_LONG).show();
        listView.setSelection(Prefs.getInt("lastPlayedSong",0));
        adapter.selectItem(Prefs.getInt("lastPlayedSong",0));


    }

    private void updateNumberOfPlay(AppDatabase db) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    // db.userDao().insertUsers(getMusicLibrary());
                    Log.e(MainActivity.class.getSimpleName(),db.userDao().getAll().size()+"");
                    Song s = db.userDao().getAll().get(1);
                    s.setNumberOfPLay(2);
                    db.userDao().updateSong(s);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void setSongsListView(CustomAdapter adapter) {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new Prefs.Builder()
                        .setContext(getApplicationContext())
                        .setMode(ContextWrapper.MODE_PRIVATE)
                        .setPrefsName("lastPlayedSong")
                        .setUseDefaultSharedPreference(true)
                        .build();
                Prefs.putInt("lastPlayedSong",position);
                view.setBackgroundColor(Color.BLUE);
                adapter.selectItem(position);
                      player.prepare(   new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(getMusicLibrary().get(position).getPath())));
        player.setPlayWhenReady(true);

            }
        });
    }


    public  static String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        //Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours == 0) {
            finalTimerString = hours + ":";
        }

        // Pre appending 0 to seconds if it is one digit
        if (seconds == 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }


    public List<Song> getMusicLibrary() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };

        Cursor cursor = this.managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

       List<Song> songs = new ArrayList<Song>();
        while(cursor.moveToNext()) {
            songs.add( new Song(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getString(4),milliSecondsToTimer(cursor.getLong(5))));
        }
        return songs;
    }
}
