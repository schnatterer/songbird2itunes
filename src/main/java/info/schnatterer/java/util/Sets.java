package info.schnatterer.java.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sets {

	/**
	 * Returns an intersection of the sets a and b, as set. Does not contain
	 * duplicates.
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
	 *         without duplicates
	 */
	public static <T, S> Set<T> intersectionSet(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return intersection(a, b, convertAtoB).collect(Collectors.toSet());
	}

	/**
	 * Returns an intersection of the sets a and b, as list. Might contain
	 * duplicates.
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
	public static <T, S> List<T> intersectionList(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return intersection(a, b, convertAtoB).collect(Collectors.toList());
	}

	/**
	 * Returns an intersection of the sets a and b, as list. Does not contain
	 * duplicates.
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
	 *         without duplicates
	 */
	public static <T, S> List<T> intersectionListDistinct(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return intersection(a, b, convertAtoB).distinct().collect(
				Collectors.toList());
	}

	/**
	 * Returns an intersection of the sets a and b, as stream. Might contain
	 * duplicates.
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
	 * The relative complement of B in A
	 * 
	 * @param a
	 * @param b
	 * @param convertAtoB
	 * @return
	 */
	public static <T, S> Stream<T> relativeComplement(Collection<T> a,
			Collection<S> b, Function<T, S> convertAtoB) {
		return a.stream().filter(
				aMember -> !b.contains(convertAtoB.apply(aMember)));
	}
}
