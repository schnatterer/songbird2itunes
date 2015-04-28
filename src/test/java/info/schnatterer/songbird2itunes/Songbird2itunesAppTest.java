package info.schnatterer.songbird2itunes;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import info.schnatterer.itunes4j.exception.ITunesException;
import info.schnatterer.songbird2itunes.migration.Songbird2itunesMigration;
import info.schnatterer.songbird2itunes.migration.Songbird2itunesMigration.Statistics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.tools.ant.types.Commandline;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class Songbird2itunesAppTest {

	@Rule
	public MockitoRule rule = MockitoJUnit.rule();

	@Mock
	private Songbird2itunesMigration s2iMock;

	private final Songbird2itunesApp classUnderTest = new Songbird2itunesApp4Test(
			"yes");

	/**
	 * Asserts the proper return code when calling with --help parameters.
	 */
	@Test
	public void help() {
		assertEquals("Running with help parameter returned unexpected result",
				Songbird2itunesApp.EXIT_SUCCESS,
				classUnderTest.run(new String[] { "--help" }));
	}

	/**
	 * Asserts the proper return code when calling with all parameters and that
	 * parameters are passed properly.
	 * 
	 * @throws ITunesException
	 * @throws SQLException
	 */
	@Test
	public void allParams() throws SQLException, ITunesException {
		when(s2iMock.migrate(anyString(), anyInt(), anyBoolean())).thenReturn(
				new Statistics());
		assertEquals("Running with all parameters returned unexpected result",
				0, classUnderTest.run(Commandline
						.translateCommandline("-r 23 -d path")));
		verify(s2iMock).migrate("path", 23, Boolean.TRUE);
	}

	/**
	 * Asserts proper return code when an exception is thrown during parameter
	 * handling.
	 * 
	 * @throws ITunesException
	 * @throws SQLException
	 */
	@Test
	public void invalidParams() throws SQLException, ITunesException {
		assertEquals(
				"Running with invalid parameters returned unexpected result",
				Songbird2itunesApp.EXIT_INVALID_PARAMS,
				classUnderTest.run(new String[] {}));
	}

	/**
	 * Asserts proper return code when an exception is thrown during conversion.
	 * 
	 * @throws ITunesException
	 * @throws SQLException
	 */
	@Test
	public void errorConversion() throws SQLException, ITunesException {
		when(s2iMock.migrate(anyString(), anyInt(), anyBoolean())).thenThrow(
				new RuntimeException("Mocked exception"));
		assertEquals("Error during conversoin returned unexpected result",
				Songbird2itunesApp.EXIT_ERROR_CONVERSION,
				classUnderTest.run(Commandline
						.translateCommandline("-r 23 -d path")));
	}

	/**
	 * Asserts proper return code and behavior when user does not confirm usage
	 * of date added workaround.
	 * 
	 * @throws ITunesException
	 * @throws SQLException
	 */
	@Test
	public void notConfirmedWorkaround() throws SQLException, ITunesException {
		when(s2iMock.migrate(anyString(), anyInt(), anyBoolean())).thenReturn(
				new Statistics());
		Songbird2itunesApp classUnderTestNoConfirmation = new Songbird2itunesApp4Test(
				"no confirm");
		assertEquals(
				"Denying confirmation for using the workaround returned unexpected result",
				Songbird2itunesApp.EXIT_SUCCESS, classUnderTestNoConfirmation
						.run(Commandline.translateCommandline("-r 23 -d path")));
		verify(s2iMock, never()).migrate(anyString(), anyInt(), anyBoolean());
	}

	private class Songbird2itunesApp4Test extends Songbird2itunesApp {
		private String confirmationString;

		public Songbird2itunesApp4Test(String confirmationString) {
			this.confirmationString = confirmationString;
		}

		@Override
		Songbird2itunesMigration createSongbird2itunes() {
			return s2iMock;
		}

		@Override
		InputStream createSystemIn() {
			return new ByteArrayInputStream(new String(confirmationString
					+ "\n").getBytes());
		}
	}
}
