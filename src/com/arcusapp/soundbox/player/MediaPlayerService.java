package com.arcusapp.soundbox.player;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.arcusapp.soundbox.model.BundleExtra;
import com.arcusapp.soundbox.model.MediaPlayerServiceListener;
import com.arcusapp.soundbox.model.RandomState;
import com.arcusapp.soundbox.model.RepeatState;
import com.arcusapp.soundbox.model.Song;

public class MediaPlayerService extends Service implements OnCompletionListener {

	private static final String TAG = "MediaPlayerService";

	// private int currentSongPosition;
	private SongStack currentSongStack;
	private List<String> songsIDList;

	private RepeatState repeatState = RepeatState.Off;
	private RandomState randomState = RandomState.Off;

	private MediaPlayer mediaPlayer;
	private MediaPlayerServiceListener currentListener;
	private final IBinder mBinder = new MyBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Called every time a client starts the service using startService
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return Service.START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Called when the Service object is instantiated. Theoretically, only once.

		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnCompletionListener(this);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent arg0) {
		currentListener = null;
		return true;
	}

	public class MyBinder extends Binder {
		public MediaPlayerService getService() {
			return MediaPlayerService.this;
		}
	}

	public void registerListener(MediaPlayerServiceListener listener) {
		this.currentListener = listener;
	}

	public void playSongs(String currentSongID, List<String> songsID) {
		if (songsID.size() == 0) {
			Log.d(TAG, "No songs to play");
			return;
		}
		if (currentSongID != BundleExtra.DefaultValues.DEFAULT_ID && !songsID.contains(currentSongID)) {
			Log.d(TAG, "Cant handle the given current ID");
			return;
		}
		songsIDList = songsID;

		int currentSongPosition;
		if (currentSongID == BundleExtra.DefaultValues.DEFAULT_ID) {
			currentSongPosition = 0;
		} else {
			currentSongPosition = songsID.indexOf(currentSongID);
		}

		// create the song stack
		currentSongStack = new SongStack(currentSongPosition, this.songsIDList, randomState);

		playCurrentSong();
	}

	public Song getCurrentSong() {
		return currentSongStack.getCurrentSong();
	}

	public List<String> getSongsIDList() {
		if (currentSongStack.getCurrentRandomState() == RandomState.Random) {
			return songsIDList;
		} else {
			return currentSongStack.getCurrentSongsIDList();
		}

	}

	public RandomState getRandomState() {
		return currentSongStack.getCurrentRandomState();
	}

	public RepeatState getRepeatState() {
		return repeatState;
	}

	public RandomState changeRandomState() {
		if (randomState == RandomState.Off) {
			randomState = RandomState.Shuffled;
		}
		else if (randomState == RandomState.Shuffled) {
			randomState = RandomState.Random;
		}
		else if (randomState == RandomState.Random) {
			randomState = RandomState.Off;
		}
		currentSongStack.setRandomState(randomState);
		return randomState;
	}

	public RepeatState changeRepeatState() {
		if (repeatState == RepeatState.Off) {
			repeatState = RepeatState.All;
		}
		else if (repeatState == RepeatState.All) {
			repeatState = RepeatState.One;
		}
		else if (repeatState == RepeatState.One) {
			repeatState = RepeatState.Off;
		}
		return repeatState;
	}

	public void playAndPause() {
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
		else {
			mediaPlayer.pause();
		}
	}

	public void playNextSong() {
		currentSongStack.moveStackFoward();
		// check if we started the playlist again
		if (currentSongStack.getCurrentSong().getID() == currentSongStack.getCurrentSongsIDList().get(0)) {
			if (repeatState == RepeatState.Off) {
				mediaPlayer.stop();
			} else {
				playCurrentSong();
			}
		} else {
			playCurrentSong();
		}
	}

	public void playPreviousSong() {
		currentSongStack.moveStackBackward();
		playCurrentSong();
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		if (repeatState == RepeatState.One) {
			mediaPlayer.stop();
			mediaPlayer.start();
		} else {
			playNextSong();
			currentListener.onSongCompletion();
		}
	}

	private void playCurrentSong() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
		}

		// play the song
		Song currentSong = currentSongStack.getCurrentSong();
		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(currentSong.getFilePath());
			mediaPlayer.prepare();
		} catch (Exception e) {
			Log.d(TAG, "Wrong file path on the first song");
		}

		currentListener.onSongCompletion();
		mediaPlayer.start();
	}
}
