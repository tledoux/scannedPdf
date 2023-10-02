package fr.bnf.toolslab;

import fr.bnf.toolslab.extractor.Extractor;
import fr.bnf.toolslab.extractor.Jpeg2000Extractor;
import fr.bnf.toolslab.extractor.JpegExtractor;
import fr.bnf.toolslab.extractor.TiffColorExtractor;
import fr.bnf.toolslab.extractor.TiffG4Extractor;
import fr.bnf.toolslab.extractor.TiffGreyExtractor;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Application to extract images from scanned PDFs.
 */
public class ExtractImageApp {
  protected static final Logger LOGGER = Logger.getLogger(ExtractImageApp.class.getName());

  private static final AtomicInteger NUM = new AtomicInteger(1);
  private File inputFile;
  private File outputDir;
  private boolean keepName = false;
  private String defaultProducer;

  protected void parseArgs(String[] args) throws IllegalArgumentException {
    String dest = ".";
    int index = 0;
    int previousIndex = -1;
    while (index < args.length - 1) {
      if (previousIndex == index) {
        throw new IllegalArgumentException("Problem with arguments list !!!");
      }
      previousIndex = index;
      if ("-log".equals(args[index])) { // Doesn't work, too late
        String logfile = args[index + 1];
        System.setProperty("java.util.logging.config.file", logfile);
        index += 2;
      } else if ("-init".equals(args[index])) {
        NUM.set(Integer.parseInt(args[index + 1]));
        index += 2;
        LOGGER.fine(String.format("Reading init %d at %d", NUM.get(), index));
      } else if ("-dest".equals(args[index])) {
        dest = args[index + 1];
        index += 2;
        LOGGER.fine(String.format("Reading dest %s at %d", dest, index));
      } else if ("-keep".equals(args[index])) {
        keepName = Boolean.parseBoolean(args[index + 1]);
        index += 2;
        LOGGER.fine(String.format("Reading keep %s at %d", keepName, index));
      } else if ("-producer".equals(args[index])) {
        defaultProducer = args[index + 1];
        index += 2;
        LOGGER.fine(String.format("DEfault producer %s at %d", defaultProducer, index));
      }
      LOGGER.fine(String.format("Parsing %d to %d", index, args.length));
    }
    outputDir = new File(dest);
    try {
      Files.createDirectories(outputDir.toPath());
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating " + dest + ": " + e.getMessage());
    }
    inputFile = new File(args[index]);
    LOGGER.info(String.format("Input file %s", inputFile.getName()));

    if (!inputFile.exists()) {
      throw new IllegalArgumentException(inputFile.getAbsolutePath() + " doesn't exist");
    }

  }


  protected void process() throws IOException {
    if (inputFile.isFile()) {
      int i = processFile(inputFile, outputDir);
      System.out.println(inputFile.getName() + " process " + i + " images");
    } else if (inputFile.isDirectory()) {
      // Retrieve only the .pdf files
      final PathMatcher pdfMatcher = FileSystems.getDefault().getPathMatcher("glob:*.pdf");
      Files.walkFileTree(inputFile.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (pdfMatcher.matches(file.getFileName())) {
            int i = processFile(file.toFile(), outputDir);
            LOGGER.info("DIR " + file.getFileName() + " process " + i + " images");
            if (!keepName) {
              int newNum = NUM.get() + i;
              NUM.set(newNum);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  protected Extractor findExtractor(PDImageXObject image, int dpiX, int dpiY) {
    List<COSName> filters = image.getStream().getFilters();
    if (filters.contains(COSName.CCITTFAX_DECODE)) {
      return new TiffG4Extractor(image, dpiX, dpiY);
    }
    if (filters.contains(COSName.DCT_DECODE)) {
      return new JpegExtractor(image, dpiX, dpiY);
    }
    if (filters.contains(COSName.JPX_DECODE)) {
      return new Jpeg2000Extractor(image);
    }
    if (filters.contains(COSName.FLATE_DECODE) || filters.contains(COSName.LZW_DECODE)
        || filters.contains(COSName.RUN_LENGTH_DECODE)) {
      PDColorSpace cspace = null;
      try {
        cspace = image.getColorSpace();
      } catch (IOException e) {
        LOGGER.warning("No colorspace in image to [" + inputFile.getName() + "]" + e.getMessage());
        return null;
      }
      int numberOfComponents = cspace.getNumberOfComponents();
      LOGGER.info(String.format("Processing image with %d components", numberOfComponents));
      if (numberOfComponents == 1) {
        return new TiffGreyExtractor(image, dpiX, dpiY);
      } else if (numberOfComponents == 3) {
        return new TiffColorExtractor(image, dpiX, dpiY);
      }
    }
    List<String> allfilters = new ArrayList<>();
    for (COSName name : filters) {
      allfilters.add(name.getName());
    }
    LOGGER.warning(String.format("Impossible to process image type %s with filters (%s)",
        image.getSuffix(), String.join(",", allfilters)));
    return null;

  }

  /**
   * Extract the images of a given PDF file.
   *
   * @param inputFile pdf file to read from
   * @param outputDir directory to save the images
   * @param keepName define if the images should be named after the input file or not
   * @return number of extracted images
   */
  protected int processFile(File inputFile, File outputDir) {
    LOGGER.info(String.format("ProcessFile %s to %s with %d", inputFile.getName(),
        outputDir.getName(), NUM.get()));
    int nbImages = 0;
    try (PDDocument document = PDDocument.load(inputFile)) {
      PDDocumentInformation info = document.getDocumentInformation();
      // String header = document.getVersion();
      LOGGER.info("Find document version " + document.getVersion());
      String producer = info.getProducer();
      if (producer == null && defaultProducer != null) {
        producer = defaultProducer;
      }
      Calendar creationDate = info.getCreationDate();
      if (creationDate == null) {
        creationDate = info.getModificationDate();
      }

      // Enumerate the pages
      for (PDPage page : document.getPages()) {
        PDResources resources = page.getResources();
        // Retrieve the dimension to calculate the resolution
        PDRectangle rectPage = page.getMediaBox(); // Found page dimension
        // MediaBox specified in "default user space units", which is points
        // (i.e. 72 dpi)
        float userUnit = page.getUserUnit(); // in multiples of 1/72 inch

        for (COSName c : resources.getXObjectNames()) {
          PDXObject o = resources.getXObject(c);
          if (o instanceof PDImageXObject) {

            PDImageXObject image = (PDImageXObject) o;
            LOGGER.info(String.format("Processing image %d", nbImages + 1));
            String extension = image.getSuffix();
            if (extension.equals("png")) {
              extension = "tiff"; // extract uncompressed TIFF instead of png
            }
            String outPrefix = "image";
            if (keepName) {
              String inputName = inputFile.getName();
              int idxLastPoint = inputName.lastIndexOf('.');
              if (idxLastPoint == -1) {
                outPrefix = inputName;
              } else {
                outPrefix = inputName.substring(0, idxLastPoint);
              }
            }
            String outName =
                String.format("%s_%03d.%s", outPrefix, NUM.get() + nbImages, extension);

            File outfile = new File(outputDir, outName);

            int width = image.getWidth();
            int height = image.getHeight();
            double dpiX = (double) width / rectPage.getWidth() * 72.0 / userUnit;
            double dpiY = (double) height / rectPage.getHeight() * 72.0 / userUnit;

            Extractor extractor = findExtractor(image, (int) dpiX, (int) dpiY);
            if (extractor != null) {
              extractor.setInfo(producer, creationDate);
              if (extractor.extract(outfile)) {
                nbImages++;
              }
              continue;
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error process file [" + inputFile.getName() + "] : " + e.getMessage());
      return 0;
    }
    return nbImages;
  }


  /**
   * Display the usage sentence.
   */
  public static void usage() {
    System.err.println("Usage : " + ExtractImageApp.class.getName()
        + " [-init 1] [-keep false] [-dest <outputPath>] <fileToExtractFrom>");
    System.exit(1);
  }


  /**
   * Main method.
   *
   * @param args arguments given in the command line
   * @throws IOException exception if error while accessing the files
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      usage();
      return;
    }

    ExtractImageApp app = new ExtractImageApp();
    try {
      app.parseArgs(args);
      app.process();
    } catch (IllegalArgumentException | IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
      return;
    }
  }

}
