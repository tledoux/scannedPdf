package fr.bnf.toolslab.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a JPEG image file from a PDImage.
 */
public class Jpeg2000Extractor extends Extractor {
  protected static final Logger LOGGER = Logger.getLogger(Jpeg2000Extractor.class.getName());

  protected PDImageXObject image;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Encapsulate PDImage")
  public Jpeg2000Extractor(PDImageXObject image) {
    LOGGER.fine("Using Jpeg2000Extractor");
    this.image = image;
  }

  @Override
  public void setInfo(String producer, Calendar creationDate) {
    // UNIMPLEMENTED
  }

  @Override
  public boolean extract(File outputFile) {
    PDStream stream = image.getStream();
    long streamLength = stream.getCOSObject().getLength();
    LOGGER.info(String.format("Stream length %d", streamLength));


    List<String> stopFilters = Arrays.asList("JPXDecode"); // keep JPX
    try (InputStream is = image.createInputStream(stopFilters);
        FileOutputStream fos = new FileOutputStream(outputFile)) {

      // Direct copy of the stream
      long totalLength = transfer(is, fos);
      LOGGER.info(String.format("Data length %d", totalLength));
    } catch (IOException e) {
      LOGGER.warning("Error processing image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    return true;
  }

}
