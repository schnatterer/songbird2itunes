package info.schnatterer.songbird2itunes;

import info.schnatterer.songbird2itunes.Songbird2itunes.Statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

/**
 * Entry point for the songbird2Itunes Command Line Application
 */
public class Songbird2itunesApp {
	/** SLF4J-Logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		System.exit(new Songbird2itunesApp().run(args));
	}

	/**
	 * @param args
	 * @return 0 on success; 1 on command line parameters error; 2 on error on
	 *         songbird 2 iTunes conversion.
	 */
	int run(String[] args) {
		/* Parse command line arguments/parameter (command line interface) */
		int ret = 0; // Presume success
		Songbird2itunesCli cliParams = null;

		try {
			cliParams = readParams(args);
			if (cliParams != null) {
				// Successfully read command line params
				Statistics stats = createSongbird2itunes().convert(
						cliParams.getPath(), cliParams.getRetries(),
						cliParams.isDateAddedWorkaround());
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
			}
		} catch (ParameterException e) {
			log.error("Error parsing command line arguments.");
			ret = 1;
		} catch (Exception e) { // Outmost "catch all" block for logging any
			// exception exiting application with error
			log.error("Conversion failed with error \"" + e.getMessage()
					+ "\". Please see log file.", e);
			ret = 2;
		}
		return ret;
	}

	/**
	 * Delegates to CLI object. Useful for testing.
	 * 
	 * @param argv
	 * @return an instance of {@link Songbird2itunesCli} when everything went
	 *         ok, or <code>null</code> if "-- help" was called.
	 * 
	 * @throws ParameterException
	 *             when something went wrong
	 */
	Songbird2itunesCli readParams(String[] args) {
		return Songbird2itunesCli.readParams(args, "songbird2itunes");
	}

	/**
	 * @return a new instance of {@link Songbird2itunes}. Useful for testing.
	 */
	Songbird2itunes createSongbird2itunes() {
		return new Songbird2itunes();
	}
}
