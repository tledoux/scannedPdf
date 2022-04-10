package fr.bnf.toolslab;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

/**
 * Engine to extract images. Coming from PDFBox source
 *
 */
public class ImageGraphicsEngine extends PDFGraphicsStreamEngine { // TODO try PDFStreamEngine
  protected static final Logger LOGGER = Logger.getLogger(ImageGraphicsEngine.class.getName());

  final List<IOException> exceptions = new ArrayList<>();
  private final Map<COSStream, Integer> processedInlineImages;
  private final AtomicInteger imageCounter;
  private final List<DimensionInfo> imageDimensions;
  
  protected ImageGraphicsEngine(PDPage page, Map<COSStream, Integer> processedInlineImages,
      AtomicInteger imageCounter) {
    super(page);
    // Replace the normal draw object to avoid building the images...
    addOperator(new DrawObjectOperator());
    // Inhibit all the text operators to speed up the analysis
    addOperator(new NoOpOperator(OperatorName.SET_FONT_AND_SIZE));
    addOperator(new NoOpOperator(OperatorName.SHOW_TEXT));
    addOperator(new NoOpOperator(OperatorName.SHOW_TEXT_ADJUSTED));
    addOperator(new NoOpOperator(OperatorName.SHOW_TEXT_LINE));
    addOperator(new NoOpOperator(OperatorName.SHOW_TEXT_LINE_AND_SPACE));
    
    this.processedInlineImages = processedInlineImages;
    this.imageCounter = imageCounter;
    this.imageDimensions = new ArrayList<>();

  }

  void run() throws IOException {
    PDPage page = getPage();
    processPage(page);
    PDResources res = page.getResources();
    if (res == null) {
      return;
    }
  }

  @Override
  public void drawImage(PDImage pdImage) throws IOException {
    int imageNumber = 0;
    if (pdImage instanceof PDImageXObject) {
      PDImageXObject xobject = (PDImageXObject) pdImage;
      Integer cachedNumber = processedInlineImages.get(xobject.getCOSObject());
      if (cachedNumber != null) {
        // skip duplicate image
        return;
      }
      if (cachedNumber == null) {
        imageNumber = imageCounter.getAndIncrement();
        processedInlineImages.put(xobject.getCOSObject(), imageNumber);
      }
    } else if (pdImage instanceof PDImageWrap) {
      PDImageWrap xobject = (PDImageWrap) pdImage;
      Integer cachedNumber = processedInlineImages.get(xobject.getCOSObject());
      if (cachedNumber != null) {
        // skip duplicate image
        return;
      }
      if (cachedNumber == null) {
        imageNumber = imageCounter.getAndIncrement();
        processedInlineImages.put(xobject.getCOSObject(), imageNumber);
      }
    } else {
      imageNumber = imageCounter.getAndIncrement();
    }
    // TODO: should we use the hash of the PDImage to check for seen
    // For now, we're relying on the cosobject, but this could lead to
    // duplicates if the pdImage is not a PDImageXObject?
    processImage(pdImage, imageNumber);
  }

  @Override
  public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
    /* DO NOTHING */
  }

  @Override
  public void clip(int windingRule) throws IOException {
    /* DO NOTHING */
  }

  @Override
  public void moveTo(float x, float y) throws IOException {
    /* DO NOTHING */
  }

  @Override
  public void lineTo(float x, float y) throws IOException {
    /* DO NOTHING */
  }

  @Override
  public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
      throws IOException {
    /* DO NOTHING */
  }

  @Override
  public Point2D getCurrentPoint() throws IOException {
    return new Point2D.Float(0, 0);
  }

  @Override
  public void closePath() throws IOException {
    /* DO NOTHING */
  }

  @Override
  public void endPath() throws IOException {
    /* DO NOTHING */
  }

  //@Override
  protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode,
      Vector displacement) throws IOException {
/* THL
    RenderingMode renderingMode = getGraphicsState().getTextState().getRenderingMode();
    if (renderingMode.isFill()) {
      processColor(getGraphicsState().getNonStrokingColor());
    }

    if (renderingMode.isStroke()) {
      processColor(getGraphicsState().getStrokingColor());
    }
    */
  }

  @Override
  public void strokePath() throws IOException {
    // THL processColor(getGraphicsState().getStrokingColor());
  }

  @Override
  public void fillPath(int windingRule) throws IOException {
    //THL processColor(getGraphicsState().getNonStrokingColor());
  }

  @Override
  public void fillAndStrokePath(int windingRule) throws IOException {
    //THL processColor(getGraphicsState().getNonStrokingColor());
  }

  @Override
  public void shadingFill(COSName shadingName) throws IOException {
    /* DO NOTHING */
  }

  private void processImage(PDImage pdImage, int imageNumber) throws IOException {
    // this is the metadata for this particular image
    String suffix = pdImage.getSuffix();
    if (suffix == null) {
      suffix = "img";
    }
    String key = String.format("image_%04d.%s", imageNumber, suffix);
    LOGGER.fine("Found " + key);
    DimensionInfo dim = new DimensionInfo(pdImage.getWidth(), pdImage.getHeight());
    imageDimensions.add(dim);
    LOGGER.fine("Found dimensions " + dim);
  }

  /**
   * @return the imageCounter
   */
  public AtomicInteger getImageCounter() {
    return imageCounter;
  }

  /**
   * @return the imageDimensions
   */
  public List<DimensionInfo> getImageDimensions() {
    return imageDimensions;
  }

}
