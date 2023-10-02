package fr.bnf.toolslab.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a JPEG image file from a PDImage.
 */
public class Jpeg2000Extractor extends Extractor {
  protected static final Logger LOGGER = Logger.getLogger(Jpeg2000Extractor.class.getName());

  protected PDImageXObject image;

  public Jpeg2000Extractor(PDImageXObject image) {
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

      // No header !!!

      // Then copy the stream
      long totalLength = transfer(is, fos);
      LOGGER.info(String.format("Data length %d", totalLength));
    } catch (IOException e) {
      LOGGER.warning("Error processing image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    return true;
  }

}
