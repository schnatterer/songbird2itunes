package info.schnatterer.java.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Implementation of basic set (in a mathematical sense) operations.
 * 
 * @author schnatterer
 *
 */
public class Sets {

	/**
	 * Returns the intersection of two sets, as stream. The streams might have
	 * different types. The result might contain duplicates.
	 * 
	 * @param a
	 *            set a, of type <code>T</code>
	 * @param b
	 *            set b, of <b>other</b> type <code>S</code>. Recommendation:
	 *            Use a {@link HashSet} here, as a lot of
	 *            {@link Collection#contains(Object)} is called
	 * @param convertAtoB
	 *            converts elements of <code>a</code> to be comparable with type
	 *            <code>b</code>
	 * @return a new instance that contains the intersection between a and b
	 */
	public static <T, S> Stream<T> intersection(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return a.stream().filter(
				aMember -> b.contains(convertAtoB.apply(aMember)));
	}

	/**
	 * Returns the relative complement of b in a ("a without b"). The streams
	 * might have different types. The result might contain duplicates.
	 * 
	 * @param a
	 *            set a, of type <code>T</code>
	 * @param b
	 *            set b, of <b>other</b> type <code>S</code>. Recommendation:
	 *            Use a {@link HashSet} here, as a lot of
	 *            {@link Collection#contains(Object)} is called
	 * @param convertAtoB
	 *            converts elements of <code>a</code> to be comparable with type
	 *            <code>b</code>
	 * @return a new instance that contains the intersection between a and b
	 */
	public static <T, S> Stream<T> relativeComplement(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return a.stream().filter(
				aMember -> !b.contains(convertAtoB.apply(aMember)));
	}
}
