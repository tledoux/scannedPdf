package fr.bnf.toolslab;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.graphics.GraphicsOperatorProcessor;
import org.apache.pdfbox.cos.COSBase;

/**
 * Operator to take place of the normal one.
 *
 */
public class NoOpOperator extends GraphicsOperatorProcessor {
  protected static final Logger LOGGER = Logger.getLogger(NoOpOperator.class.getName());
  private final String operatorName;

  public NoOpOperator(String name) {
    operatorName = name;
  }

  @Override
  public void process(Operator operator, List<COSBase> operands) throws IOException {
    return;
  }

  @Override
  public String getName() {
    return operatorName;
  }
}
