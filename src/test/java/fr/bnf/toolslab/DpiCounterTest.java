package fr.bnf.toolslab;

import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

public class DpiCounterTest {
	protected static Logger LOGGER = Logger.getLogger(DpiCounterTest.class
			.getName());

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testIncrement() {
		DpiCounter test1 = new DpiCounter();
		LOGGER.info(test1.toString());
		assertEquals(0, test1.get(200));

		test1.increment(100);
		LOGGER.info(test1.toString());
		assertEquals(1, test1.get(100));
		assertEquals(0, test1.get(200));

		test1.increment(205);
		LOGGER.info(test1.toString());
		assertEquals(1, test1.get(100));
		assertEquals(1, test1.get(200));

		test1.increment(102);
		LOGGER.info(test1.toString());
		assertEquals(2, test1.get(100));
		assertEquals(1, test1.get(200));
	}

	@Test
	public void testGetBest() {
		DpiCounter test1 = new DpiCounter();
		Entry<Integer, Integer> best1 = test1.getBest();
		LOGGER.info(test1.toString());
		assertEquals(0, best1.getKey().intValue());

		test1.increment(101);
		test1.increment(102);
		test1.increment(103);
		test1.increment(104);
		test1.increment(105);
		test1.increment(200);
		test1.increment(200);
		Entry<Integer, Integer> best2 = test1.getBest();
		LOGGER.info(test1.toString());
		assertEquals(100, best2.getKey().intValue());
		assertEquals(5, best2.getValue().intValue());
	}

}
