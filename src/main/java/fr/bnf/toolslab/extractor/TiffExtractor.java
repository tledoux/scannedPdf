package fr.bnf.toolslab.extractor;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Extract a TIFF uncompressed image file from a PDImage.
 */
public abstract class TiffExtractor extends Extractor {
  protected static final Logger LOGGER = Logger.getLogger(TiffExtractor.class.getName());

  public static final int IFD_SIZE = 12;

  public static final short TIFF_TYPE_ASCII = 2;
  public static final short TIFF_TYPE_SHORT = 3;
  public static final short TIFF_TYPE_LONG = 4;
  public static final short TIFF_TYPE_RATIONAL = 5; // 2 LONGs (2 * 4 bytes)

  // Value to be replaced when the length has been calculated or reached
  public static final int DUMMY_LENGTH = 144;
  // Value to be replaced when the information will be available (ie offsets)
  public static final int DUMMY_VALUE = 0;

  // List of common TIFF TAGs values
  public static final short NEWSUBFILETYPE = 254;
  public static final short SUBFILETYPE = 255;
  public static final short IMAGEWIDTH = 256;
  public static final short IMAGELENGTH = 257;
  public static final short BITSPERSAMPLE = 258;
  public static final short COMPRESSION = 259;
  public static final short PHOTOMETRICINTERPRETATION = 262;
  public static final short THRESHHOLDING = 263;
  public static final short CELLWIDTH = 264;
  public static final short CELLLENGTH = 265;
  public static final short FILLORDER = 266;
  public static final short DOCUMENTNAME = 269;
  public static final short IMAGEDESCRIPTION = 270;
  public static final short MAKE = 271;
  public static final short MODEL = 272;
  public static final short STRIPOFFSETS = 273;
  public static final short ORIENTATION = 274;
  public static final short SAMPLESPERPIXEL = 277;
  public static final short ROWSPERSTRIP = 278;
  public static final short STRIPBYTECOUNTS = 279;
  public static final short MINSAMPLEVALUE = 280;
  public static final short MAXSAMPLEVALUE = 281;
  public static final short XRESOLUTION = 282;
  public static final short YRESOLUTION = 283;
  public static final short PLANARCONFIGURATION = 284;
  public static final short PAGENAME = 285;
  public static final short XPOSITION = 286;
  public static final short YPOSITION = 287;
  public static final short FREEOFFSETS = 288;
  public static final short FREEBYTECOUNTS = 289;
  public static final short GRAYRESPONSEUNIT = 290;
  public static final short GRAYRESPONSECURVE = 291;
  public static final short T4OPTIONS = 292;
  public static final short T6OPTIONS = 293;
  public static final short RESOLUTIONUNIT = 296;
  public static final short PAGENUMBER = 297;
  public static final short TRANSFERFUNCTION = 301;
  public static final short SOFTWARE = 305;
  public static final short DATETIME = 306;
  public static final short ARTIST = 315;
  public static final short HOSTCOMPUTER = 316;
  public static final short PREDICTOR = 317;
  public static final short WHITEPOINT = 318;
  public static final short PRIMARYCHROMATICITIES = 319;
  public static final short COLORMAP = 320;
  public static final short HALFTONEHINTS = 321;
  public static final short TILEWIDTH = 322;
  public static final short TILELENGTH = 323;
  public static final short TILEOFFSETS = 324;
  public static final short TILEBYTECOUNTS = 325;
  public static final short INKSET = 332;
  public static final short INKNAMES = 333;
  public static final short NUMBEROFINKS = 334;
  public static final short DOTRANGE = 336;
  public static final short TARGETPRINTER = 337;
  public static final short EXTRASAMPLES = 338;
  public static final short SAMPLEFORMAT = 339;
  public static final short SMINSAMPLEVALUE = 340;
  public static final short SMAXSAMPLEVALUE = 341;
  public static final short TRANSFERRANGE = 342;
  public static final short JPEGPROC = 512;
  public static final short JPEGINTERCHANGEFORMAT = 513;
  public static final short JPEGINTERCHANGEFORMATLENGTH = 514;
  public static final short JPEGRESTARTINTERVAL = 515;
  public static final short JPEGLOSSLESSPREDICTORS = 517;
  public static final short JPEGPOINTTRANSFORMS = 518;
  public static final short JPEGQTABLES = 519;
  public static final short JPEGDCTABLES = 520;
  public static final short JPEGACTABLES = 521;
  public static final short YCBCRCOEFFICIENTS = 529;
  public static final short YCBCRSUBSAMPLING = 530;
  public static final short YCBCRPOSITIONING = 531;
  public static final short REFERENCEBLACKWHITE = 532;
  public static final short COPYRIGHT = (short) 33432;

  protected PDImageXObject image;
  protected int dpiX;
  protected int dpiY;
  protected String producer;
  protected String creationDate;

  protected ByteBuffer headerBuffer;
  protected int idxStripOffset = 0;
  protected int idxResolutionX = 0;
  protected int idxResolutionY = 0;
  protected int idxProducer = 0;
  protected int idxCreationDate = 0;

  // YYYY:MM:DD HH:MM:SS
  // cf https://www.awaresystems.be/imaging/tiff/specification/TIFF6.pdf#page=31
  private SimpleDateFormat sdfTiff = new SimpleDateFormat("YYYY:MM:dd HH:mm:ss");

  /**
   * Constructor for an abstract TiffExtractor.
   * 
   * @param image image to extract
   * @param dpiX resolution in X
   * @param dpiY resolution in Y
   */
  public TiffExtractor(PDImageXObject image, int dpiX, int dpiY) {
    this.image = image;
    this.dpiX = dpiX;
    this.dpiY = dpiY;

    this.headerBuffer = ByteBuffer.allocate(1000);

    // Prepare the header

    this.headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
    // cf https://www.awaresystems.be/imaging/tiff/specification/TIFF6.pdf
    addByte((byte) 'I'); // 'I' => LITTLE_ENDIAN
    addByte((byte) 'I'); // 'I' => LITTLE_ENDIAN
    addShort((short) 42);
    addInt(8); // offset (in bytes) of the first IFD
  }

  @Override
  public void setInfo(String producer, Calendar creationDate) {
    this.producer = producer;
    if (creationDate != null) {
      Date cdate = creationDate.getTime();
      this.creationDate = sdfTiff.format(cdate);
    }
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

  protected void addAsciiString(String s) {
    for (int i = 0, n = s.length(); i < n; i++) {
      headerBuffer.put((byte) s.charAt(i));
    }
    headerBuffer.put((byte) 0); // End with a ZERO byte

  }

  /**
   * Add an field in the IFD.
   * 
   * @param tag value of the tiff tag
   * @param type tiff type
   * @param count number of repetition
   * @param value actually value or offset
   * @return the position to set a new value
   */
  protected int addField(short tag, short type, int count, int value) {
    headerBuffer.putShort(tag);
    headerBuffer.putShort(type);
    headerBuffer.putInt(count);
    int idx = headerBuffer.position();
    if (type == TIFF_TYPE_RATIONAL || type == TIFF_TYPE_ASCII) {
      // The value is just an offset
      headerBuffer.putInt(value);
    } else if (type == TIFF_TYPE_SHORT) {
      headerBuffer.putShort((short) value);
      headerBuffer.putShort((short) 0);
    } else {
      headerBuffer.putInt(value);
    }
    return idx;
  }

  protected void setField(int offset, int value) {
    headerBuffer.putInt(offset, value);
  }

  protected short calculateNbFields(short base) {
    short nbFields = base;
    if (producer != null) {
      nbFields++;
    }
    if (creationDate != null) {
      nbFields++;
    }
    return nbFields;
  }

  protected void addExtraFields() {
    if (producer != null) {
      idxProducer = addField(SOFTWARE, TIFF_TYPE_ASCII, producer.length() + 1, DUMMY_VALUE);
    }
    if (creationDate != null) {
      idxCreationDate = addField(DATETIME, TIFF_TYPE_ASCII, creationDate.length() + 1, DUMMY_VALUE);
    }
  }

  protected void addResolutionData() {
    LOGGER.info(String.format("Found XRESOLUTION %d YRESOLUTION %d", (int) dpiX, (int) dpiY));
    setField(idxResolutionX, getPosition());
    addInt((int) dpiX);
    addInt((int) 1);
    setField(idxResolutionY, getPosition());
    addInt((int) dpiY);
    addInt((int) 1);
  }

  protected void addExtraData() {
    if (producer != null) {
      setField(idxProducer, getPosition());
      addAsciiString(producer);
    }
    if (creationDate != null) {
      setField(idxCreationDate, getPosition());
      addAsciiString(creationDate);
    }
  }

  @Override
  public abstract boolean extract(File outputFile);


}
