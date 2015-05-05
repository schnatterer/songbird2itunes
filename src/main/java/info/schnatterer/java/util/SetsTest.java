package info.schnatterer.java.util;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class SetsTest {

	public static final StringWrapper SW1 = new StringWrapper("1");
	public static final StringWrapper SW2 = new StringWrapper("2");
	public static final StringWrapper SW3 = new StringWrapper("3");
	public static final StringWrapper SW4 = new StringWrapper("4");
	public static final StringWrapper SW5 = new StringWrapper("5");
	public static final StringWrapper SW6 = new StringWrapper("6");
	public static final StringWrapper SW7 = new StringWrapper("7");

	private static final List<StringWrapper> STRING_WRAPPER_1_5 = Arrays
			.asList(new StringWrapper[] { SW1, SW2, SW3, SW4, SW5 });
	private static final List<String> STRING_3_7 = Arrays.asList(new String[] {
			"3", "4", "5", "6", "7" });

	private static final List<StringWrapper> A_DUPLICATES = Arrays
			.asList(new StringWrapper[] { SW1, SW2, SW3, SW3, SW4, SW5 });
	@SuppressWarnings("serial")
	private static final Set<StringWrapper> EXPECTED_INTERSECTION_STRING_WRAPPER = new HashSet<StringWrapper>() {
		{
			add(SW3);
			add(SW4);
			add(SW5);
		}
	};
	@SuppressWarnings("serial")
	private static final List<StringWrapper> EXPECTED_INTERCEPTION_DUPLICATES_STRING_WRAPPER = new LinkedList<StringWrapper>() {
		{
			addAll(EXPECTED_INTERSECTION_STRING_WRAPPER);
			add(SW3);
		}
	};

	/**
	 * Test for
	 * {@link Sets#intersectionSet(java.util.Collection, java.util.Collection, java.util.function.Function)}
	 * .
	 */
	@Test
	public void testIntersectionSet() {
		Set<StringWrapper> actual = Sets.intersectionSet(STRING_WRAPPER_1_5,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertEquals("intersection returned unexpected result",
				EXPECTED_INTERSECTION_STRING_WRAPPER, actual);
	}

	/**
	 * Test for
	 * {@link Sets#intersectionSet(java.util.Collection, java.util.Collection, java.util.function.Function)}
	 * that contains duplicates.
	 */
	@Test
	public void testIntersectioSetDuplicate() {
		Set<StringWrapper> actual = Sets.intersectionSet(A_DUPLICATES,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertEquals("intersection returned unexpected result",
				EXPECTED_INTERSECTION_STRING_WRAPPER, actual);
	}

	/**
	 * 
	 * Test for
	 * {@link Sets#intersectionList(java.util.Collection, java.util.Collection, java.util.function.Function)}
	 * .
	 */
	@Test
	public void testIntersectionList() {
		List<StringWrapper> actual = Sets.intersectionList(STRING_WRAPPER_1_5,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertThat("intersection returned unexpected result", actual,
				containsInAnyOrder(EXPECTED_INTERSECTION_STRING_WRAPPER
						.toArray()));
	}

	/**
	 * Test for
	 * {@link Sets#intersectionList(java.util.Collection, java.util.Collection, java.util.function.Function)
	 * *} that contains duplicates.
	 */
	@Test
	public void testIntersectionListDuplicate() {
		List<StringWrapper> actual = Sets.intersectionList(A_DUPLICATES,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertThat(
				"intersection returned unexpected result",
				actual,
				containsInAnyOrder(EXPECTED_INTERCEPTION_DUPLICATES_STRING_WRAPPER
						.toArray()));
	}

	/**
	 * 
	 * Test for
	 * {@link Sets#intersectionList(java.util.Collection, java.util.Collection, java.util.function.Function)}
	 * .
	 */
	@Test
	public void testIntersectionListDistinct() {
		List<StringWrapper> actual = Sets.intersectionList(STRING_WRAPPER_1_5,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertThat("intersection returned unexpected result", actual,
				containsInAnyOrder(EXPECTED_INTERSECTION_STRING_WRAPPER
						.toArray()));
	}

	/**
	 * Test for
	 * {@link Sets#intersectionList(java.util.Collection, java.util.Collection, java.util.function.Function)
	 * *} that contains duplicates.
	 */
	@Test
	public void testIntersectionListDistinctDuplicate() {
		List<StringWrapper> actual = Sets.intersectionList(A_DUPLICATES,
				STRING_3_7, aMember -> aMember.getWrapped());
		assertThat(
				"intersection returned unexpected result",
				actual,
				containsInAnyOrder(EXPECTED_INTERCEPTION_DUPLICATES_STRING_WRAPPER
						.toArray()));
	}

	/**
	 * Test for
	 * {@link Sets#relativeComplement(java.util.Collection, java.util.Collection, java.util.function.Function)
	 * .
	 */
	@Test
	public void testRelativeComplementA() {
		List<StringWrapper> a = Arrays.asList(new StringWrapper[] { SW3, SW4,
				SW5, SW6, SW7 });
		List<String> b = Arrays
				.asList(new String[] { "1", "2", "3", "4", "5" });

		List<StringWrapper> expected = Arrays.asList(new StringWrapper[] { SW6,
				SW7 });

		List<StringWrapper> actual = Sets.relativeComplement(a, b,
				aMember -> aMember.getWrapped()).collect(Collectors.toList());
		assertThat("relative complement of a in b returned unexpected result",
				actual, containsInAnyOrder(expected.toArray()));
	}

	/**
	 * Simple class that does not equal a string, but can be easily converted.
	 * 
	 * @author schnatterer
	 *
	 */
	private static class StringWrapper {
		private String wrapped;

		public StringWrapper(String wrapped) {
			this.wrapped = wrapped;
		}

		public String getWrapped() {
			return wrapped;
		}

		@Override
		public String toString() {
			return wrapped.toString();
		}
	}
}
