package com.example.musicplayer;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.room.Room;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.pixplicity.easyprefs.library.Prefs;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity {

    private SimpleExoPlayer  player ;
    List<Song> songs;
    private int oldPosition;
    MediaSource audioSource;
    PlayerView playerView ;
    DefaultDataSourceFactory dataSourceFactory;
    ListView listView;
    EditText inputSearch;
    PlayerNotificationManager playerNotificationManager;
    ConcatenatingMediaSource concatenatingMediaSource;
    Button searchButton;
    CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 225);

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)
        {
            new Prefs.Builder()
                    .setContext(getApplicationContext())
                    .setMode(ContextWrapper.MODE_PRIVATE)
                    .setPrefsName("lastPlayedSong")
                    .setUseDefaultSharedPreference(true)
                    .build();
            songs = getMusicLibrary();
            Collections.sort(songs, (o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),AppDatabase.class, "database-name").build();
            updateNumberOfPlay(db);
            setUpMusicPlayer();
            listView =  findViewById(R.id.songsList);
            listView.setLongClickable(true);
            listView.setClickable(true);
            adapter=
                    new CustomAdapter(this, R.layout.song_item, songs);
            listView.setAdapter(adapter);
            setSongsListView(adapter);
            selectLastPlayerSong(adapter);
            inputSearch = (EditText) findViewById(R.id.autoCompleteTextView);
            inputSearch.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                    // When user changed the Text
                    ((CustomAdapter)MainActivity.this.listView.getAdapter()).getFilter().filter(cs);
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                }
            });

            searchButton = findViewById(R.id.searchButton);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buildAlertDialog(adapter, inputSearch.getText().toString());

                }
            });
        }else{
            Toast.makeText(this,"Permission not granted",Toast.LENGTH_LONG).show();
        }







        //buildAlertDialog("151");

    }

    private void selectLastPlayerSong(CustomAdapter adapter) {
        new Prefs.Builder()
                .setContext(getApplicationContext())
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName("lastPlayedSong")
                .setUseDefaultSharedPreference(true)
                .build();
       listView.setSelection(Prefs.getInt("lastPlayedSong",0));
        adapter.selectItem(Prefs.getInt("lastPlayedSong",0));
    }

    private void setUpMusicPlayer() {
        playerView = findViewById(R.id.video_view);
        player = ExoPlayerFactory.newSimpleInstance(this,new DefaultTrackSelector());
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this,"audio demo"));

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(getApplicationContext(), "channel1", R.string.playback_channel_name, 1,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return songs.get(player.getCurrentWindowIndex()).getTitle();
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        return PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return songs.get(player.getCurrentWindowIndex()).getArtistName();
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return null;
                    }
                }
                );
        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                                                                      @Override
                                                                      public void onNotificationStarted ( int notificationId, Notification notification){
                                                                          Intent intent = new Intent();
                                                                          Util.startForegroundService(getApplicationContext(),intent);
                                                                      }
                                                                      @Override
                                                                      public void onNotificationCancelled ( int notificationId){
                                                                          player.release();
                                                                      }
                                                                  }
        );

        player.addListener(new ExoPlayer.EventListener(){
            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(playbackState == ExoPlayer.STATE_ENDED){
                    player.prepare(   new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(Uri.parse(songs.get(oldPosition+1).getPath())));
                    player.setPlayWhenReady(true);
                }
            }


            @Override
            public void onPlayerError(ExoPlaybackException error) {
            }

            @Override
            public void onPositionDiscontinuity(int i) {
                int latestWindowIndex = player.getCurrentWindowIndex();
                    // item selected in playlist has changed, handle here
                    oldPosition = latestWindowIndex;
            }
        });
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

                Prefs.putInt("lastPlayedSong",position);
                view.setBackgroundColor(Color.BLUE);
                oldPosition = position;
                adapter.selectItem(position);
                     player.prepare(   new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(songs.get(position).getPath())));
                player.setPlayWhenReady(true);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),"clicked",Toast.LENGTH_LONG).show();
                PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                popup.getMenuInflater().inflate(R.menu.main_menu,
                        popup.getMenu());
                popup.show();


                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.addToPlaylist:

                                break;
                        }
                        return true;
                    }
                });
                return true;
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

    public void buildAlertDialog(CustomAdapter adapter,String searchedTitle){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Map<Integer,String> foundSongs = new HashMap<>();
        builder.setTitle("Choose an song");
       for(int i=0;i<songs.size();i++){

           if( StringUtils.containsIgnoreCase(songs.get(i).getTitle(),searchedTitle)){
                foundSongs.put(i,songs.get(i).getTitle() + "\n" + songs.get(i).getArtistName());
           }
       }
        String[] foundSongsTitles = foundSongs.values().toArray(new String[foundSongs.size()]);

        builder.setItems(foundSongsTitles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = foundSongsTitles[which].substring(0,foundSongsTitles[which].indexOf("\n")).trim();
                String artist =  foundSongsTitles[which].substring(foundSongsTitles[which].indexOf("\n")).trim();
                Song foundSong = songs.stream().filter((song) -> song.getArtistName().equalsIgnoreCase(artist) && StringUtils.containsIgnoreCase(song.getTitle(),title)).collect(Collectors.toList()).get(0);
                Toast.makeText(getApplicationContext(),songs.indexOf(foundSong)+"",Toast.LENGTH_LONG).show();

                listView.requestFocusFromTouch();
                listView.setSelection(songs.indexOf(foundSong));
                adapter.selectItem(songs.indexOf(foundSong));
                listView.setItemChecked(songs.indexOf(foundSong),true);
                Prefs.putInt("lastPlayedSong",songs.indexOf(foundSong));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        menu.setHeaderTitle("Select The Action");
    }


}
