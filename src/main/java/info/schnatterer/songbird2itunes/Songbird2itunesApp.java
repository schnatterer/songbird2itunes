package info.schnatterer.songbird2itunes;

import info.schnatterer.itunes4j.ITunesException;
import info.schnatterer.songbird2itunes.Songbird2itunes.Statistics;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the songbird2Itunes Command Line Application
 */
public class Songbird2itunesApp {
	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws SQLException, ITunesException {
		new Songbird2itunesApp().run(args);
	}

	private void run(String[] args) {
		// TODO implement a CLI here
		try {
			Statistics stats = new Songbird2itunes().convert(args[0]);
			log.info("Finished converting.");
			log.info("Processed " + stats.getTracksProcessed()
					+ " tracks (total) of which " + stats.getTracksFailed()
					+ " failed.");
			log.info("Processed " + stats.getPlaylistsProcessed()
					+ " playlists of which " + stats.getPlaylistsFailed()
					+ " failed.");
			log.info("Processed " + stats.getPlaylistTracksProcessed()
					+ " tracks (playlist members) of which "
					+ stats.getPlaylistTracksFailed() + " failed.");
			log.info("See log file for more info");
		} catch (SQLException e) {
			// TODO handle
			throw new RuntimeException(e);
		} catch (ITunesException e) {
			// TODO handle
			throw new RuntimeException(e);
		}

	}
}
