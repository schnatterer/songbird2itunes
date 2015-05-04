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
package info.schnatterer.java.lang;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A java wrapper for the system clock. For now this will
 * <ul>
 * <li><b>only work on windows</b></li>
 * <li><b>only work when run as administrator</b></li>
 * </ul>
 * So better don't use this :)
 * 
 * @author schnatterer
 *
 */
public class SystemClock {
	/**
	 * Thread-safe holder for the {@link DateFormat} used for formatting the
	 * time values.
	 */
	private static ThreadLocal<DateFormat> dateFormatHolderTime = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("HH:mm");
			return dateFormat;
		}
	};
	/**
	 * Thread-safe holder for the {@link DateFormat} used for formatting the
	 * date values.
	 */
	private static ThreadLocal<DateFormat> dateFormatHolderDate = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
			return dateFormat;
		}
	};

	/**
	 * Sets the system clock synchronously (that is, by forking a process and
	 * waiting for its termination) without throwing exceptions. Any errors are
	 * logged.
	 * 
	 * @param date
	 *            the date and time to se
	 * 
	 * @throws SystemClockException
	 *             wraps all exceptions
	 */
	public void set(Date date) throws SystemClockException {
		// Windows "cmd /C date dd-MM-yy & time hh:mm:ss"
		synchronousExec("cmd /C date "
				+ dateFormatHolderDate.get().format(date) + " & time "
				+ dateFormatHolderTime.get().format(date));
		// Linux: "date -s MMddhhmm[[yy]yy]"
	}

	/**
	 * Resyncs the system clock with a time server.
	 * 
	 * @throws SystemClockException
	 *             wraps all exceptions
	 */
	public void resync() throws SystemClockException {
		synchronousExec("cmd /C w32tm /resync /force");
	}

	/**
	 * Higher level level wrapper for synchronous system calls. Forks a process
	 * and waits for the result. Any errors are uniformly wrapped in a
	 * {@link SystemClockException}.
	 * 
	 * @param execCommand
	 *            command to fork and execute
	 * 
	 * @throws SystemClockException
	 *             wraps all exceptions
	 */
	protected void synchronousExec(String execCommand)
			throws SystemClockException {
		try {
			Process proc = exec(execCommand);
			int exitValue = proc.waitFor();
			if (exitValue != 0) {
				throw new SystemClockException(
						"Setting the system clock failed (\"" + execCommand
								+ "\"): Exit value " + exitValue);
			}
		} catch (IOException | InterruptedException | RuntimeException e) {
			throw new SystemClockException(
					"Setting the system clock failed (\"" + execCommand
							+ "\") with exception: " + e.getMessage(), e);
		}
	}

	/**
	 * Low level wrapper for {@link Runtime#exec(String)}. Useful for testing.
	 * 
	 * @param execCommand
	 *            a specified system command.
	 * 
	 * @return A new {@link Process} object for managing the subprocess
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	private Process exec(String execCommand) throws IOException {
		return Runtime.getRuntime().exec(execCommand);
	}

	/**
	 * Wraps all kinds of system-specific exceptions that might occur when
	 * setting the system clock.
	 * 
	 * @author schnatterer
	 */
	public static class SystemClockException extends Exception {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new exception with the specified detail message and
		 * cause.
		 * <p>
		 * Note that the detail message associated with {@code cause} is
		 * <i>not</i> automatically incorporated in this exception's detail
		 * message.
		 *
		 * @param message
		 *            the detail message (which is saved for later retrieval by
		 *            the {@link #getMessage()} method).
		 * @param cause
		 *            the cause (which is saved for later retrieval by the
		 *            {@link #getCause()} method). (A <tt>null</tt> value is
		 *            permitted, and indicates that the cause is nonexistent or
		 *            unknown.)
		 */
		public SystemClockException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructs a new exception with the specified detail message. The
		 * cause is not initialized, and may subsequently be initialized by a
		 * call to {@link #initCause}.
		 *
		 * @param message
		 *            the detail message. The detail message is saved for later
		 *            retrieval by the {@link #getMessage()} method.
		 */
		public SystemClockException(String message) {
			super(message);
		}
	}
}
