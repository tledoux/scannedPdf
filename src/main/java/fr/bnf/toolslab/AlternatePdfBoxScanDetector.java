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
				this.nbImages += parsePage(page);
			}
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
			int nbScanPages = 0;
			int scanDpi = 0;
			for (int pageNum : pagesToTest) {
				DimensionInfo dimPage = pageDimensions.get(pageNum);
				DimensionInfo dimImage = imageDimensions.get(pageNum);
				if (dimImage == DimensionInfo.EMPTY) {
					continue;
				}

				int dpiFound = findDensity(dimImage, dimPage, 1.0f);
				if (dpiFound != 0) {
					// If the resolution found is too different from the
					// previous resolution, ignore the page
					if (scanDpi != 0 && Math.abs(scanDpi - dpiFound) > 10) {
						continue;
					}
					scanDpi = dpiFound;
					nbScanPages++;
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
			e.printStackTrace();
			fd.setValid(false);
			throw e;
		} finally {
			fd.setTimeToProcess(System.currentTimeMillis() - beginTime);
		}
	}

	protected int parsePage(PDPage page) throws IOException {
		PDRectangle rect = page.getMediaBox(); // Found page dimension
		// MediaBox specified in "default user space units", which is points
		// (i.e. 72 dpi)
		// float userUnit = page.getUserUnit(); // in multiples of 1/72 inch

		DimensionInfo dimPage = new DimensionInfo((long) (rect.getWidth()),
				(long) (rect.getHeight()));
		LOGGER.fine("Found page dimension " + dimPage.toString(""));
		pageDimensions.add(dimPage);

		PDResources resources = page.getResources();
		int nbImagesInPage = 0;
		COSDictionary resc = resources.getCOSObject();
		COSDictionary dict = (COSDictionary) resc
				.getDictionaryObject(COSName.XOBJECT);
		if (dict == null) {
			// No image
			imageDimensions.add(DimensionInfo.EMPTY);
			return 0;
		}
		for (Entry<COSName, COSBase> e : dict.entrySet()) {
			LOGGER.fine("Looking for entry " + e.getKey());
			COSBase value = e.getValue();
			if (value == null) {
				continue;
			} else if (value instanceof COSObject) {
				value = ((COSObject) value).getObject();
			}
			if (!(value instanceof COSStream)) {
				continue;
			}
			try (COSStream stream = (COSStream) value) {
				if (COSName.IMAGE.equals(stream.getCOSName(COSName.SUBTYPE))) {
					Long width = stream.getLong(COSName.WIDTH);
					Long height = stream.getLong(COSName.HEIGHT);
					DimensionInfo dimImage = new DimensionInfo(width, height);
					LOGGER.fine("Found image " + e.getKey()
							+ " with dimension " + dimImage.toString());
					if (nbImages == 0) {
						// Only record the first one
						imageDimensions.add(dimImage);
					}
					nbImagesInPage++;
				}
			}
		}
		return nbImagesInPage;
	}
}
