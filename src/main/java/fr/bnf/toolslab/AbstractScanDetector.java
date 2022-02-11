package fr.bnf.toolslab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public abstract class AbstractScanDetector {
	protected static Logger LOGGER = Logger
			.getLogger(AbstractScanDetector.class.getName());

	protected static final int THRESHOLD = 2;
	protected static final int MAX_SAMPLES = 10;

	abstract void init(FileDescriptor fd);

	abstract void parse() throws IOException;

	/**
	 * Select nbSamples pages in a random fashion
	 * 
	 * @param nbSamples
	 *            number of samples to collect
	 * @param nbPages
	 *            total number of pages
	 * @return a list of selected pages
	 */
	List<Integer> pickSamples(int nbSamples, int nbPages) {
		final Random random = new Random();
		List<Integer> pagesToTest = new ArrayList<>();
		while (pagesToTest.size() < nbSamples) {
			int page = random.nextInt(nbPages);
			if (!pagesToTest.contains(page)) {
				pagesToTest.add(page);
			}
		}
		return pagesToTest;
	}

	/**
	 * @param dimImage
	 * @param dimPage
	 * @param userUnit
	 * @param width
	 */
	int findDensity(DimensionInfo dimImage, DimensionInfo dimPage,
			float userUnit) {
		if (!dimImage.contains(dimPage)) {
			return 0;
		}
		double dpiX = (double) dimImage.x / dimPage.x * 72.0 / userUnit;
		// double dpiY = (double) dimImage.y / dimPage.y * 72.0 /
		// userUnit;
		LOGGER.fine("Found density of " + (int) dpiX);

		return (int) dpiX;
	}

}