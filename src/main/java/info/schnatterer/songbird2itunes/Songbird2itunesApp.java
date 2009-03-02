package info.schnatterer.songbird2itunes;

import info.schnatterer.songbird2itunes.Songbird2itunes.Statistics;

import java.io.InputStream;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

/**
 * Entry point for the songbird2Itunes Command Line Application
 */
public class Songbird2itunesApp {
	static final int EXIT_SUCCESS = 0;
	static final int EXIT_INVALID_PARAMS = 1;
	static final int EXIT_ERROR_CONVERSION = 2;

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
			cliParams = Songbird2itunesCli.readParams(args, "songbird2itunes");
			if (cliParams != null) {
				if (cliParams.isDateAddedWorkaround() && !confirmedWorkaround()) {
					return EXIT_SUCCESS;
				}
				// Successfully read command line params. Do conversion
				printStats(createSongbird2itunes().convert(cliParams.getPath(),
						cliParams.getRetries(),
						cliParams.isDateAddedWorkaround()));
				return EXIT_SUCCESS;
			}
		} catch (ParameterException e) {
			log.error("Error parsing command line arguments.");
			ret = EXIT_INVALID_PARAMS;
		} catch (Exception e) { // Outmost "catch all" block for logging any
			// exception exiting application with error
			log.error("Conversion failed with error \"" + e.getMessage()
					+ "\". Please see log file.", e);
			ret = EXIT_ERROR_CONVERSION;
		}
		return ret;
	}

	/**
	 * Writes statistics to log.
	 * 
	 * @param stats
	 *            statistics to write
	 */
	private void printStats(Statistics stats) {
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
	}

	/**
	 * Make user confirm to use the "date added workaround"
	 * 
	 * @return <code>true</code> if the user confirmed, <code>false</code>
	 *         otherwise
	 */
	private boolean confirmedWorkaround() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(createSystemIn());
			log.info("You used the option for using the workaround to set the date added in iTunes. As setting the date added in iTunes is not possible, this workaround sets the system clock to the desired date and then adds the song to iTunes. Make sure to");
			log.info(" - start songbird2itunes with administration rights,");
			log.info(" - either close iTunes or start iTunes as administrator, ");
			log.info(" - deactivate the automatic sync of windows with a time server for the progress of conversion to iTunes.");
			log.info("If you REALLY want to do this, type \"yes\". If not just press enter and restart without this option!");
			if ("yes".equals(scanner.nextLine())) {
				return true;
			}
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		return false;
	}

	/**
	 * @return a new instance of System.in. Useful for testing.
	 */
	InputStream createSystemIn() {
		return System.in;
	}

	/**
	 * @return a new instance of {@link Songbird2itunes}. Useful for testing.
	 */
	Songbird2itunes createSongbird2itunes() {
		return new Songbird2itunes();
	}
}
