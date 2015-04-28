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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.apache.tools.ant.types.Commandline;
import org.junit.Test;

import com.beust.jcommander.ParameterException;

public class Songbird2itunesCliTest {

	private static final Integer DEFAULT_RETRIES = 50;

	/** Calls CLI with minimal required parameters. */
	@Test
	public void minimal() {
		Songbird2itunesCli args = parseArgs("path");
		assertEquals("Unexpected main parameter", "path", args.getPath());
		// Assert defaults
		assertEquals("Unexpected default for parameter retries",
				DEFAULT_RETRIES, args.getRetries());
		assertFalse("Unexpected default for parameter isDateAddedWorkaround",
				args.isDateAddedWorkaround());
	}

	/** Calls CLI with multiple main parameters. */
	@Test
	public void multipleMainParams() {
		Songbird2itunesCli args = parseArgs("path path2");
		// Ignore path2
		assertEquals("Unexpected main parameter", "path", args.getPath());
		// Assert defaults
		assertEquals("Unexpected default for parameter retries",
				DEFAULT_RETRIES, args.getRetries());
		assertFalse("Unexpected default for parameter isDateAddedWorkaround",
				args.isDateAddedWorkaround());
	}

	/** Calls CLI with --help parameter. */
	@Test
	public void help() {
		assertNull("Calling with help parameter returned unexepcted result",
				parseArgs("--help"));
	}

	/** Calls CLI with no main parameter. */
	@Test(expected = ParameterException.class)
	public void noMain() {
		parseArgs("-r " + DEFAULT_RETRIES);
	}

	/**
	 * Convenience method that takes just the parameters passed to CLI splits
	 * them command-line-style (to {@link String} array), hands them to
	 * {@link Songbird2itunesCli#readParams(String[], String)} and returns the
	 * result.
	 * 
	 * @param args
	 *            the args to pass to CLI
	 * 
	 * @return the {@link Songbird2itunesCli} object for validation
	 */
	private Songbird2itunesCli parseArgs(String args) {
		String[] translateCommandline = Commandline.translateCommandline(args);
		return Songbird2itunesCli.readParams(translateCommandline, "prog name");
	}

}
