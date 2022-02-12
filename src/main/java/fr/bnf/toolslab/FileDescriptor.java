package fr.bnf.toolslab;

import java.io.File;

public class FileDescriptor {
	protected static final String SEP = ";";

	File file;
	boolean isValid;
	int nbPages;
	int nbImages;

	boolean isScan;
	int resolution;

	long timeToProcess;

	public FileDescriptor(File f) {
		this.file = f;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @return the isValid
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * @param isValid
	 *            the isValid to set
	 */
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	/**
	 * @return the nbPages
	 */
	public int getNbPages() {
		return nbPages;
	}

	/**
	 * @param nbPages
	 *            the nbPages to set
	 */
	public void setNbPages(int nbPages) {
		this.nbPages = nbPages;
	}

	/**
	 * @return the nbImages
	 */
	public int getNbImages() {
		return nbImages;
	}

	/**
	 * @param nbImages
	 *            the nbImages to set
	 */
	public void setNbImages(int nbImages) {
		this.nbImages = nbImages;
	}

	/**
	 * @return the isScan
	 */
	public boolean isScan() {
		return isScan;
	}

	/**
	 * @param isScan
	 *            the isScan to set
	 */
	public void setScan(boolean isScan) {
		this.isScan = isScan;
	}

	/**
	 * @return the resolution
	 */
	public int getResolution() {
		return resolution;
	}

	/**
	 * @param resolution
	 *            the resolution to set
	 */
	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	/**
	 * @return the timeToProcess
	 */
	public long getTimeToProcess() {
		return timeToProcess;
	}

	/**
	 * @param timeToProcess
	 *            the timeToProcess to set
	 */
	public void setTimeToProcess(long timeToProcess) {
		this.timeToProcess = timeToProcess;
	}

	public static String headString() {
		return "FILENAME;PROCESSING TIME;VALID;NB PAGES;NB IMAGES;TYPE;RESOLUTION";
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(file.getName());
		sb.append(SEP).append(timeToProcess);
		if (isValid) {
			sb.append(SEP).append("valid");
			sb.append(SEP).append(nbPages);
			sb.append(SEP).append(nbImages);
			if (isScan) {
				sb.append(SEP).append("scanned pdf");
				sb.append(SEP).append(resolution);
			} else {
				sb.append(SEP).append("native pdf");
			}
		} else {
			sb.append(SEP).append("not valid");

		}
		return sb.toString();
	}
}
