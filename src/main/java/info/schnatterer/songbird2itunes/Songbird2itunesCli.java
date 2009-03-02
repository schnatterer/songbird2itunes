package info.schnatterer.songbird2itunes;

import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * The Songbird2itunesCli command line interface takes care of parsing the
 * arguments and printing out potential errors
 * 
 * @author schnatterer
 * 
 */
public class Songbird2itunesCli {

	// Don't instantiate!
	private Songbird2itunesCli() {
	}

	/**
	 * Using the {@link JCommander} framework to parse parameters.
	 * 
	 * See http://jcommander.org/
	 */
	private JCommander commander = null;

	private static final String EOL = System.getProperty("line.separator");

	/** Description for main parameter - path to songbird database. */
	private static final String DESC_DB = "Path to songbird database file";

	private static final String DESC_RETR = "(optional) Number of retries after an iTunes error";
	private static final String DESC_DATE_ADDED = "(optional) workaround for converting the date added to iTunes. NOTE: This requires admin rights and set your system date before adding each track. Use with extreme care.";
	private static final String DESC_HELP = "(optional) Show this message";

	/**
	 * Reads the command line parameters and prints error messages when
	 * something went wrong
	 * 
	 * @param argv
	 * @return an instance of {@link Songbird2itunesCli} when everything went
	 *         ok, or <code>null</code> if "-- help" was called.
	 * 
	 * @throws ParameterException
	 *             when something went wrong
	 */
	public static Songbird2itunesCli readParams(String[] argv,
			String programmName) throws ParameterException {
		Songbird2itunesCli cliParams = new Songbird2itunesCli();
		try {
			cliParams.commander = new JCommander(cliParams);
			cliParams.commander.setProgramName(programmName);
			cliParams.commander.parse(argv);
		} catch (ParameterException e) {
			// Print err
			StringBuilder errStr = new StringBuilder(e.getMessage() + EOL);
			cliParams.commander.usage(errStr, "  ");
			System.err.println(errStr.toString());
			// Rethrow, so the main application knows something went wrong
			throw e;
		}

		if (cliParams.help == true) {
			cliParams.commander.usage();
			cliParams = null;
		}

		return cliParams;
	}

	/** Definition of parameter - main parameter (path to songbird db). */
	@Parameter(required = true, arity = 1, description = DESC_DB)
	private List<String> mainParams;

	@Parameter(names = { "-r", "--retries" }, description = DESC_RETR)
	private final Integer retries = 50;

	@Parameter(names = { "-d", "--dateadded" }, description = DESC_DATE_ADDED)
	private boolean dateAddedWorkaround = false;

	@Parameter(names = "--help", help = true, description = DESC_HELP)
	private boolean help;

	/**
	 * @return the main parameter - path to songbird database
	 */
	public String getPath() {
		return mainParams.get(0);
	}

	/**
	 * @return the retries
	 */
	public Integer getRetries() {
		return retries;
	}

	/**
	 * @return the dateAddedWorkaround
	 */
	public boolean isDateAddedWorkaround() {
		return dateAddedWorkaround;
	}
}
