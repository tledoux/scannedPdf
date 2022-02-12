package fr.bnf.toolslab;

import java.io.IOException;
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

public class PdfBoxScanDetector extends AbstractScanDetector {
  protected static final Logger LOGGER = Logger.getLogger(PdfBoxScanDetector.class.getName());

  FileDescriptor fd;

  @Override
  public void init(FileDescriptor fd) {
    LOGGER.fine("Processing " + fd.getFile().getName());
    this.fd = fd;
  }

  @Override
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
      LOGGER.fine("First pass in " + (System.currentTimeMillis() - beginTime));
      LOGGER.fine("Find " + nbPages + " pages and " + nbImages + " images");
      if (nbPages != nbImages) {
        fd.setScan(false);
        return;
      }

      // Second heuristic: pick some pages and look if the image covers
      // all the page
      int nbSamples = Math.min(nbPages, MAX_SAMPLES);
      List<Integer> pagesToTest = pickSamples(nbSamples, nbPages);
      DpiCounter counter = new DpiCounter();
      for (int pageNum : pagesToTest) {
        PDPage page = document.getPage(pageNum);
        int dpiFound = isScanPage(page);
        if (dpiFound != 0) {
          counter.increment(dpiFound);
        }
      }
      LOGGER.fine("Second pass in " + (System.currentTimeMillis() - beginTime));
      // If more scanned pages than the threshold
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
      fd.setValid(false);
      throw e;
    } finally {
      fd.setTimeToProcess(System.currentTimeMillis() - beginTime);
    }
  }

  /**
   * Count the number of images in the PDF document.
   * 
   * @param document PDF document to scan
   * @return number of found images
   * @throws IOException exception if error while reading the file
   */
  int countImages(PDDocument document) throws IOException {
    int nbImages = 0;
    for (PDPage page : document.getPages()) {
      PDResources resources = page.getResources();
      // Count the images in the page or in the form of the page
      for (COSName name : resources.getXObjectNames()) {
        if (resources.isImageXObject(name)) {
          nbImages++;
          continue;
        }
        PDXObject xobject = resources.getXObject(name);
        if (xobject instanceof PDFormXObject) {
          PDFormXObject form = (PDFormXObject) xobject;
          PDResources formResources = form.getResources();
          for (COSName nameInForm : formResources.getXObjectNames()) {
            if (formResources.isImageXObject(nameInForm)) {
              nbImages++;
              continue;
            }
          }
        }
      }
    }
    return nbImages;
  }

  /**
   * Look if there is an image that covers all the page.
   * 
   * @param page PDF page to evaluate
   */
  private int isScanPage(PDPage page) throws IOException {
    PDRectangle rect = page.getMediaBox(); // Found page dimension
    // MediaBox specified in "default user space units", which is points
    // (i.e. 72 dpi)
    float userUnit = page.getUserUnit(); // in multiples of 1/72 inch

    DimensionInfo dimPage = new DimensionInfo((long) rect.getWidth(), (long) rect.getHeight());
    LOGGER.fine("Found page dimension " + dimPage.toString());

    // Enumerate the resources to avoid building a complete image
    PDResources resources = page.getResources();
    // Count the images in the page or in the form of the page
    for (COSName name : resources.getXObjectNames()) {
      if (resources.isImageXObject(name)) {
        DimensionInfo dimImage = lookupImage(resources, name);
        if (!DimensionInfo.EMPTY.equals(dimImage)) {
          return findDensity(dimImage, dimPage, userUnit);
        }

      }
      PDXObject xobject = resources.getXObject(name);
      if (xobject instanceof PDFormXObject) {
        PDFormXObject form = (PDFormXObject) xobject;
        PDResources formResources = form.getResources();
        for (COSName nameInForm : formResources.getXObjectNames()) {
          if (formResources.isImageXObject(nameInForm)) {
            DimensionInfo dimImage = lookupImage(formResources, nameInForm);
            if (!DimensionInfo.EMPTY.equals(dimImage)) {
              return findDensity(dimImage, dimPage, userUnit);
            }
          }
        }
      }
    }
    return 0;
  }

}
