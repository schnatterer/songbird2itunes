package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.ITunes;
import info.schnatterer.itunes4j.entity.Playlist;
import info.schnatterer.itunes4j.entity.Rating;
import info.schnatterer.itunes4j.entity.Track;
import info.schnatterer.itunes4j.exception.ITunesException;
import info.schnatterer.itunes4j.exception.NotModifiableException;
import info.schnatterer.itunes4j.exception.WrongParameterException;
import info.schnatterer.java.lang.ELong;
import info.schnatterer.songbirddbapi4.SongbirdDb;
import info.schnatterer.songbirddbapi4j.domain.MediaItem;
import info.schnatterer.songbirddbapi4j.domain.MemberMediaItem;
import info.schnatterer.songbirddbapi4j.domain.Property;
import info.schnatterer.songbirddbapi4j.domain.SimpleMediaList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com4j.ComException;

public class Songbird2itunes {
	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static ThreadLocal<DateFormat> dateFormatHolderTime = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("HH:mm");
			return dateFormat;
		}
	};
	private static ThreadLocal<DateFormat> dateFormatHolderDate = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
			return dateFormat;
		}
	};

	/**
	 * Converts all tracks and playlists from a songbird database to iTunes.
	 * 
	 * @param songbirdDbFile
	 *            absolute File path to songbird database
	 * @param exceptionRetries
	 *            After running into a {@link ComException} - amount of times
	 *            adding track is retried before exiting with an error.
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @return statistics that keeps track of the number of converted objects
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	public Statistics convert(String songbirdDbFile, int exceptionRetries,
			boolean setSystemDate) throws SQLException, ITunesException {
		File file = new File(songbirdDbFile);

		// Create database wrapper instance
		SongbirdDb songbirdDb = createSongbirdDb(file);
		// Create reference to iTunes
		ITunes iTunes = createItunes();

		Statistics stats = new Statistics();

		convertTracks(songbirdDb, iTunes, stats, exceptionRetries,
				setSystemDate);

		convertPlaylists(songbirdDb, iTunes, stats);
		return stats;
	}

	/**
	 * Converts playlists from songbird2iTunes.
	 * 
	 * @param songbirdDb
	 *            songbird database wrapper
	 * @param iTunes
	 *            iTunes wrapper
	 * @param stats
	 *            the statistics object that keeps track of the number of
	 *            converted objects
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	private void convertPlaylists(SongbirdDb songbirdDb, ITunes iTunes,
			Statistics stats) throws SQLException, ITunesException {
		List<SimpleMediaList> playLists = songbirdDb.getPlayLists(true, true);
		log.info("Found " + playLists.size() + " playlists");

		for (SimpleMediaList playList : playLists) {
			stats.playlistProcessed();
			String playlistName = playList.getList().getProperty(
					Property.PROP_MEDIA_LIST_NAME);
			Playlist iTunesplaylist = iTunes.createPlaylist(playlistName);
			log.info("Created Playlist #" + stats.getPlaylistsProcessed()
					+ ": " + playlistName);
			for (MemberMediaItem member : playList.getMembers()) {
				try {
					stats.playlistTrackProcessed();

					/*
					 * Don't call addTrack() here because we can assume that all
					 * tracks have been added by now.
					 */
					iTunesplaylist.addFile(new File(new URI(member.getMember()
							.getContentUrl())).getAbsolutePath());
					log.info("Playlist \""
							+ playlistName
							+ "\": Added track "
							+ member.getMember().getProperty(
									Property.PROP_ARTIST_NAME)
							+ " - "
							+ member.getMember().getProperty(
									Property.PROP_TRACK_NAME));
				} catch (URISyntaxException e) {
					log.warn("Error adding track to playlist \"" + playlistName
							+ "\" , invalid URI: "
							+ member.getMember().getContentUrl(), e);
					stats.playlistTrackFailed();
				}
			}
		}
	}

	/**
	 * Converts tracks from songbird2iTunes.
	 * 
	 * @param songbirdDb
	 *            songbird database wrapper
	 * @param iTunes
	 *            iTunes wrapper
	 * @param stats
	 *            the statistics object that keeps track of the number of
	 *            converted objects
	 * @param exceptionRetries
	 *            After running into a {@link ComException} - amount of times
	 *            adding track is retried before exiting with an error.
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	private void convertTracks(SongbirdDb songbirdDb, ITunes iTunes,
			Statistics stats, int exceptionRetries, boolean setSystemDate)
			throws SQLException, ITunesException {
		// Query all tracks from songbird
		List<MediaItem> tracks = songbirdDb.getAllTracks();
		log.info("Found " + tracks.size() + " tracks");

		for (MediaItem sbTrack : tracks) {
			addTrack(iTunes, sbTrack, stats, exceptionRetries, setSystemDate);
		}

		if (setSystemDate) {
			log.debug("Trying to resync system time from time server");
			sysCall("cmd /C w32tm /resync /force");
		}
	}

	/**
	 * Add track to iTunes. If an {@link ComException} is throw, the method
	 * calls itself <code>nRetries</code> recursively. If it still fails an
	 * exception is re-thrown.
	 * 
	 * @param iTunes
	 *            iTunes wrapper instance.
	 * @param sbTrack
	 *            the source track to add to iTunes
	 * @param stats
	 *            the statistics object that keeps track of the number of
	 *            converted objects
	 * @param exceptionRetries
	 *            After running into a {@link ComException} - amount of times
	 *            adding track is retried before exiting with an error.
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @throws ITunesException
	 *             after all retries have been used.
	 */
	private void addTrack(ITunes iTunes, MediaItem sbTrack, Statistics stats,
			int exceptionRetries, boolean setSystemDate) throws ITunesException {
		Track iTunesTrack = null;
		try {
			stats.trackProcessed();

			String absolutePath = new File(new URI(sbTrack.getContentUrl()))
					.getAbsolutePath();

			Date dateCreated = sbTrack.getDateCreated();

			String trackName = sbTrack.getProperty(Property.PROP_TRACK_NAME);
			String artistName = sbTrack.getProperty(Property.PROP_ARTIST_NAME);

			Date lastPlayTime = sbTrack
					.getPropertyAsDate(Property.PROP_LAST_PLAY_TIME);
			Date lastSkipTime = sbTrack
					.getPropertyAsDate(Property.PROP_LAST_SKIP_TIME);
			Long playCount = sbTrack
					.getPropertyAsLong(Property.PROP_PLAY_COUNT);
			Long rating = sbTrack.getPropertyAsLong(Property.PROP_RATING);
			Long skipCount = sbTrack
					.getPropertyAsLong(Property.PROP_SKIP_COUNT);

			if (setSystemDate) {
				// TODO fail on error here?
				/*
				 * Changing the dateAdded is not possible via iTunes COM API
				 * 
				 * Dirty Hack: Change the computer's system date, add the file
				 * and This will only work if the process runs as administrator
				 * and iTunes is either not running or already started as
				 * administrator!
				 */
				log.debug("Track #" + stats.getTracksProcessed()
						+ ": Setting system time to " + dateCreated);
				// dd-MM-yy
				sysCall("cmd /C date "
						+ dateFormatHolderDate.get().format(dateCreated));
				// hh:mm:ss
				sysCall("cmd /C time "
						+ dateFormatHolderTime.get().format(dateCreated));
			}

			iTunesTrack = iTunes.addFile(absolutePath);

			// Play count
			iTunesTrack.setPlayedCount(handleSongbirdLongValue(playCount));
			// last played
			if (lastPlayTime != null) {
				iTunesTrack.setPlayedDate(lastPlayTime);
			}

			iTunesTrack.setRating(handleSongbirdRating(rating));

			// Skip count
			iTunesTrack.setSkippedCount(handleSongbirdLongValue(skipCount));
			// last skipped
			if (lastSkipTime != null) {
				iTunesTrack.setSkippedDate(lastSkipTime);
			}

			log.info("Added track #" + stats.getTracksProcessed() + ": "
					+ artistName + " - " + trackName + ": created="
					+ dateCreated + "; lastPlayed=" + lastPlayTime
					+ "; lastSkipTime=" + lastSkipTime + "; playCount="
					+ playCount + "; rating=" + rating + "; skipCount="
					+ skipCount + "; path=" + absolutePath);
		} catch (URISyntaxException e) {
			log.warn(
					"Error adding track iTunes, invalid URI: "
							+ sbTrack.getContentUrl(), e);
			stats.trackFailed();
		} catch (IOException e) {
			log.warn(
					"File not added by iTunes. Corrupt or missing? Skipping file: "
							+ sbTrack.getContentUrl(), e);
			stats.trackFailed();
		} catch (WrongParameterException e) {
			log.warn(
					"File not added by iTunes. Unsupported type? Skipping file: "
							+ sbTrack.getContentUrl(), e);
			stats.trackFailed();
			// TODO try to convert?
		} catch (NotModifiableException e) {
			retryAdding(e, iTunes, stats, sbTrack, exceptionRetries,
					setSystemDate);
		}
	}

	private void sysCall(String execCommand) {
		Process exec;
		try {
			exec = Runtime.getRuntime().exec(execCommand);
			exec.waitFor();
			if (exec.exitValue() == 1) {
				log.warn("System call failed (returned 1): \"" + execCommand
						+ "\"");
			}
		} catch (IOException e) {
			log.warn("System call failed with exception: \"" + execCommand
					+ "\"", e);
		} catch (InterruptedException e) {
			log.warn("System call failed with exception: \"" + execCommand
					+ "\"", e);
		}
	}

	/**
	 * Retries calling {@link #addTrack(ITunes, MediaItem, Statistics, int)}
	 * after a {@link ITunesException}. This will be done for
	 * <code>nRetries</code> times. Then the application exits in error. Why?
	 * iTunes seems to return errors and reconsiders on retry.
	 * 
	 * An example is the "a0040203" ({@link NotModifiableException}) error in
	 * iTunes.
	 * 
	 * This exception might occur right after a track has been added but iTunes
	 * (for some reasons) won't let us modify it for some more milliseconds.
	 * Maybe it parses artwork or goes fishing.
	 * 
	 * After handling the exception (~ 500ms) modifying the track will most
	 * likely work, because iTunes seems to have finished what it did before. So
	 * just keep trying some more times. If the error persists, throw Exception.
	 * 
	 * @param e
	 *            exception that might be a "a0040203"
	 * @param iTunes
	 *            reference to the iTunes wrapper
	 * @param stats
	 *            number of the track
	 * @param sbTrack
	 *            reference to the songbird track
	 * @param nRetries
	 *            amount of retries left
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @throws ITunesException
	 *             after all retries have been used.
	 */
	private void retryAdding(ITunesException e, ITunes iTunes,
			Statistics stats, MediaItem sbTrack, int nRetries,
			boolean setSystemDate) throws ITunesException {
		if (nRetries > 0) {
			log.debug(
					"Track was added, but error setting attributes. Retrying "
							+ nRetries + " more times. File: "
							+ sbTrack.getContentUrl(), e);
			addTrack(iTunes, sbTrack, stats, nRetries - 1, setSystemDate);
		} else {
			throw new ITunesException(
					"Unable to add track. Tried multiple times without luck. File: "
							+ sbTrack.getContentUrl(), e);
		}
	}

	/**
	 * Factory method for {@link SongbirdDb} API. Useful for testing.
	 * 
	 * @param songbirdDbFile
	 *            the path to the database
	 * @return an instance of the songbirdDb API
	 */
	protected SongbirdDb createSongbirdDb(File songbirdDbFile) {
		return new SongbirdDb(songbirdDbFile.getAbsolutePath());
	}

	/**
	 * Factory method for {@link ITunes} wrapper. Useful for testing.
	 * 
	 * @return
	 */
	protected ITunes createItunes() {
		return new ITunes();
	}

	/**
	 * Songbird database might return a <code>null</code> that means zero, in
	 * addition iTunes can only handle integers. This method provides this
	 * conversion, returning a primitive long value.
	 * 
	 * @param longValue
	 *            value to convert
	 * @return a primitive (non-<code>null</code>) instance of
	 *         <code>longValue</code>
	 */
	protected int handleSongbirdLongValue(Long longValue) {
		if (longValue == null) {
			return 0;
		}

		return new ELong(longValue).toInt();
	}

	/**
	 * Converts a songbird rating (<code>null</code> or 0..5) to an iTunes
	 * {@link Rating} object.
	 * 
	 * @param rating
	 *            the rating read from songbird
	 * @return an iTunes {@link Rating} object
	 */
	protected Rating handleSongbirdRating(Long rating) {
		return Rating.fromStars(handleSongbirdLongValue(rating));
	}

	public static class Statistics {
		private long tracksProcessed = 0;
		private long tracksFailed = 0;
		private long playlistTracksProcessed = 0;
		private long playlistTracksFailed = 0;
		private long playlistsProcessed = 0;
		private final long playlistsFailed = 0;

		private void trackProcessed() {
			tracksProcessed++;
		}

		private void trackFailed() {
			tracksFailed++;
		}

		private void playlistTrackProcessed() {
			playlistTracksProcessed++;
		}

		private void playlistTrackFailed() {
			playlistTracksFailed++;
		}

		private void playlistProcessed() {
			playlistsProcessed++;
		}

		// private void playlistFailed() {
		// playlistsFailed++;
		// }

		public long getTracksFailed() {
			return tracksFailed;
		}

		public long getTracksProcessed() {
			return tracksProcessed;
		}

		protected long getPlaylistTracksProcessed() {
			return playlistTracksProcessed;
		}

		protected long getPlaylistTracksFailed() {
			return playlistTracksFailed;
		}

		protected long getPlaylistsProcessed() {
			return playlistsProcessed;
		}

		protected long getPlaylistsFailed() {
			return playlistsFailed;
		}

	}
}
