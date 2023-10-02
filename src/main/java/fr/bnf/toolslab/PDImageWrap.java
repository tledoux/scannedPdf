package fr.bnf.toolslab;

import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.filter.DecodeOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

/**
 * A wrapper around a image. The buffer is not decoded.
 */
public class PDImageWrap extends PDXObject implements PDImage {

  /**
   * Log instance.
   */
  protected static final Logger LOGGER = Logger.getLogger(PDImageWrap.class.getName());

  private PDColorSpace colorSpace;

  /**
   * current resource dictionary (has color spaces).
   */
  private final PDResources resources;

  /**
   * Creates an Image XObject with the given stream as its contents and current color spaces. This
   * constructor is for internal PDFBox use and is not for PDF generation. Users who want to create
   * images should look at {@link #createFromFileByExtension(File, PDDocument) }.
   *
   * @param stream the XObject stream to read
   * @param resources the current resources
   * @throws java.io.IOException if there is an error creating the XObject.
   */
  public PDImageWrap(PDStream stream, PDResources resources) throws IOException {
    super(stream, COSName.IMAGE);
    this.resources = resources;
  }

  /**
   * Returns the metadata associated with this XObject, or null if there is none.
   * 
   * @return the metadata associated with this object.
   */
  public PDMetadata getMetadata() {
    COSStream cosStream = getCOSObject().getCOSStream(COSName.METADATA);
    if (cosStream != null) {
      return new PDMetadata(cosStream);
    }
    return null;
  }

  /**
   * Sets the metadata associated with this XObject, or null if there is none.
   * 
   * @param meta the metadata associated with this object
   */
  public void setMetadata(PDMetadata meta) {
    getCOSObject().setItem(COSName.METADATA, meta);
  }

  /**
   * Returns the key of this XObject in the structural parent tree.
   *
   * @return this object's key the structural parent tree or -1 if there isn't any.
   */
  public int getStructParent() {
    return getCOSObject().getInt(COSName.STRUCT_PARENT);
  }

  /**
   * Sets the key of this XObject in the structural parent tree.
   * 
   * @param key the new key for this XObject
   */
  public void setStructParent(int key) {
    getCOSObject().setInt(COSName.STRUCT_PARENT, key);
  }

  /**
   * {@inheritDoc} The returned images are cached via a SoftReference.
   */
  @Override
  public BufferedImage getImage() throws IOException {
    return getImage(null, 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BufferedImage getImage(Rectangle region, int subsampling) throws IOException {
    throw new IOException("Not supporting getting image");
  }

  @Override
  public BufferedImage getRawImage() throws IOException {
    throw new IOException("Not supporting getting image");
  }

  @Override
  public WritableRaster getRawRaster() throws IOException {
    throw new IOException("Not supporting getting image");
  }

  /*
   * {@inheritDoc} The returned images are not cached.
   */
  @Override
  public BufferedImage getStencilImage(Paint paint) throws IOException {
    throw new IOException("Not supporting getting image");
  }

  /**
   * Returns an RGB buffered image containing the opaque image stream without any masks applied. If
   * this Image XObject is a mask then the buffered image will contain the raw mask.
   * 
   * @return the image without any masks applied
   * @throws IOException if the image cannot be read
   */
  public BufferedImage getOpaqueImage() throws IOException {
    throw new IOException("Not supporting getting image");

  }

  /**
   * Returns the color key mask array associated with this image, or null if there is none.
   * 
   * @return Mask Image XObject
   */
  public COSArray getColorKeyMask() {
    return getCOSObject().getCOSArray(COSName.MASK);
  }

  @Override
  public int getBitsPerComponent() {
    if (isStencil()) {
      return 1;
    } else {
      return getCOSObject().getInt(COSName.BITS_PER_COMPONENT, COSName.BPC);
    }
  }

  @Override
  public void setBitsPerComponent(int bpc) {
    getCOSObject().setInt(COSName.BITS_PER_COMPONENT, bpc);
  }

  @Override
  public PDColorSpace getColorSpace() throws IOException {
    if (colorSpace == null) {
      COSBase cosBase = getCOSObject().getItem(COSName.COLORSPACE, COSName.CS);
      if (cosBase != null) {
        COSObject indirect = null;
        if (cosBase instanceof COSObject && resources != null
            && resources.getResourceCache() != null) {
          // PDFBOX-4022: use the resource cache because several images
          // might have the same colorspace indirect object.
          indirect = (COSObject) cosBase;
          colorSpace = resources.getResourceCache().getColorSpace(indirect);
          if (colorSpace != null) {
            return colorSpace;
          }
        }
        colorSpace = PDColorSpace.create(cosBase, resources);
        if (indirect != null) {
          resources.getResourceCache().put(indirect, colorSpace);
        }
      } else if (isStencil()) {
        // stencil mask color space must be gray, it is often missing
        return PDDeviceGray.INSTANCE;
      } else {
        // an image without a color space is always broken
        throw new IOException("could not determine color space");
      }
    }
    return colorSpace;
  }

  @Override
  public InputStream createInputStream() throws IOException {
    return getStream().createInputStream();
  }

  @Override
  public InputStream createInputStream(DecodeOptions options) throws IOException {
    return getStream().createInputStream(options);
  }

  @Override
  public InputStream createInputStream(List<String> stopFilters) throws IOException {
    return getStream().createInputStream(stopFilters);
  }

  @Override
  public boolean isEmpty() {
    return getStream().getCOSObject().getLength() == 0;
  }

  @Override
  public void setColorSpace(PDColorSpace cs) {
    getCOSObject().setItem(COSName.COLORSPACE, cs != null ? cs.getCOSObject() : null);
    colorSpace = null;
  }

  @Override
  public int getHeight() {
    return getCOSObject().getInt(COSName.HEIGHT);
  }

  @Override
  public void setHeight(int h) {
    getCOSObject().setInt(COSName.HEIGHT, h);
  }

  @Override
  public int getWidth() {
    return getCOSObject().getInt(COSName.WIDTH);
  }

  @Override
  public void setWidth(int w) {
    getCOSObject().setInt(COSName.WIDTH, w);
  }

  @Override
  public boolean getInterpolate() {
    return getCOSObject().getBoolean(COSName.INTERPOLATE, false);
  }

  @Override
  public void setInterpolate(boolean value) {
    getCOSObject().setBoolean(COSName.INTERPOLATE, value);
  }

  @Override
  public void setDecode(COSArray decode) {
    getCOSObject().setItem(COSName.DECODE, decode);
  }

  @Override
  public COSArray getDecode() {
    return getCOSObject().getCOSArray(COSName.DECODE);
  }

  @Override
  public boolean isStencil() {
    return getCOSObject().getBoolean(COSName.IMAGE_MASK, false);
  }

  @Override
  public void setStencil(boolean isStencil) {
    getCOSObject().setBoolean(COSName.IMAGE_MASK, isStencil);
  }

  /**
   * This will get the suffix for this image type, e.g. jpg/png.
   * 
   * @return The image suffix or null if not available.
   */
  @Override
  public String getSuffix() {
    List<COSName> filters = getStream().getFilters();

    if (filters.isEmpty()) {
      return "png";
    } else if (filters.contains(COSName.DCT_DECODE)) {
      return "jpg";
    } else if (filters.contains(COSName.JPX_DECODE)) {
      return "jpx";
    } else if (filters.contains(COSName.CCITTFAX_DECODE)) {
      return "tiff";
    } else if (filters.contains(COSName.FLATE_DECODE) || filters.contains(COSName.LZW_DECODE)
        || filters.contains(COSName.RUN_LENGTH_DECODE)) {
      return "png";
    } else if (filters.contains(COSName.JBIG2_DECODE)) {
      return "jb2";
    } else {
      LOGGER.info("getSuffix() returns null, filters: " + filters);
      return null;
    }
  }

  /**
   * This will get the optional content group or optional content membership dictionary.
   *
   * @return The optional content group or content membership dictionary or null if there is none.
   */
  public PDPropertyList getOptionalContent() {
    COSDictionary optionalContent = getCOSObject().getCOSDictionary(COSName.OC);
    return optionalContent != null ? PDPropertyList.create(optionalContent) : null;
  }

  /**
   * Sets the optional content group or optional content membership dictionary.
   * 
   * @param oc The optional content group or optional content membership dictionary.
   */
  public void setOptionalContent(PDPropertyList oc) {
    getCOSObject().setItem(COSName.OC, oc);
  }
}
