package fr.bnf.toolslab.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a TIFF G4 image file from a PDImage.
 */
public class TiffGreyExtractor extends TiffExtractor {
  protected static final Logger LOGGER = Logger.getLogger(TiffGreyExtractor.class.getName());

  protected int idxStripByteCounts = 0;

  /**
   * Constructor.
   * 
   * @param image image to extract
   * @param dpiX resolution in X
   * @param dpiY resolution in Y
   */
  public TiffGreyExtractor(PDImageXObject image, int dpiX, int dpiY) {
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
    long streamLength = stream.getCOSObject().getLength(); // compress size
    long completeStreamLength = stream.getDecodedStreamLength(); // -1
    LOGGER.info(
        String.format("Stream length %d, uncompressed %d", streamLength, completeStreamLength));

    int width = image.getWidth();
    int height = image.getHeight();
    int bitsPerComponent = image.getBitsPerComponent();
    PDColorSpace cspace = null;
    try {
      cspace = image.getColorSpace();
    } catch (IOException e) {
      LOGGER.warning("No colorspace in image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    LOGGER.info("Found colorSpace " + cspace);

    try (FileOutputStream fos = new FileOutputStream(outputFile)) {

      // Append IFDs to the header
      // cf https://www.awaresystems.be/imaging/tiff/specification/TIFF6.pdf
      addShort(calculateNbFields((short) 11)); // 2-byte count of the number of directory entries
      addField(IMAGEWIDTH, TIFF_TYPE_LONG, 1, width);
      addField(IMAGELENGTH, TIFF_TYPE_LONG, 1, height);
      addField(BITSPERSAMPLE, TIFF_TYPE_SHORT, 1, bitsPerComponent);
      addField(COMPRESSION, TIFF_TYPE_SHORT, 1, 1);// NoCompression=1
      addField(PHOTOMETRICINTERPRETATION, TIFF_TYPE_SHORT, 1, 0); // TODO or 1
      // For each strip, the byte offset of that strip
      idxStripOffset = addField(STRIPOFFSETS, TIFF_TYPE_LONG, 1, DUMMY_LENGTH);
      addField(ROWSPERSTRIP, TIFF_TYPE_LONG, 1, height); // number of rows in each strip
      // For each strip, the number of bytes in that strip after any compression
      idxStripByteCounts = addField(STRIPBYTECOUNTS, TIFF_TYPE_LONG, 1, DUMMY_VALUE);
      idxResolutionX = addField(XRESOLUTION, TIFF_TYPE_RATIONAL, 1, DUMMY_VALUE);
      idxResolutionY = addField(YRESOLUTION, TIFF_TYPE_RATIONAL, 1, DUMMY_VALUE);
      addField(RESOLUTIONUNIT, TIFF_TYPE_SHORT, 1, 2); // inch=2
      addExtraFields();
      addInt(0); // Next IFD offset (0=END)
      // DATA
      addResolutionData();
      addExtraData();

      // Need to know the length of the uncompressed stream !!!
      // HUGE memory hole
      byte[] fullData = stream.toByteArray();
      long totalLength = fullData.length;
      setField(idxStripByteCounts, (int)totalLength);
      
      int lenHeader = getPosition();
      setField(idxStripOffset, lenHeader);
      fos.write(headerBuffer.array(), 0, lenHeader);
      LOGGER.info(String.format("Header final size %d", lenHeader));

      // Then copy the stream
      fos.write(fullData, 0, (int) totalLength);
      LOGGER.info(String.format("Data length %d", totalLength));
    } catch (IOException e) {
      LOGGER.warning("Error processing image to [" + outputFile.getName() + "]" + e.getMessage());
      return false;
    }
    return true;
  }
}
