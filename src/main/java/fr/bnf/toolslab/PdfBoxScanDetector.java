package fr.bnf.toolslab;

import java.io.IOException;
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

public class PdfBoxScanDetector extends AbstractScanDetector {
	protected static Logger LOGGER = Logger.getLogger(PdfBoxScanDetector.class
			.getName());

	FileDescriptor fd;

	public void init(FileDescriptor fd) {
		LOGGER.fine("Processing " + fd.getFile().getName());
		this.fd = fd;
	}

	public void parse() throws IOException {
		long beginTime = System.currentTimeMillis();
		try (PDDocument document = PDDocument.load(fd.getFile())) {
			// First heuristic: compare the number of pages and the number of
			// images
			int nbPages = document.getNumberOfPages();
			int nbImages = countImages(document);
			fd.setValid(true);
			fd.setNbPages(nbPages);
			fd.setNbImages(nbImages);
			LOGGER.fine("First pass in "
					+ (System.currentTimeMillis() - beginTime));
			if (nbPages != nbImages) {
				fd.setScan(false);
				return;
			}

			// Second heuristic: pick some pages and look if the image covers
			// all the page
			int nbSamples = Math.min(nbPages, MAX_SAMPLES);
			List<Integer> pagesToTest = pickSamples(nbSamples, nbPages);

			int nbScanPages = 0;
			int scanDpi = 0;
			for (int pageNum : pagesToTest) {
				PDPage page = document.getPage(pageNum);
				int dpiFound = isScanPage(page);
				if (dpiFound != 0) {
					if (scanDpi != 0 && Math.abs(scanDpi - dpiFound) > 10) {
						continue;
					}
					scanDpi = dpiFound;
					nbScanPages++;
				} else {
					// No dpi detected, ignore the page
				}
			}
			LOGGER.fine("Second pass in "
					+ (System.currentTimeMillis() - beginTime));
			// If more scanned pages than the threshold
			if (nbScanPages > nbSamples / THRESHOLD) {
				fd.setScan(true);
				fd.setResolution(scanDpi);
			}

		} catch (IOException e) {
			fd.setValid(false);
			throw e;
		} finally {
			fd.setTimeToProcess(System.currentTimeMillis() - beginTime);
		}
	}

	int countImages(PDDocument document) {
		int nbImages = 0;
		for (PDPage page : document.getPages()) {
			PDResources resources = page.getResources();
			for (COSName name : resources.getXObjectNames()) {
				if (resources.isImageXObject(name)) {
					nbImages++;
				}
			}
		}
		return nbImages;
	}

	/**
	 * Look if there is an image that covers all the page
	 * 
	 * @param page
	 */
	private int isScanPage(PDPage page) throws IOException {
		PDRectangle rect = page.getMediaBox(); // Found page dimension
		// MediaBox specified in "default user space units", which is points
		// (i.e. 72 dpi)
		float userUnit = page.getUserUnit(); // in multiples of 1/72 inch

		DimensionInfo dimPage = new DimensionInfo((long) rect.getWidth(),
				(long) rect.getHeight());
		LOGGER.fine("Found page dimension " + dimPage.toString(""));

		// Enumerate the resources to avoid building a complete image
		PDResources pdResources = page.getResources();
		COSDictionary resc = pdResources.getCOSObject();
		COSDictionary dict = (COSDictionary) resc
				.getDictionaryObject(COSName.XOBJECT);
		if (dict == null) {
			// No image
			return 0;
		}
		for (Entry<COSName, COSBase> e : dict.entrySet()) {
			LOGGER.fine("Looking for entry " + e.getKey());
			COSBase c = e.getValue();
			if (c == null) {
				continue;
			} else if (c instanceof COSObject) {
				c = ((COSObject) c).getObject();
			}
			if (!(c instanceof COSStream)) {
				continue;
			}
			try (COSStream stream = (COSStream) c) {
				if (COSName.IMAGE.equals(stream.getCOSName(COSName.SUBTYPE))) {
					Long width = stream.getLong(COSName.WIDTH);
					Long height = stream.getLong(COSName.HEIGHT);
					DimensionInfo dimImage = new DimensionInfo(width, height);
					LOGGER.fine("Found image " + e.getKey()
							+ " with dimension " + dimImage.toString());
					return findDensity(dimImage, dimPage, userUnit);
				}
			}
		}
		return 0;
	}

}
