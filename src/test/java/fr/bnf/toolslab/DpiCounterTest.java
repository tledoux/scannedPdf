package fr.bnf.toolslab;

import static org.junit.Assert.assertEquals;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;

public class DpiCounterTest {
  protected static Logger LOGGER = Logger.getLogger(DpiCounterTest.class.getName());

  DpiCounter counter;

  @Before
  public void setUp() throws Exception {
    counter = new DpiCounter();
  }

  @Test
  public void testIncrement() {
    LOGGER.info(counter.toString());
    assertEquals(0, counter.get(200));

    counter.increment(96);
    LOGGER.info(counter.toString());
    assertEquals(1, counter.get(100));
    assertEquals(0, counter.get(200));

    counter.increment(204);
    LOGGER.info(counter.toString());
    assertEquals(1, counter.get(100));
    assertEquals(1, counter.get(200));

    counter.increment(102);
    LOGGER.info(counter.toString());
    assertEquals(2, counter.get(100));
    assertEquals(1, counter.get(200));
  }

  @Test
  public void testGet() {
    assertEquals(0, counter.get(200));
    counter.increment(104);
    assertEquals(0, counter.get(200));
    assertEquals(1, counter.get(100));
    counter.increment(199);
    counter.increment(201);
    assertEquals(2, counter.get(200));
    assertEquals(1, counter.get(100));
    assertEquals(0, counter.get(210));
  }

  @Test
  public void testGetBest() {
    Entry<Integer, Integer> best1 = counter.getBest();
    LOGGER.info(counter.toString());
    assertEquals(0, best1.getKey().intValue());

    // All 110
    counter.increment(105);
    // All 200
    counter.increment(199);
    counter.increment(201);
    // All 100
    counter.increment(95);
    counter.increment(96);
    counter.increment(97);
    counter.increment(98);
    counter.increment(99);
    counter.increment(100);
    counter.increment(101);
    counter.increment(102);
    counter.increment(103);
    counter.increment(104);
    Entry<Integer, Integer> best2 = counter.getBest();
    LOGGER.info(counter.toString());
    assertEquals(100, best2.getKey().intValue());
    assertEquals(10, best2.getValue().intValue());
  }

  @Test
  public void testtoString() {
    assertEquals("", counter.toString());

    counter.increment(105);
    assertEquals("DPI(110)=1", counter.toString());
    counter.increment(199);
    assertEquals("DPI(110)=1; DPI(200)=1", counter.toString());
    counter.increment(97);
    assertEquals("DPI(100)=1; DPI(110)=1; DPI(200)=1", counter.toString());
    counter.increment(201);
    assertEquals("DPI(100)=1; DPI(110)=1; DPI(200)=2", counter.toString());
    counter.increment(95);
    assertEquals("DPI(100)=2; DPI(110)=1; DPI(200)=2", counter.toString());
  }

}
