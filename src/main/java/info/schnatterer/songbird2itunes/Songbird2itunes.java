package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.ITunes;
import info.schnatterer.itunes4j.ITunesException;
import info.schnatterer.itunes4j.entity.Rating;
import info.schnatterer.itunes4j.entity.Track;
import info.schnatterer.java.lang.ELong;
import info.schnatterer.songbirddbapi4.SongbirdDb;
import info.schnatterer.songbirddbapi4j.domain.MediaItem;
import info.schnatterer.songbirddbapi4j.domain.Property;

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
import com4j.util.ComObjectCollector;

public class Songbird2itunes {
	/**
	 * After running into a "a0040203" error - amount of times writing is
	 * retried before exiting with an error.
	 */
	private static final int A0040203_RETRIES = 50;
	/**
	 * Maximal amount of milliseconds the application sleeps after adding a
	 * file. Sleeping might be necessary because of the "a0040203" error
	 */
	private static final int A0040203_SLEEP_TIME_MAX = 200;
	/**
	 * The amount of milliseconds that is added to the sleeptime after an
	 * "a0040203" error occurred.
	 */
	private static final int A0040203_SLEEP_TIME_INCREMENT = 10;

	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public void convert(String songbirdDbFile) throws SQLException,
			ITunesException {
		File file = new File(songbirdDbFile);
		int sleepTime = 0;

		// Create database wrapper instance
		SongbirdDb songbirdDb = createSongbirdDb(file);
		// Create reference to iTunes
		ITunes iTunes = createItunes();

		// Query all tracks from songbird
		List<MediaItem> tracks = songbirdDb.getAllTracks();
		log.info(String.format("Found %s tracks", tracks.size()));

		for (int i = 0; i < tracks.size(); i++) {
			MediaItem sbTrack = tracks.get(i);

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

			sleepTime = addTrack(iTunes, i, sbTrack, A0040203_RETRIES,
					sleepTime);
		}

		// TODO once we're finished re-sync system clock
		// System.out.println("Trying to resync system time from time server");
		// exec = Runtime.getRuntime().exec("cmd /C w32tm /resync /force");
		// exec.waitFor();
		// if (exec.exitValue() == 1) {
		// System.out
		// .println("Warning - Failed to resync system time from time server");
		// }

		// Query all playlists
		// List<SimpleMediaList> playLists = songbirdDb.getPlayLists(true,
		// true);
		// for (SimpleMediaList playList : playLists) {
		// try {
		// Playlist iTunesplaylist = iTunes.createPlaylist(playList
		// .getList().getProperty(Property.PROP_MEDIA_LIST_NAME));
		// for (MemberMediaItem member : playList.getMembers()) {
		// iTunesplaylist.addFile(new File(new URI(member.getMember()
		// .getContentUrl())).getAbsolutePath());
		// }
		// } catch (ITunesException e) {
		// // TODO handle
		// throw e;
		// } catch (URISyntaxException e) {
		// // TODO handle
		// throw new RuntimeException(e);
		// }
		// }
	}

	/**
	 * Add track to iTunes.
	 * 
	 * @param iTunes
	 *            iTunes wrapper instance.
	 * @param trackIndex
	 * @param sbTrack
	 * @param nRetries
	 * @param sleepTime
	 * 
	 * @return
	 * 
	 * @throws ITunesException
	 *             after {@link #A0040203_RETRIES} unsuccessfull retries.
	 */
	private int addTrack(ITunes iTunes, int trackIndex, MediaItem sbTrack,
			int nRetries, int sleepTime) throws ITunesException {
		Track iTunesTrack = null;
		try {
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

			if (sleepTime > 0) {
				/*
				 * Sleep before trying to edit, in order to avoid error
				 * "a0040203".
				 */
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					log.debug("Sleeping for " + sleepTime
							+ "ms was interrupted.", e);
					/*
					 * Logging is enough here. Sleeping is only for improving
					 * performance. So if it fails, just don't care.
					 */
				}
			}

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

			log.info("Added Track #" + trackIndex + ": " + artistName + " - "
					+ trackName + ": created=" + dateCreate + "; lastPlayed="
					+ lastPlayTime + "; lastSkipTime=" + lastSkipTime
					+ "; playCount=" + playCount + "; rating=" + rating
					+ "; skipCount=" + skipCount + "; path=" + absolutePath);
		} catch (URISyntaxException e) {
			// TODO handle
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.warn(
					"File not added by iTunes. Corrupt or missing? Skipping file: "
							+ sbTrack.getContentUrl(), e);
		} catch (ComException e) {
			sleepTime = handlea0040203(iTunes, trackIndex, sbTrack, nRetries,
					sleepTime, e);
		} finally {
			/*
			 * Dispose all COM objects. This must be done in order to avoid
			 * iTunes throwing "a0040203" exceptions.
			 */
			// TODO this seems not to solve the problem
			// comCollector.disposeAll();
		}
		return sleepTime;
	}

	/**
	 * Handles "a0040203" (not modifiable) errors in iTunes by increasing sleep
	 * time. By retrying to add the track (
	 * {@link #addTrack(ITunes, ComObjectCollector, int, MediaItem, int, int)}).
	 * Also the sleep time is increased.
	 * 
	 * @param iTunes
	 * @param trackIndex
	 * @param sbTrack
	 * @param nRetries
	 * @param sleepTime
	 * @param e
	 * 
	 * @return the new sleep time
	 * 
	 * @throws ITunesException
	 *             after {@link #A0040203_RETRIES} unsuccessful retries.
	 * @throws ComException
	 *             <code>e</code> is re-thrown when not an a0040203
	 */
	private int handlea0040203(ITunes iTunes, int trackIndex,
			MediaItem sbTrack, int nRetries, int sleepTime, ComException e)
			throws ITunesException {
		if (!e.getMessage().contains("a0040203")) {
			// Rethrow
			throw e;
		}
		/*
		 * Special case: a0040203 (not modifiable) This exception might occur
		 * right after a track has been added but iTunes (for some reasons)
		 * won't let us modify it for some more seconds. Maybe it parses artwork
		 * or goes fishing.
		 * 
		 * After handling the exception (~ 500ms) modifying the track most
		 * likely work, because iTunes seems to have finished what it did
		 * before. In order to improve performance, we will adaptively start
		 * sleeping after each added track. This should improve performance,
		 * because we won't sleep as long as handling exceptions would take.
		 */
		if (nRetries > 0) {
			if (sleepTime < A0040203_SLEEP_TIME_MAX) {
				// Increase sleep time after each "a0040203" exception
				sleepTime += A0040203_SLEEP_TIME_INCREMENT;
			}
			log.debug(
					"Track was added, but error setting attributes. Retrying "
							+ nRetries + " more times. Sleep time=" + sleepTime
							+ ". File: " + sbTrack.getContentUrl(), e);

			sleepTime = addTrack(iTunes, trackIndex, sbTrack, nRetries - 1,
					sleepTime);
		} else {
			throw new ITunesException("Unable to modify track. Tried "
					+ A0040203_RETRIES + " times without luck. File: "
					+ sbTrack.getContentUrl(), e);
		}
		return sleepTime;
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
}
