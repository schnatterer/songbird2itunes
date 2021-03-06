/**
 * Copyright (C) 2015 Johannes Schnatterer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.schnatterer.songbird2itunes;

import info.schnatterer.java.util.jar.Jar;
import info.schnatterer.songbird2itunes.migration.Songbird2itunesMigration;
import info.schnatterer.songbird2itunes.migration.Songbird2itunesMigration.Statistics;

import java.io.IOException;
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
	static final String PROG_NAME = "songbird2itunes";

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

		printWelcomeMessage();

		try {
			cliParams = Songbird2itunesCli.readParams(args, PROG_NAME);
			if (cliParams != null) {
				if (cliParams.isDateAddedWorkaround() && !confirmedWorkaround()) {
					return EXIT_SUCCESS;
				}
				// Successfully read command line params. Do conversion
				printStats(createSongbird2itunes().migrate(cliParams.getPath(),
						cliParams.getRetries(),
						cliParams.isDateAddedWorkaround(),
						cliParams.getPlaylistNames(),
						cliParams.isPlaylistsOnly()));
				return EXIT_SUCCESS;
			}
		} catch (ParameterException e) {
			log.error("Error parsing command line arguments.");
			ret = EXIT_INVALID_PARAMS;
		} catch (Exception e) { /*
								 * Outmost "catch all" block for logging any
								 * exception exiting application with error
								 */
			log.error("Conversion failed with error \"" + e.getMessage()
					+ "\". Please see log file.", e);
			ret = EXIT_ERROR_CONVERSION;
		}
		return ret;
	}

	/**
	 * Writes a welcome message to the log/console, including a build number, if
	 * available.
	 */
	private void printWelcomeMessage() {
		String welcomeMessage = "Welcome to " + PROG_NAME;
		String buildNumber;
		try {
			buildNumber = Jar.getBuildNumberFromManifest();
			if (buildNumber != null) {
				welcomeMessage = welcomeMessage + " (" + buildNumber + ")";
			}
		} catch (IOException e) {
			// If something fails we just don't print the build number
		}
		log.info(welcomeMessage);
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
			log.info("You used the option for using the workaround to set the \"date added\" in iTunes. As iTunes does not allow setting the \"date added\", this workaround sets the system clock to the desired date and then adds the song to iTunes. Make sure to");
			log.info(" - start songbird2itunes with administration rights,");
			log.info(" - either close iTunes or start iTunes as administrator, ");
			log.info(" - deactivate the automatic sync of windows with a time server for the progress of conversion to iTunes.");
			log.info(" - better not use your computer while the migration is running, because the system date might be invalid.");
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
	 * @return a new instance of {@link Songbird2itunesMigration}. Useful for
	 *         testing.
	 */
	Songbird2itunesMigration createSongbird2itunes() {
		return new Songbird2itunesMigration();
	}
}
