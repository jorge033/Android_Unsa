package com.phant.musicappwidget;

import java.io.IOException;
import java.math.BigDecimal;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MusicActivity extends ListActivity {
	private static final int UPDATE_FREQUENCY = 500;
	private static final int STEP_VALUE = 4000;

	private MediaCursorAdapter mediaAdapter = null;
	private TextView selectedFile = null;
	private SeekBar seekbar = null;
	private MediaPlayer reproductor;
	private ImageButton play = null;
	private ImageButton prev = null;
	private ImageButton next = null;
	private String currentSong = "";
	private boolean isStarted = true;
	private boolean isMoveingSeekBar = false;

	private final Handler handler = new Handler();

	private final Runnable updatePositionRunnable = new Runnable() {
		public void run() {
			updatePosition();
		}
	};

	private class MediaCursorAdapter extends SimpleCursorAdapter {

		@SuppressWarnings("deprecation")
		public MediaCursorAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c, new String[] {
					MediaStore.MediaColumns.DISPLAY_NAME,
					MediaStore.MediaColumns.TITLE,
					MediaStore.Audio.AudioColumns.DURATION }, new int[] {
					R.id.displayname, R.id.title, R.id.duration });
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView titulo = (TextView) view.findViewById(R.id.title);
			TextView nombre = (TextView) view.findViewById(R.id.displayname);
			TextView duracion = (TextView) view.findViewById(R.id.duration);

			nombre.setText(cursor.getString(cursor
					.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));

			titulo.setText(cursor.getString(cursor
					.getColumnIndex(MediaStore.MediaColumns.TITLE)));

			long duracionMs = Long.parseLong(cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

			double duracionMin = ((double) duracionMs / 1000.0) / 60.0;

			duracionMin = new BigDecimal(Double.toString(duracionMin))
					.setScale(2, BigDecimal.ROUND_UP).doubleValue();

			duracion.setText("" + duracionMin);

			view.setTag(cursor.getString(cursor
					.getColumnIndex(MediaStore.MediaColumns.DATA)));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(R.layout.playlist, parent, false);

			bindView(v, context, cursor);

			return v;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music);
		reproductor = new MediaPlayer();
		
		seekbar = (SeekBar) findViewById(R.id.seekbar);
        play = (ImageButton)findViewById(R.id.play);
        prev = (ImageButton)findViewById(R.id.prev);
        next = (ImageButton)findViewById(R.id.next);
        selectedFile = (TextView) findViewById(R.id.selectedfile);
        
		reproductor.setOnCompletionListener(onCompletion);
		reproductor.setOnErrorListener(onError);
		seekbar.setOnSeekBarChangeListener(seekBarChanged);

		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);

		if (cursor != null) {
			cursor.moveToFirst();

			mediaAdapter = new MediaCursorAdapter(this, R.layout.playlist,
					cursor);

			setListAdapter(mediaAdapter);

			play.setOnClickListener(onButtonClick);
			next.setOnClickListener(onButtonClick);
			prev.setOnClickListener(onButtonClick);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.music, menu);
		return true;
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position,
			long id) {
		super.onListItemClick(list, view, position, id);

		currentSong = (String) view.getTag();

		startPlay(currentSong);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		handler.removeCallbacks(updatePositionRunnable);
		reproductor.stop();
		reproductor.reset();
		reproductor.release();

		reproductor = null;
	}

	private void startPlay(String file) {
		Log.i("Selected: ", file);

		selectedFile.setText(file);
		seekbar.setProgress(0);

		reproductor.stop();
		reproductor.reset();

		try {
			reproductor.setDataSource(file);
			reproductor.prepare();
			reproductor.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		seekbar.setMax(reproductor.getDuration());
		play.setImageResource(android.R.drawable.ic_media_pause);

		updatePosition();

		isStarted = true;
	}

	private void stopPlay() {
		reproductor.stop();
		reproductor.reset();
		play.setImageResource(android.R.drawable.ic_media_play);
		handler.removeCallbacks(updatePositionRunnable);
		seekbar.setProgress(0);

		isStarted = false;
	}

	private void updatePosition() {
		handler.removeCallbacks(updatePositionRunnable);

		seekbar.setProgress(reproductor.getCurrentPosition());

		handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
	}

	private View.OnClickListener onButtonClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.play: {
				if (reproductor.isPlaying()) {
					handler.removeCallbacks(updatePositionRunnable);
					reproductor.pause();
					play
							.setImageResource(android.R.drawable.ic_media_play);
				} else {
					if (isStarted) {
						reproductor.start();
						play
								.setImageResource(android.R.drawable.ic_media_pause);

						updatePosition();
					} else {
						startPlay(currentSong);
					}
				}

				break;
			}
			case R.id.next: {
				int seekto = reproductor.getCurrentPosition() + STEP_VALUE;

				if (seekto > reproductor.getDuration())
					seekto = reproductor.getDuration();

				reproductor.pause();
				reproductor.seekTo(seekto);
				reproductor.start();

				break;
			}
			case R.id.prev: {
				int seekto = reproductor.getCurrentPosition() - STEP_VALUE;

				if (seekto < 0)
					seekto = 0;

				reproductor.pause();
				reproductor.seekTo(seekto);
				reproductor.start();

				break;
			}
			}
		}
	};

	private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			stopPlay();
		}
	};

	private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}
	};

	private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			isMoveingSeekBar = false;
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			isMoveingSeekBar = true;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (isMoveingSeekBar) {
				reproductor.seekTo(progress);

				Log.i("OnSeekBarChangeListener", "onProgressChanged");
			}
		}
	};
}
