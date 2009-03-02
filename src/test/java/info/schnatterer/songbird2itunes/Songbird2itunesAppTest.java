package info.schnatterer.songbird2itunes;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import info.schnatterer.itunes4j.ITunesException;
import info.schnatterer.songbird2itunes.Songbird2itunes.Statistics;

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
	private Songbird2itunes s2iMock;

	private final Songbird2itunesApp classUnderTest = new Songbird2itunesApp4Test();

	/**
	 * Asserts the proper return code when calling with --help parameters.
	 */
	@Test
	public void help() {
		assertEquals("Running with help parameter returned unexpected result",
				0, classUnderTest.run(new String[] { "--help" }));
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
		when(s2iMock.convert(anyString(), anyInt(), anyBoolean())).thenReturn(
				new Statistics());
		assertEquals("Running with all parameters returned unexpected result",
				0, classUnderTest.run(Commandline
						.translateCommandline("-r 23 -d path")));
		verify(s2iMock).convert("path", 23, Boolean.TRUE);
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
				1, classUnderTest.run(new String[] {}));
	}

	/**
	 * Asserts proper return code when an exception is thrown during conversion.
	 * 
	 * @throws ITunesException
	 * @throws SQLException
	 */
	@Test
	public void errorConversion() throws SQLException, ITunesException {
		when(s2iMock.convert(anyString(), anyInt(), anyBoolean())).thenThrow(
				new RuntimeException("Mocked exception"));
		assertEquals("Error during conversoin returned unexpected result", 2,
				classUnderTest.run(Commandline
						.translateCommandline("-r 23 -d path")));
	}

	private class Songbird2itunesApp4Test extends Songbird2itunesApp {
		@Override
		Songbird2itunes createSongbird2itunes() {
			return s2iMock;
		}

	}
}
