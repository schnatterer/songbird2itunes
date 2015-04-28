package info.schnatterer.songbird2itunes.migration;

import info.schnatterer.itunes4j.ITunes;
import info.schnatterer.itunes4j.entity.Playlist;
import info.schnatterer.itunes4j.entity.Rating;
import info.schnatterer.itunes4j.entity.Track;
import info.schnatterer.itunes4j.exception.ITunesException;
import info.schnatterer.itunes4j.exception.NotModifiableException;
import info.schnatterer.itunes4j.exception.WrongParameterException;
import info.schnatterer.java.lang.ELong;
import info.schnatterer.songbirddbapi4j.SongbirdDb;
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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Songbird2itunesMigration {
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
	 * Migrate all tracks and playlists from a songbird database to iTunes.
	 * 
	 * @param songbirdDbFile
	 *            absolute File path to songbird database
	 * @param exceptionRetries
	 *            After running into a {@link NotModifiableException} - amount
	 *            of times adding track is retried before exiting with an error.
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @return statistics that keeps track of the number of migrated objects
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	public Statistics migrate(String songbirdDbFile, int exceptionRetries,
			boolean setSystemDate) throws SQLException, ITunesException {
		// Create database wrapper instance
		SongbirdDb songbirdDb = createSongbirdDb(new File(songbirdDbFile));
		// Create reference to iTunes
		ITunes iTunes = createItunes();

		Statistics stats = migrateTracks(songbirdDb, iTunes, exceptionRetries,
				setSystemDate);

		// Migrate playlists but do not add attributes again (faster)
		stats.merge(migratePlaylists(songbirdDb, iTunes, exceptionRetries,
				false, false));

		return stats;
	}

	/**
	 * Migrates playlists from songbird2iTunes.
	 * 
	 * @param songbirdDb
	 *            songbird database wrapper
	 * @param iTunes
	 *            iTunes wrapper After running into a
	 *            {@link NotModifiableException} - amount of times adding track
	 *            is retried before exiting with an error.
	 * @param setProperties
	 *            <code>true</code> migrates properties lastPlayTime,
	 *            lastSkipTime, playCount, rating, skipCount
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @return statistics about the migration
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	private Statistics migratePlaylists(SongbirdDb songbirdDb, ITunes iTunes,
			int exceptionRetries, boolean setProperties, boolean setSystemDate)
			throws SQLException, ITunesException {
		Statistics stats = new Statistics();
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
				stats.playlistTrackProcessed();

				Optional<Track> optionalTrack = addTrack(iTunes,
						member.getMember(), exceptionRetries, setProperties,
						setSystemDate);
				if (optionalTrack.isPresent()) {
					printPlaylistTrack(stats.getPlaylistTracksProcessed(),
							playlistName, member.getMember());
					iTunesplaylist.addTrack(optionalTrack.get());
				} else {
					stats.playlistTrackFailed();
				}
			}
		}
		return stats;
	}

	/**
	 * Migrates tracks from songbird2iTunes.
	 * 
	 * @param songbirdDb
	 *            songbird database wrapper
	 * @param iTunes
	 *            iTunes wrapper
	 * @param exceptionRetries
	 *            After running into a {@link NotModifiableException} - amount
	 *            of times adding track is retried before exiting with an error.
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * 
	 * @return statistics about the migration
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	private Statistics migrateTracks(SongbirdDb songbirdDb, ITunes iTunes,
			int exceptionRetries, boolean setSystemDate) throws SQLException,
			ITunesException {
		Statistics stats = new Statistics();

		// Query all tracks from songbird
		List<MediaItem> tracks = songbirdDb.getAllTracks();
		log.info("Found " + tracks.size() + " tracks");

		try {
			for (MediaItem sbTrack : tracks) {
				stats.trackProcessed();
				Optional<Track> optionalTrack = addTrack(iTunes, sbTrack,
						exceptionRetries, true, setSystemDate);
				if (optionalTrack.isPresent()) {
					printTrack(stats.getTracksProcessed(), optionalTrack.get(),
							sbTrack.getContentUrl());
				} else {
					stats.trackFailed();
				}
			}
		} finally {
			if (setSystemDate) {
				log.debug("Trying to resync system time from time server");
				sysCall("cmd /C w32tm /resync /force");
			}
		}
		return stats;
	}

	/**
	 * Logs all details about a track:
	 * 
	 * @param trackIndex
	 *            Number of the track
	 * @param track
	 *            the track whose details to log
	 * @param path
	 *            path to the track
	 */
	private void printPlaylistTrack(long playlistTrackIndex,
			String playlistName, MediaItem sbTrack) {
		log.info("Added playlist track #" + playlistTrackIndex
				+ ": Playlist \"" + playlistName + "\" - Track "
				+ sbTrack.getProperty(Property.PROP_ARTIST_NAME) + " - "
				+ sbTrack.getProperty(Property.PROP_TRACK_NAME));
	}

	/**
	 * Logs all details about a track:
	 * 
	 * @param trackIndex
	 *            Number of the track
	 * @param track
	 *            the track whose details to log
	 * @param path
	 *            path to the track
	 */
	private void printTrack(long trackIndex, Track track, String path) {
		log.info("Added track #" + trackIndex + ": " + track.getArtist()
				+ " - " + track.getName() + ": created=" + track.getDateAdded()
				+ "; lastPlayed=" + track.getPlayedDate() + "; lastSkipTime="
				+ track.getSkippedDate() + "; playCount="
				+ track.getPlayedCount() + "; rating=" + track.getRating()
				+ "; skipCount=" + track.getSkippedCount() + "; path=" + path);
	}

	/**
	 * Add track to iTunes. If a {@link NotModifiableException} is throw, the
	 * method calls itself <code>nRetries</code> recursively. If it still fails
	 * an exception is re-thrown.
	 * 
	 * @param iTunes
	 *            iTunes wrapper instance.
	 * @param sbTrack
	 *            the source track to add to iTunes
	 * @param exceptionRetries
	 *            After running into a {@link NotModifiableException} - amount
	 *            of times adding track is retried before exiting with an error.
	 * @param setProperties
	 *            <code>true</code> migrates properties lastPlayTime,
	 *            lastSkipTime, playCount, rating, skipCount
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track. When <code>false</code>, the current system date is
	 *            used as date adde.Is ignored when <code>setProperties</code>
	 *            is <code>false</code>.
	 * @return an instance of the added track or {@link Optional#empty()} in
	 *         case of error. If empty, a warning was logged.
	 * 
	 * @throws ITunesException
	 *             after all retries have been used.
	 */
	private Optional<Track> addTrack(ITunes iTunes, MediaItem sbTrack,
			int exceptionRetries, boolean setProperties, boolean setSystemDate)
			throws ITunesException {
		Track iTunesTrack = null;
		try {
			// Get absolute path first (as this might fail)
			Optional<String> absolutePath = toAbsolutePath(sbTrack);
			if (!absolutePath.isPresent()) {
				return Optional.empty();
			}

			// Add track and wait for iTunes reference
			iTunesTrack = iTunes.addFile(absolutePath.get());

			Date dateCreated = sbTrack.getDateCreated();

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
				log.debug("Setting system time to " + dateCreated);
				// dd-MM-yy
				sysCall("cmd /C date "
						+ dateFormatHolderDate.get().format(dateCreated));
				// hh:mm:ss
				sysCall("cmd /C time "
						+ dateFormatHolderTime.get().format(dateCreated));
			}

			// Play count
			iTunesTrack.setPlayedCount(convertSongbirdLongValue(playCount));
			// last played
			if (lastPlayTime != null) {
				iTunesTrack.setPlayedDate(lastPlayTime);
			}

			iTunesTrack.setRating(convertSongbirdRating(rating));

			// Skip count
			iTunesTrack.setSkippedCount(convertSongbirdLongValue(skipCount));
			// last skipped
			if (lastSkipTime != null) {
				iTunesTrack.setSkippedDate(lastSkipTime);
			}
			return Optional.of(iTunesTrack);
		} catch (IOException e) {
			log.warn(
					"File not added by iTunes. File corrupt, missing or not supported by iTunes? Skipping file: "
							+ sbTrack.getContentUrl(), e);
		} catch (WrongParameterException e) {
			log.warn(
					"File not added by iTunes. Unsupported type? Skipping file: "
							+ sbTrack.getContentUrl(), e);
			// TODO try to convert?
		} catch (NotModifiableException e) {
			return retryAdding(e, iTunes, sbTrack, exceptionRetries,
					setSystemDate);
		}
		return Optional.empty();
	}

	/**
	 * Migrates a songbird track to an absolute URL in the file system. If not a
	 * valid file an appropriate warning is logged.
	 * 
	 * @param sbTrack
	 *            the songbird track whose absolute path is required
	 * 
	 * @return the absolute path of the track or an empty result if invalid URI
	 *         or not a file URI.
	 * 
	 */
	private Optional<String> toAbsolutePath(MediaItem sbTrack) {
		URI uri = null;
		try {
			uri = new URI(sbTrack.getContentUrl());
			return Optional.of(new File(uri).getAbsolutePath());
		} catch (URISyntaxException e) {
			log.warn(
					"Error adding track iTunes, invalid URI: "
							+ sbTrack.getContentUrl(), e);
			return Optional.empty();
		} catch (IllegalArgumentException e) {
			log.warn("Songbird track URI cannot is not a valid path within the file system. Error: "
					+ e.getMessage()
					+ ". Skipping track: "
					+ sbTrack.getContentUrl());
			return Optional.empty();
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
	 * after a {@link ITunesException}. This is done for <code>nRetries</code>
	 * times, before giving up and logging a warning. Why? iTunes seems to
	 * return errors and reconsiders on retry.
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
	 * @param sbTrack
	 *            reference to the songbird track
	 * @param nRetries
	 *            amount of retries left
	 * @param setSystemDate
	 *            use workaround - try to set the date added in iTunes by
	 *            setting the system date to the date added and then adding the
	 *            track
	 * @return
	 * 
	 * @throws ITunesException
	 *             if thrown by
	 *             {@link #addTrack(ITunes, MediaItem, Statistics, int, boolean)}
	 */
	private Optional<Track> retryAdding(ITunesException e, ITunes iTunes,
			MediaItem sbTrack, int nRetries, boolean setSystemDate)
			throws ITunesException {
		if (nRetries > 0) {
			log.debug(
					"Track was added, but error setting attributes. Retrying "
							+ nRetries + " more times. File: "
							+ sbTrack.getContentUrl(), e);
			return addTrack(iTunes, sbTrack, nRetries - 1, true, setSystemDate);
		} else {
			log.warn(
					"Unable set track attributes, tried multiple times without luck. Skipping. You might manually add  File: "
							+ sbTrack.getContentUrl(), e);
			return Optional.empty();
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
	protected int convertSongbirdLongValue(Long longValue) {
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
	protected Rating convertSongbirdRating(Long rating) {
		return Rating.fromStars(convertSongbirdLongValue(rating));
	}

	public static class Statistics {
		private long tracksProcessed = 0;
		private long tracksFailed = 0;
		private long playlistTracksProcessed = 0;
		private long playlistTracksFailed = 0;
		private long playlistsProcessed = 0;
		private long playlistsFailed = 0;

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

		public long getPlaylistTracksProcessed() {
			return playlistTracksProcessed;
		}

		public long getPlaylistTracksFailed() {
			return playlistTracksFailed;
		}

		public long getPlaylistsProcessed() {
			return playlistsProcessed;
		}

		public long getPlaylistsFailed() {
			return playlistsFailed;
		}

		private void merge(Statistics stats) {
			this.tracksProcessed = stats.tracksProcessed;
			this.tracksFailed = stats.tracksFailed;
			this.playlistTracksProcessed = stats.playlistTracksProcessed;
			this.playlistTracksFailed = stats.playlistTracksFailed;
			this.playlistsProcessed = stats.playlistsProcessed;
			this.playlistsFailed = stats.playlistsFailed;
		}
	}
}
