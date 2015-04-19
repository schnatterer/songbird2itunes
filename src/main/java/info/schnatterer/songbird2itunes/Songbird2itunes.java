package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.ITunes;
import info.schnatterer.itunes4j.entity.Rating;
import info.schnatterer.itunes4j.entity.Track;
import info.schnatterer.itunes4j.exception.ITunesException;
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

import com4j.COM4J;
import com4j.util.ComObjectCollector;

public class Songbird2itunes {
	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public void convert(String songbirdDbFile) throws SQLException,
			ITunesException {

		File file = new File(songbirdDbFile);

		// Create database wrapper instance
		SongbirdDb songbirdDb = createSongbirdDb(file);
		// Create reference to iTunes
		ITunes iTunes = createItunes();
		/*
		 * Collectors keeps track of COM objects and can be used to dispose
		 * them. As it is created now, the iTunes references is not listed and
		 * not disposed when col.disposeAll() is called.
		 */
		ComObjectCollector comCollector = new ComObjectCollector();
		COM4J.addListener(comCollector);

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

			Track iTunesTrack = null;
			try {
				String absolutePath = new File(new URI(sbTrack.getContentUrl()))
						.getAbsolutePath();
				iTunesTrack = iTunes.addFile(absolutePath);

				Date dateCreate = sbTrack.getDateCreated();

				String trackName = sbTrack
						.getProperty(Property.PROP_TRACK_NAME);
				String artistName = sbTrack
						.getProperty(Property.PROP_ARTIST_NAME);

				Date lastPlayTime = sbTrack
						.getPropertyAsDate(Property.PROP_LAST_PLAY_TIME);
				Date lastSkipTime = sbTrack
						.getPropertyAsDate(Property.PROP_LAST_SKIP_TIME);
				Long playCount = sbTrack
						.getPropertyAsLong(Property.PROP_PLAY_COUNT);
				Long rating = sbTrack.getPropertyAsLong(Property.PROP_RATING);
				Long skipCount = sbTrack
						.getPropertyAsLong(Property.PROP_SKIP_COUNT);

				log.info("Added Track #" + i + ": " + artistName + " - "
						+ trackName + ": created=" + dateCreate
						+ "; lastPlayed=" + lastPlayTime + "; lastSkipTime="
						+ lastSkipTime + "; playCount=" + playCount
						+ "; rating=" + rating + "; skipCount=" + skipCount
						+ "; path=" + absolutePath);

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

				// TODO remove me, I'm only for first tests
				if (rating != null && rating > 0) {
					break;
				}
			} catch (URISyntaxException e) {
				// TODO handle
				throw new RuntimeException(e);
			} catch (IOException e) {
				log.warn(
						"iTunes did not add a file. Skipping file: "
								+ sbTrack.getContentUrl(), e);
			} finally {
				/*
				 * Dispose all COM objects. This must be done in order to avoid
				 * iTunes throwing "a0040203" exceptions.
				 */
				// TODO this seems not to solve the problem
				comCollector.disposeAll();
			}
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
