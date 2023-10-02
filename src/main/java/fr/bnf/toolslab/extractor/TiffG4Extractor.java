package fr.bnf.toolslab.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a TIFF G4 image file from a PDImage.
 */
public class TiffG4Extractor extends TiffExtractor {
  protected static final Logger LOGGER = Logger.getLogger(TiffG4Extractor.class.getName());

  /**
   * Constructor.
   *
   * @param image image to extract
   * @param dpiX resolution in X
   * @param dpiY resolution in Y
   */
  public TiffG4Extractor(PDImageXObject image, int dpiX, int dpiY) {
    super(image, dpiX, dpiY);
  }

  /**
   * Extract the image to the file.
   *
   * @param outputFile file to write to
   * @return success or not
   */
  @Override
  public boolean extract(File outputFile) {
    PDStream stream = image.getStream();
    long streamLength = stream.getCOSObject().getLength();
    LOGGER.info(String.format("Stream length %d", streamLength));

    int width = image.getWidth();
    int height = image.getHeight();

    // See §3.3.5 of pdfreference1.7old.pdf
    COSDictionary dict = stream.getCOSObject().getCOSDictionary(COSName.DECODE_PARMS);
    int factorK = 0;
    boolean blackIs1 = false;
    if (dict != null) {
      factorK = dict.getInt(COSName.K, 0);
      blackIs1 = dict.getBoolean(COSName.BLACK_IS_1, false);
    } else {
      LOGGER.warning("No dictionnnary for " + COSName.DECODE_PARMS);
    }
    LOGGER.info(String.format("Image size (%d x %d) with factorK %d, blackIs1 %d", width, height,
        factorK, blackIs1 ? 1 : 0));
    int compression = calculateCompression(factorK);
    int photometricInterpretation = blackIs1 ? 1 : 0;

    List<String> stopFilters = Arrays.asList("CCITTFaxDecode"); // keep G4 compression
    try (InputStream is = image.createInputStream(stopFilters);
        FileOutputStream fos = new FileOutputStream(outputFile)) {

      // Append IFDs to the header
      // cf https://www.awaresystems.be/imaging/tiff/specification/TIFF6.pdf
      addShort(calculateNbFields((short) 11)); // 2-byte count of the number of directory entries
      addField(IMAGEWIDTH, TIFF_TYPE_LONG, 1, width);
      addField(IMAGELENGTH, TIFF_TYPE_LONG, 1, height);
      addField(BITSPERSAMPLE, TIFF_TYPE_SHORT, 1, 1);
      // NoCompression=1, CCITT G3=3, CCITT G4=4
      addField(COMPRESSION, TIFF_TYPE_SHORT, 1, compression);
      // WhiteIsZero=0, BlackIsZero=1
      addField(PHOTOMETRICINTERPRETATION, TIFF_TYPE_SHORT, 1, photometricInterpretation);
      // For each strip, the byte offset of that strip
      idxStripOffset = addField(STRIPOFFSETS, TIFF_TYPE_LONG, 1, DUMMY_LENGTH);
      addField(ROWSPERSTRIP, TIFF_TYPE_LONG, 1, height); // number of rows in each strip
      // For each strip, the number of bytes in that strip after any compression
      addField(STRIPBYTECOUNTS, TIFF_TYPE_LONG, 1, (int) streamLength);
      idxResolutionX = addField(XRESOLUTION, TIFF_TYPE_RATIONAL, 1, DUMMY_VALUE);
      idxResolutionY = addField(YRESOLUTION, TIFF_TYPE_RATIONAL, 1, DUMMY_VALUE);
      addField(RESOLUTIONUNIT, TIFF_TYPE_SHORT, 1, 2); // inch=2
      addExtraFields();
      addInt(0); // Next IFD offset (0=END)
      // DATA
      addResolutionData();
      addExtraData();

      int lenHeader = getPosition();
      setField(idxStripOffset, lenHeader);
      fos.write(headerBuffer.array(), 0, lenHeader);

      LOGGER.fine(String.format("Header final %d", lenHeader));

      // Then copy the stream
      long totalLength = transfer(is, fos);
      LOGGER.info(String.format("Data length %d", totalLength));
    } catch (IOException e) {
      LOGGER.warning("Error processing image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Infer compression value given the K parameter.
   *
   * @param factorK K parameter of the parameter
   * @return compression value
   */
  @SuppressFBWarnings(value = "DB_DUPLICATE_BRANCHES",
      justification = "Duplication is intended to facilitate the reading")
  private int calculateCompression(int factorK) {
    int compression = 1;
    if (factorK < 0) { // Pure 2-dimensional encoding (Group 4)
      compression = 4; // CCITT G4
    } else if (factorK == 0) { // Pure 1-dimensional encoding (Group 3, 1D)
      compression = 3; // Group 3 1D
    } else { // Mixed 1 and 2-dimensional encoding (Group 3, 2D)
      compression = 3; // Group 3 2D
    }
    return compression;
  }
}
