package fr.bnf.toolslab;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Application to detect scanned PDFs.
 */
public class ScannedPdfApp {

  public static void usage() {
    System.err.println("Usage : " + ScannedPdfApp.class.getName() + " <fileOrDirectoryToTest>");
    System.exit(1);
  }

  /**
   * Main method.
   *
   * @param args arguments given in the command line
   * @throws IOException exception if error while accessing the files
   */
  public static void main(String[] args) throws IOException {
    boolean useAlternate = false;
    boolean useStream = false;
    boolean useStrict = false;
    if (args.length < 1) {
      usage();
      return;
    }
    int index = 0;
    if ("-alt".equals(args[0])) {
      useAlternate = true;
      index++;
    } else if ("-stream".equals(args[0])) {
      useStream = true;
      index++;
    }
    else if ("-strict".equals(args[0])) {
      useStrict = true;
      index++;
    }

    File inputFile = new File(args[index]);
    if (!inputFile.exists()) {
      System.err.println(inputFile.getAbsolutePath() + " doesn't exist");
      System.exit(1);
      return;
    }

    final AbstractScanDetector detector =
        useAlternate ? new AlternatePdfBoxScanDetector()
            : (useStream ? new StreamPdfBoxScanDetector()
                : (useStrict ? new StrictPdfBoxScanDetector() : new PdfBoxScanDetector()));

    if (inputFile.isFile()) {
      FileDescriptor fd = processFile(inputFile, detector);
      System.out.println(FileDescriptor.headString());
      System.out.println(fd.toString());

    } else if (inputFile.isDirectory()) {
      // Retrieve only the .pdf files
      final PathMatcher pdfMatcher = FileSystems.getDefault().getPathMatcher("glob:*.pdf");
      System.out.println(FileDescriptor.headString());
      Files.walkFileTree(inputFile.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (pdfMatcher.matches(file.getFileName())) {
            FileDescriptor fd = processFile(file.toFile(), detector);
            System.out.println(fd.toString());
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  /**
   * Process a given file.
   *
   * @param inputFile file to process
   * @param detector detector to use
   * @return the description of the file
   */
  private static FileDescriptor processFile(File inputFile, final AbstractScanDetector detector) {
    FileDescriptor fd = new FileDescriptor(inputFile);
    detector.init(fd);
    try {
      detector.parse();
    } catch (IOException e) {
      System.err.println("Error process file " + e.getMessage());
      fd.setValid(false);
    }
    return fd;
  }
}
