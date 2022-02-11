package fr.bnf.toolslab;

import java.io.File;
import java.io.IOException;

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

	public static void main(String[] args) {
		boolean alternate = false;
		if (args.length < 1) {
			usage();
			return;
		}
		int index = 0;
		if ("-alt".equals(args[0])) {
			alternate = true;
			index++;
		}

		File fIn = new File(args[index]);
		if (!fIn.exists()) {
			System.err.println(fIn.getAbsolutePath() + " doesn't exist");
			System.exit(1);
			return;
		}

		AbstractScanDetector detector = new PdfBoxScanDetector();
		if (alternate) {
			detector = new AlternatePdfBoxScanDetector();
		}

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
			File[] files = fIn.listFiles((file) -> {
				if (!file.canRead()) {
					return false;
				}
				String filename = file.getName();
				return filename.endsWith(".pdf");
			});
			if (files == null) {
				return;
			}
			System.out.println(FileDescriptor.headString());
			for (File f : files) {
				FileDescriptor fd = new FileDescriptor(f);
				detector.init(fd);
				try {
					detector.parse();
				} catch (IOException e) {
					System.err.println("Error process file " + e.getMessage());
					fd.setValid(false);
				}
				System.out.println(fd.toString());
			}
		}
	}
}
