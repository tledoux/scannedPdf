package fr.bnf.toolslab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

public abstract class AbstractScanDetector {
  protected static final Logger LOGGER = Logger.getLogger(AbstractScanDetector.class.getName());

  protected static final int THRESHOLD = 2;
  protected static final int MAX_SAMPLES = 10;

  /**
   * Method to provide the file descriptor of the file to scan.
   * 
   * @param fd file descriptor of the file
   */
  abstract void init(FileDescriptor fd);

  /**
   * Method to parse the given file.
   * 
   * @throws IOException exception if error while reading the file
   */
  abstract void parse() throws IOException;

  /**
   * Recursively inspect the resources looking for images.
   * 
   * @param resources resources to inspect
   * @param predicate a predicate to call for each image. Return <code>true</code> if recursion
   *        continues.
   * @throws IOException in case of IO problems
   */
  void recurseForImages(PDResources resources, Predicate<DimensionInfo> predicate)
      throws IOException {
    for (COSName name : resources.getXObjectNames()) {
      if (resources.isImageXObject(name)) {
        DimensionInfo dimImage = lookupImage(resources, name);
        if (!predicate.test(dimImage)) {
          break;
        }
        continue;
      }
      PDXObject xobject = resources.getXObject(name);
      if (xobject instanceof PDFormXObject) {
        PDFormXObject form = (PDFormXObject) xobject;
        PDResources formResources = form.getResources();
        recurseForImages(formResources, predicate);
      }
    }
  }

  /**
   * Find the filter associated with the stream. FLate= Direct, DCT=JPEG, JPX=JPEG2000, CCITT=TIFF
   * G3, LZW=compress LZW, RLE=compress RLE, JBIG2
   * 
   * @param stream stream to analyze
   * @return name of the filter
   */
  protected String decodeFilter(COSStream stream) {
    COSBase filters = stream.getFilters();
    if (filters instanceof COSName) {
      return ((COSName) filters).getName();
    } else if (filters instanceof COSArray) {
      COSArray filterArray = (COSArray) filters;
      for (int i = 0; i < filterArray.size(); i++) {
        COSBase base = filterArray.get(i);
        if (base instanceof COSName) {
          return ((COSName) filters).getName();
        }
      }
    }
    return "";
  }

  /**
   * Lookup the technical metadata of an image WITHOUT reading the image. <b>Don't call
   * resc.getXObject() on a image, access the COSStream directly.</b>
   * 
   * @param pdResources the resources of the parent
   * @param name the name of the object
   * @return the dimension of the image
   * @throws IOException exception if error while reading the file
   */
  protected DimensionInfo lookupImage(PDResources pdResources, COSName name) throws IOException {
    COSDictionary resc = pdResources.getCOSObject();
    COSDictionary dict = (COSDictionary) resc.getDictionaryObject(COSName.XOBJECT);
    if (dict == null) {
      // No image
      return DimensionInfo.EMPTY;
    }
    try (COSStream stream = dict.getCOSStream(name)) {
      if (stream != null && COSName.IMAGE.equals(stream.getCOSName(COSName.SUBTYPE))) {
        Long width = stream.getLong(COSName.WIDTH);
        Long height = stream.getLong(COSName.HEIGHT);
        LOGGER.info("Image filter " + decodeFilter(stream));
        return new DimensionInfo(width, height);
      }
    }
    return DimensionInfo.EMPTY;
  }

  /**
   * Select nbSamples pages in a random fashion.
   * 
   * @param nbSamples number of samples to collect
   * @param nbPages total number of pages
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
   * Calculate the relative density with the given dimensions.
   * 
   * @param dimImage dimension of the image
   * @param dimPage dimension of the page in 72 dpi per userUnit
   * @param userUnit potential multiplier of 72 dpi
   * @return the calculate density
   */
  int findDensity(DimensionInfo dimImage, DimensionInfo dimPage, float userUnit) {
    if (!dimImage.contains(dimPage)) {
      return 0;
    }
    double dpiX = (double) dimImage.width / dimPage.width * 72.0 / userUnit;
    // double dpiY = (double) dimImage.y / dimPage.y * 72.0 /
    // userUnit;
    LOGGER.fine("Found density of " + (int) dpiX);

    return (int) dpiX;
  }

}
