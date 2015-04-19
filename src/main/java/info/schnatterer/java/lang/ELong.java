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

	public ELong(java.lang.Long wrappedLong) {
		this.wrappedLong = wrappedLong;
	}

	/**
	 * Safely casts a {@link ELong} to an {@link Integer}. If {@link ELong}
	 * cannot fit into an {@link Integer} an exception is thrown.
	 * 
	 * @param l
	 *            value to convert. Can be <code>null</code>
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
