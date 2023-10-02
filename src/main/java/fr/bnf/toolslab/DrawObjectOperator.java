package fr.bnf.toolslab;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.contentstream.operator.MissingOperandException;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.operator.graphics.GraphicsOperatorProcessor;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.MissingResourceException;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Operator to take place of the normal one.
 *
 */
public class DrawObjectOperator extends GraphicsOperatorProcessor {
  protected static final Logger LOGGER = Logger.getLogger(DrawObjectOperator.class.getName());

  @Override
  public void process(Operator operator, List<COSBase> operands) throws IOException {
    if (operands.isEmpty()) {
      throw new MissingOperandException(operator, operands);
    }
    COSBase base0 = operands.get(0);
    if (!(base0 instanceof COSName)) {
      return;
    }
    COSName objectName = (COSName) base0;
    PDResources pdResources = context.getResources();
    // Don't let the image to be build
    if (pdResources.isImageXObject(objectName)) {
      LOGGER.fine("Found image for " + objectName);
      COSDictionary resc = pdResources.getCOSObject();
      COSDictionary dict = (COSDictionary) resc.getDictionaryObject(COSName.XOBJECT);
      COSStream stream = dict.getCOSStream(objectName);
      PDImage image = new PDImageWrap(new PDStream(stream), pdResources);
      LOGGER.fine("Build PDImageWrap for " + objectName);
      context.drawImage(image);
      LOGGER.fine("DrawImage for " + objectName);
      return;
    }
    PDXObject xobject = context.getResources().getXObject(objectName);

    if (xobject == null) {
      throw new MissingResourceException("Missing XObject: " + objectName.getName());
    } else if (xobject instanceof PDImageXObject) {
      LOGGER.fine("DrawObject image " + xobject);
      PDImageXObject image = (PDImageXObject) xobject;
      context.drawImage(image);
    } else if (xobject instanceof PDFormXObject) {
      try {
        context.increaseLevel();
        if (context.getLevel() > 50) {
          LOGGER.severe("recursion is too deep, skipping form XObject");
          return;
        }
        if (xobject instanceof PDTransparencyGroup) {
          context.showTransparencyGroup((PDTransparencyGroup) xobject);
        } else {
          context.showForm((PDFormXObject) xobject);
        }
      } finally {
        context.decreaseLevel();
      }
    }
  }

  @Override
  public String getName() {
    return OperatorName.DRAW_OBJECT;
  }
}
