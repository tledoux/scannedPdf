package fr.bnf.toolslab;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Counter to classify the different values.
 * In order to group the value, only he multiple of 10 are kept.
 *
 */
public class DpiCounter {

	Map<Integer, Integer> dpis = new HashMap<>();

	private int getRoundedValue(int value) {
		return (value / 10) * 10;
	}

	public void increment(int dpi) {
		int rounded = getRoundedValue(dpi);
		Integer previous = dpis.get(rounded);
		if (previous == null) {
			dpis.put(rounded, 1);
		} else {
			dpis.put(rounded, previous.intValue() + 1);
		}
	}

	public int get(int dpi) {
		Integer previous = dpis.get(getRoundedValue(dpi));
		if (previous == null) {
			return 0;
		}
		return previous.intValue();
	}

	public Entry<Integer, Integer> getBest() {
		// Find the most usual dpi
		int maxNumber = 0;
		Entry<Integer, Integer> best = new AbstractMap.SimpleEntry<>(0, 0);
		for (Entry<Integer, Integer> e : dpis.entrySet()) {
			if (e.getValue().intValue() > maxNumber) {
				best = new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
				maxNumber = e.getValue().intValue();
			}
		}
		return best;
	}

	public String toString() {
		List<String> parts = new ArrayList<>();
		for (Entry<Integer, Integer> e : dpis.entrySet()) {
			parts.add("DPI(" + e.getKey() + ")=" + e.getValue());
		}
		return String.join("; ",parts);
		
		
	}
}
