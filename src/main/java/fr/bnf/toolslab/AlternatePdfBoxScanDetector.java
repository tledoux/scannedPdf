package fr.bnf.toolslab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

public class AlternatePdfBoxScanDetector extends AbstractScanDetector {
  protected static final Logger LOGGER = Logger.getLogger(AlternatePdfBoxScanDetector.class
      .getName());

  FileDescriptor fd;
  int nbPages;
  int nbImages;
  int nbImagesInPage;
  List<DimensionInfo> pageDimensions;
  List<DimensionInfo> imageDimensions;

  @Override
  public void init(FileDescriptor fd) {
    LOGGER.fine("Processing " + fd.getFile().getName());
    this.fd = fd;
    this.nbPages = 0;
    this.nbImages = 0;
    this.pageDimensions = new ArrayList<>();
    this.imageDimensions = new ArrayList<>();
  }

  @Override
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
      LOGGER.fine("First pass in " + (System.currentTimeMillis() - beginTime));

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
      LOGGER.fine("Second pass in " + (System.currentTimeMillis() - beginTime));
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

    DimensionInfo dimPage = new DimensionInfo((long) (rect.getWidth()), (long) (rect.getHeight()));
    LOGGER.fine("Found page [" + numPage + "] with dimension " + dimPage.toString());
    pageDimensions.add(dimPage);

    PDResources resources = page.getResources();
    for (COSName name : resources.getXObjectNames()) {
      if (resources.isImageXObject(name)) {
        DimensionInfo dimImage = lookupImage(resources, name);
        if (!DimensionInfo.EMPTY.equals(dimImage)) {
          if (nbImagesInPage == 0) {
            // Only record the first dimension in a page
            imageDimensions.add(dimImage);
          }
          nbImagesInPage++;
        }
        continue;
      }
      PDXObject xobject = resources.getXObject(name);
      if (xobject instanceof PDFormXObject) {
        PDFormXObject form = (PDFormXObject) xobject;
        PDResources formResources = form.getResources();
        for (COSName nameInForm : formResources.getXObjectNames()) {
          if (formResources.isImageXObject(nameInForm)) {
            DimensionInfo dimImage = lookupImage(formResources, nameInForm);
            if (!DimensionInfo.EMPTY.equals(dimImage)) {
              if (nbImagesInPage == 0) {
                // Only record the first dimension in a page
                imageDimensions.add(dimImage);
              }
              nbImagesInPage++;
            }
            continue;
          }
        }
      }
    }

    return nbImagesInPage;
  }
}
