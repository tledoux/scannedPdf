package fr.bnf.toolslab.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a JPEG image file from a PDImage.
 */
public class JpegExtractor extends Extractor {
  protected static final Logger LOGGER = Logger.getLogger(JpegExtractor.class.getName());

  protected PDImageXObject image;
  protected int dpiX;
  protected int dpiY;

  protected ByteBuffer headerBuffer;

  /**
   * Constructor.
   *
   * @param image image to extract
   * @param dpiX resolution in X
   * @param dpiY resolution in Y
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Encapsulate PDImage")
  public JpegExtractor(PDImageXObject image, int dpiX, int dpiY) {
    LOGGER.fine("Using JpegExtractor with " + dpiX + " DPI");
    this.image = image;
    this.dpiX = dpiX;
    this.dpiY = dpiY;
    this.headerBuffer = ByteBuffer.allocate(1000);

    // Prepare the header
    this.headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public void setInfo(String producer, Calendar creationDate) {
    // UNIMPLEMENTED
  }

  protected int getPosition() {
    return headerBuffer.position();
  }

  protected void addByte(byte b) {
    headerBuffer.put(b);
  }

  protected void addShort(short s) {
    headerBuffer.putShort(s);
  }

  protected void addInt(int i) {
    headerBuffer.putInt(i);
  }

  @Override
  public boolean extract(File outputFile) {
    PDStream stream = image.getStream();
    long streamLength = stream.getCOSObject().getLength();
    LOGGER.info(String.format("Stream length %d", streamLength));

    // See §3.3.7 of pdfreference1.7old.pdf
    // ICC
    int bitsPerComponent = image.getBitsPerComponent();
    LOGGER.info("Found bitsPerComponent " + bitsPerComponent);
    PDColorSpace cspace = null;
    try {
      cspace = image.getColorSpace();
    } catch (IOException e) {
      LOGGER.warning("No colorspace in image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    LOGGER.info("Found colorSpace " + cspace);

    List<String> stopFilters = Arrays.asList("DCTDecode"); // keep DCT
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
