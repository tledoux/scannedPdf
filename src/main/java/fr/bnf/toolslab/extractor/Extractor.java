package fr.bnf.toolslab.extractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Abstract class for all the extractors.
 *
 */
public abstract class Extractor {

  protected long transfer(InputStream is, OutputStream os) throws IOException {
    // Then copy the stream
    byte[] buf = new byte[1024];
    int length;
    long totalLength = 0;
    while ((length = is.read(buf)) != -1) {
      os.write(buf, 0, length);
      totalLength += length;
    }
    return totalLength;
  }

  /**
   * Define additional parameters if encounter.
   * @param producer producer of the image
   * @param creationDate creation date of the image
   */
  public abstract void setInfo(String producer, Calendar creationDate);

  /**
   * Extract the image to the file.
   * @param outputFile file to write to
   * @return success or not
   */
  public abstract boolean extract(File outputFile) throws IOException;
}
