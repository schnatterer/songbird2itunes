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

/**
 * Extended Long - wrapper for {@link java.lang.Long} that provides extended
 * features.
 * 
 * @author schnatterer
 *
 */
public class ELong {

	private final java.lang.Long wrappedLong;

	/**
	 * Creates a new extended long from a standard java boxed long.
	 * 
	 * @param wrappedLong
	 *            the java long instance to wrap
	 */
	public ELong(java.lang.Long wrappedLong) {
		this.wrappedLong = wrappedLong;
	}

	/**
	 * Safely casts a {@link ELong} to an {@link Integer}. If {@link ELong}
	 * cannot fit into an {@link Integer} an exception is thrown.
	 * 
	 * @return an instance of <code>l</code> as {@link Integer} or
	 *         <code>null</code>
	 * @throws IllegalArgumentException
	 *             when <code>l</code> is less than {@link Integer#MIN_VALUE} or
	 *             greater than {@link Integer#MAX_VALUE}
	 */
	public Integer toInt() throws IllegalArgumentException {
		if (wrappedLong == null) {
			return null;
		}
		if (wrappedLong < Integer.MIN_VALUE || wrappedLong > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(wrappedLong
					+ " cannot be cast to int without changing its value.");
		}
		return Integer.valueOf(wrappedLong.intValue());
	}
}
