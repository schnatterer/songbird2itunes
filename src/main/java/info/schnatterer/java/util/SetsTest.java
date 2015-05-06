package info.schnatterer.java.util;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
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

	/**
	 * Test for
	 * {@link Sets#intersection(java.util.Collection, java.util.Collection, java.util.function.Function)}
	 * .
	 */
	@Test
	public void testIntersectionSet() {
		List<StringWrapper> a = Arrays.asList(new StringWrapper[] { SW1, SW2,
				SW3, SW4, SW5 });
		List<String> b = Arrays
				.asList(new String[] { "3", "4", "5", "6", "7" });
		List<StringWrapper> expected = Arrays.asList(new StringWrapper[] { SW3,
				SW4, SW5 });

		List<StringWrapper> actual = Sets.intersection(a, b,
				aMember -> aMember.getWrapped()).collect(Collectors.toList());
		assertEquals("intersection returned unexpected result", expected,
				actual);
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
