package fr.bnf.toolslab;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Counter to classify the different values. In order to group the values, only multiples of 10 are
 * kept, using the nearest value.
 *
 */
public class DpiCounter {

  Map<Integer, Integer> dpis = new HashMap<>();

  /**
   * Get the nearest ten.
   *
   * @param value value to round
   * @return rounded value
   */
  private int getRoundedValue(int value) {
    return ((value + 5) / 10) * 10;
  }

  public void increment(int dpi) {
    int rounded = getRoundedValue(dpi);
    dpis.merge(Integer.valueOf(rounded), 1, Integer::sum);
  }

  public int get(int dpi) {
    return dpis.getOrDefault(dpi, 0);
  }

  /**
   * Return the most usual dpi.
   *
   * @return Entry containing the most usual dpi and the number of occurrences
   */
  public Entry<Integer, Integer> getBest() {
    final Comparator<Map.Entry<Integer, Integer>> valueComparator =
        Map.Entry.comparingByValue(Comparator.reverseOrder());
    final Entry<Integer, Integer> defaultValue = new AbstractMap.SimpleEntry<>(0, 0);
    return dpis.entrySet().stream().sorted(valueComparator).findFirst()
        .orElseGet(() -> defaultValue);
  }

  @Override
  public String toString() {
    final Comparator<Map.Entry<Integer, Integer>> keyComparator =
        Map.Entry.comparingByKey(Comparator.naturalOrder());
    return dpis.entrySet().stream().sorted(keyComparator)
        .map(e -> "DPI(" + e.getKey() + ")=" + e.getValue()).collect(Collectors.joining("; "));
  }
}
