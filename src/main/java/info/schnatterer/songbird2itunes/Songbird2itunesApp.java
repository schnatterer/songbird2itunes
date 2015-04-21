package info.schnatterer.songbird2itunes;

import info.schnatterer.songbird2itunes.Songbird2itunes.Statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the songbird2Itunes Command Line Application
 */
public class Songbird2itunesApp {
	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		System.exit(new Songbird2itunesApp().run(args));
	}

	private int run(String[] args) {
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
			return 0;
		} catch (Exception e) { // Outmost "catch all" block for logging any
								// exception exiting application with error
			log.error("Conversion failed with error \"" + e.getMessage()
					+ "\". Please see log file.", e);
			return 1;
		}

	}
}
