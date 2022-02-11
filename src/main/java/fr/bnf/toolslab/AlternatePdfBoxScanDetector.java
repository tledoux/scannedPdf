package fr.bnf.toolslab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class AlternatePdfBoxScanDetector extends AbstractScanDetector {
	protected static Logger LOGGER = Logger
			.getLogger(AlternatePdfBoxScanDetector.class.getName());

	FileDescriptor fd;
	int nbPages;
	int nbImages;
	int nbImagesInPage;
	List<DimensionInfo> pageDimensions;
	List<DimensionInfo> imageDimensions;

	public void init(FileDescriptor fd) {
		LOGGER.fine("Processing " + fd.getFile().getName());
		this.fd = fd;
		this.nbPages = 0;
		this.nbImages = 0;
		this.pageDimensions = new ArrayList<>();
		this.imageDimensions = new ArrayList<>();
	}

	public void parse() throws IOException {
		long beginTime = System.currentTimeMillis();
		try (PDDocument document = PDDocument.load(fd.getFile())) {
			this.nbPages = 0;
			this.nbImages = 0;
			fd.setValid(true);
			fd.setNbPages(nbPages);
			for (PDPage page : document.getPages()) {
				this.nbPages++;
				this.nbImages += parsePage(page, this.nbPages);
			}
			fd.setNbPages(this.nbPages);
			fd.setNbImages(this.nbImages);
			LOGGER.fine("First pass in "
					+ (System.currentTimeMillis() - beginTime));

			// First heuristic: compare the number of pages and the number of
			// images
			if (this.nbPages != this.nbImages) {
				fd.setScan(false);
				return;
			}
			assert (pageDimensions.size() == imageDimensions.size());

			// Second heuristic: pick some pages and look if the image covers
			// all the page
			int nbSamples = Math.min(nbPages, MAX_SAMPLES);
			List<Integer> pagesToTest = pickSamples(nbSamples, nbPages);

			// Classify all the dpiFound (could be 0)
			DpiCounter counter = new DpiCounter();

			for (int pageNum : pagesToTest) {
				DimensionInfo dimPage = pageDimensions.get(pageNum);
				DimensionInfo dimImage = imageDimensions.get(pageNum);
				LOGGER.fine("Page [" + pageNum + "] dimension " + dimImage);
				if (dimImage == DimensionInfo.EMPTY) {
					continue;
				}

				int dpiFound = findDensity(dimImage, dimPage, 1.0f);
				LOGGER.fine("Page [" + pageNum + "] density " + dpiFound);

				if (dpiFound != 0) {
					counter.increment(dpiFound);
				}
			}
			// Find the most usual dpi
			Entry<Integer, Integer> bestDpi = counter.getBest();
			LOGGER.fine("Second pass in "
					+ (System.currentTimeMillis() - beginTime));
			if (bestDpi.getKey() == 0) {
				return;
			}
			// If more scanned pages than the threshold
			if (bestDpi.getValue().intValue() > nbSamples / THRESHOLD) {
				fd.setScan(true);
				fd.setResolution(bestDpi.getKey());
			}
		} catch (IOException e) {
			e.printStackTrace();
			fd.setValid(false);
			throw e;
		} finally {
			fd.setTimeToProcess(System.currentTimeMillis() - beginTime);
		}
	}

	protected int parsePage(PDPage page, int numPage) throws IOException {
		nbImagesInPage = 0;
		PDRectangle rect = page.getMediaBox(); // Found page dimension
		// MediaBox specified in "default user space units", which is points
		// (i.e. 72 dpi)
		// float userUnit = page.getUserUnit(); // in multiples of 1/72 inch

		DimensionInfo dimPage = new DimensionInfo((long) (rect.getWidth()),
				(long) (rect.getHeight()));
		LOGGER.fine("Found page [" + numPage + "] with dimension "
				+ dimPage.toString());
		pageDimensions.add(dimPage);

		PDResources resources = page.getResources();
		COSDictionary resc = resources.getCOSObject();

		recurseLookForImage(resc, numPage == 2);

		return nbImagesInPage;
	}

	protected void recurseLookForImage(COSDictionary resc, boolean debug)
			throws IOException {
		COSDictionary dict = (COSDictionary) resc
				.getDictionaryObject(COSName.XOBJECT);
		if (dict == null) {
			// No image
			if (debug) {
				LOGGER.fine("No XOBJECT dictionnary");
			}
			return;
		}

		for (Entry<COSName, COSBase> e : dict.entrySet()) {
			if (debug) {
				LOGGER.fine("Looking for entry key=" + e.getKey());
			}
			COSBase value = e.getValue();
			if (debug) {
				LOGGER.fine("Looking for entry value=" + value + ", class="
						+ value.getClass());
			}
			if (value == null) {
				continue;
			} else if (value instanceof COSObject) {
				value = ((COSObject) value).getObject();
			}
			if (debug) {
				LOGGER.fine("Looking for object=" + value + ", class="
						+ value.getClass());
			}
			if (!(value instanceof COSStream)) {
				continue;
			}
			try (COSStream stream = (COSStream) value) {
				COSName subtype = stream.getCOSName(COSName.SUBTYPE);
				if (COSName.IMAGE.equals(subtype)) {
					Long width = stream.getLong(COSName.WIDTH);
					Long height = stream.getLong(COSName.HEIGHT);
					DimensionInfo dimImage = new DimensionInfo(width, height);
					if (debug)
						LOGGER.fine("Found image " + e.getKey()
								+ " with dimension " + dimImage.toString());
					if (nbImagesInPage == 0) {
						// Only record the first dimension in a page
						imageDimensions.add(dimImage);
					}
					nbImagesInPage++;
				} else if (COSName.FORM.equals(subtype)) {
					// Look for a image in the resources
					COSDictionary streamResc = (COSDictionary) stream
							.getCOSDictionary(COSName.RESOURCES);
					if (streamResc != null) {
						recurseLookForImage(streamResc, debug);
					}
				}
			}
		}
	}

}
