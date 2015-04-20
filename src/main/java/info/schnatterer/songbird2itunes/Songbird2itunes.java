package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.ITunes;
import info.schnatterer.itunes4j.ITunesException;
import info.schnatterer.itunes4j.entity.Playlist;
import info.schnatterer.itunes4j.entity.Rating;
import info.schnatterer.itunes4j.entity.Track;
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
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com4j.ComException;

public class Songbird2itunes {
	/**
	 * After running into a "a0040203" error - amount of times writing is
	 * retried before exiting with an error.
	 */
	private static final int A0040203_RETRIES = 50;

	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Converts all tracks and playlists from a songbird database to iTunes.
	 * 
	 * @param songbirdDbFile
	 *            absolute File path to songbird database
	 * 
	 * @return statistics that keeps track of the number of converted objects
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	public Statistics convert(String songbirdDbFile) throws SQLException,
			ITunesException {
		File file = new File(songbirdDbFile);

		// Create database wrapper instance
		SongbirdDb songbirdDb = createSongbirdDb(file);
		// Create reference to iTunes
		ITunes iTunes = createItunes();

		Statistics stats = new Statistics();

		convertTracks(songbirdDb, iTunes, stats);

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
		for (SimpleMediaList playList : playLists) {
			stats.playlistProcessed();
			Playlist iTunesplaylist = iTunes.createPlaylist(playList.getList()
					.getProperty(Property.PROP_MEDIA_LIST_NAME));
			for (MemberMediaItem member : playList.getMembers()) {
				try {
					stats.playlistTrackProcessed();
					iTunesplaylist.addFile(new File(new URI(member.getMember()
							.getContentUrl())).getAbsolutePath());
				} catch (URISyntaxException e) {
					log.warn(
							"Error adding track to playlist \""
									+ playList.getList().getProperty(
											Property.PROP_MEDIA_LIST_NAME)
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
	 * 
	 * @throws SQLException
	 *             errors when querying source database
	 * @throws ITunesException
	 *             errors when writing to target iTunes
	 */
	private void convertTracks(SongbirdDb songbirdDb, ITunes iTunes,
			Statistics stats) throws SQLException, ITunesException {
		// Query all tracks from songbird
		List<MediaItem> tracks = songbirdDb.getAllTracks();
		log.info("Found " + tracks.size() + " tracks");

		for (MediaItem sbTrack : tracks) {
			// TODO offer option for setting the date
			/*
			 * Changing the dateAdded is not possible via iTunes COM API
			 * 
			 * Dirty Hack: Change the computer's system date, add the file and
			 * This will only work if the process runs as administrator and
			 * iTunes is either not running or already started as administrator!
			 */
			//
			// // resync the system date
			// System.out.println("Setting system time");
			// Process exec = Runtime.getRuntime().exec("cmd /C date " +
			// "01-01-1990"); // dd-MM-yy
			// exec.waitFor();
			// if (exec.exitValue() == 1) {
			// System.out.println("Warning - Setting system time failed");
			// }
			//
			// // Runtime.getRuntime().exec("cmd /C time " + strTimeToSet); //
			// hh:mm:ss

			addTrack(iTunes, sbTrack, stats, A0040203_RETRIES);
		}

		// TODO once we're finished re-sync system clock
		// System.out.println("Trying to resync system time from time server");
		// exec = Runtime.getRuntime().exec("cmd /C w32tm /resync /force");
		// exec.waitFor();
		// if (exec.exitValue() == 1) {
		// System.out
		// .println("Warning - Failed to resync system time from time server");
		// }
	}

	/**
	 * Add track to iTunes. If an "a0040203" error occurs the method calls
	 * itself <code>nRetries</code> recursively. If it still fails an exception
	 * is thrown.
	 * 
	 * @param iTunes
	 *            iTunes wrapper instance.
	 * @param sbTrack
	 *            the source track to add to iTunes
	 * @param stats
	 *            the statistics object that keeps track of the number of
	 *            converted objects
	 * @param nRetries
	 *            number of retries after an "a0040203" error
	 * 
	 * @throws ITunesException
	 *             after {@link #A0040203_RETRIES} unsuccessful retries.
	 */
	private void addTrack(ITunes iTunes, MediaItem sbTrack, Statistics stats,
			int nRetries) throws ITunesException {
		Track iTunesTrack = null;
		try {
			stats.trackProcessed();

			String absolutePath = new File(new URI(sbTrack.getContentUrl()))
					.getAbsolutePath();
			iTunesTrack = iTunes.addFile(absolutePath);

			Date dateCreate = sbTrack.getDateCreated();

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

			log.info("Added Track #" + stats.getTracksProcessed() + ": "
					+ artistName + " - " + trackName + ": created="
					+ dateCreate + "; lastPlayed=" + lastPlayTime
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
		} catch (ComException e) {
			handleA0040203(e, iTunes, stats, sbTrack, nRetries);
		}
	}

	/**
	 * Handles "a0040203" (not modifiable) errors in iTunes.
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
	 * 
	 * @throws ITunesException
	 *             after {@link #A0040203_RETRIES} unsuccessful retries.
	 * @throws ComException
	 *             <code>e</code> is re-thrown when not an a0040203
	 */
	private void handleA0040203(ComException e, ITunes iTunes,
			Statistics stats, MediaItem sbTrack, int nRetries)
			throws ITunesException {
		if (!e.getMessage().contains("a0040203")) {
			// Rethrow other exceptions
			throw e;
		}
		if (nRetries > 0) {
			log.debug(
					"Track was added, but error setting attributes. Retrying "
							+ nRetries + " more times. File: "
							+ sbTrack.getContentUrl(), e);
			addTrack(iTunes, sbTrack, stats, nRetries - 1);
		} else {
			throw new ITunesException("Unable to modify track. Tried "
					+ A0040203_RETRIES + " times without luck. File: "
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
