package fr.bnf.toolslab;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class DimensionInfoTest {

	DimensionInfo dim = new DimensionInfo(100, 100);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testApproximate() {
		DimensionInfo dim2 = new DimensionInfo(95, 95);
		assertTrue(dim.approximate(dim2, 10.0));
		assertTrue(dim.approximate(dim2, 5.0));
		assertFalse(dim.approximate(dim2, 2.0));
	}

	@Test
	public void testContains() {
		DimensionInfo dim2 = new DimensionInfo(95, 95);
		assertTrue(dim.contains(dim2));
		
		DimensionInfo dim3 = new DimensionInfo(105, 105);
		assertFalse(dim.contains(dim3));

		DimensionInfo dim4 = new DimensionInfo(105, 85);
		assertFalse(dim.contains(dim4));
	}
	
	@Test
	public void testToString() {
		assertEquals("[100x100]", dim.toString());
	}
}
