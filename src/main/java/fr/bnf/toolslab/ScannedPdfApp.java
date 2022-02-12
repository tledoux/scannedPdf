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
 * ScannedPdf
 *
 * Application to detect scanned pdfs
 */
public class ScannedPdfApp {
	public static void usage() {
		System.err.println("Usage : " + ScannedPdfApp.class.getName()
				+ " <fileToTest>");
		System.exit(1);
	}

	public static void main(String[] args) throws IOException {
		boolean useAlternate = false;
		if (args.length < 1) {
			usage();
			return;
		}
		int index = 0;
		if ("-alt".equals(args[0])) {
			useAlternate = true;
			index++;
		}

		File fIn = new File(args[index]);
		if (!fIn.exists()) {
			System.err.println(fIn.getAbsolutePath() + " doesn't exist");
			System.exit(1);
			return;
		}

		final AbstractScanDetector detector = useAlternate ? new AlternatePdfBoxScanDetector()
				: new PdfBoxScanDetector();

		if (fIn.isFile()) {
			FileDescriptor fd = new FileDescriptor(fIn);
			detector.init(fd);
			try {
				detector.parse();
			} catch (IOException e) {
				System.err.println("Error process file " + e.getMessage());
				fd.setValid(false);
			}
			System.out.println(FileDescriptor.headString());
			System.out.println(fd.toString());

		} else if (fIn.isDirectory()) {
			// Retrieve only the .pdf files
			final PathMatcher pdfMatcher = FileSystems.getDefault()
					.getPathMatcher("glob:*.pdf");
			System.out.println(FileDescriptor.headString());
			Files.walkFileTree(fIn.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) {
					if (pdfMatcher.matches(file.getFileName())) {
						FileDescriptor fd = new FileDescriptor(file.toFile());
						detector.init(fd);
						try {
							detector.parse();
						} catch (IOException e) {
							System.err.println("Error process file "
									+ e.getMessage());
							fd.setValid(false);
						}
						System.out.println(fd.toString());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
}
