package fr.bnf.toolslab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
   * Getter for the file.
   *
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * Setter for the file.
   *
   * @param file the file to set
   */
  public void setFile(File file) {
    this.file = file;
  }

  /**
   * Boolean to know if the file is valid.
   * @return isValid validity of the file
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Setter for the validity of the file.
   * 
   * @param isValid boolean to set
   */
  public void setValid(boolean isValid) {
    this.isValid = isValid;
  }

  /**
   * Getter for the number of pages.
   *
   * @return page counter
   */
  public int getNbPages() {
    return nbPages;
  }

  /**
   * Setter for the number of pages.
   * 
   * @param nbPages page counter
   */
  public void setNbPages(int nbPages) {
    this.nbPages = nbPages;
  }

  /**
   * Getter for the number of images.
   *
   * @return image counter
   */
  public int getNbImages() {
    return nbImages;
  }

  /**
   * Setter for the number of pages.
   * 
   * @param nbImages image counter
   */
  public void setNbImages(int nbImages) {
    this.nbImages = nbImages;
  }

  /**
   * Boolean to know if the document is scan-based.
   * @return isScan boolean to indicate if it's a scanned PDF
   */
  public boolean isScan() {
    return isScan;
  }

  /**
   * Setter for the scanned indicator.
   * @param isScan boolean to indicate whether it's a scanned PDF
   */
  public void setScan(boolean isScan) {
    this.isScan = isScan;
  }

  /**
   * Getter for the resolution of the scan.
   * 
   * @return resolution in DPI
   */
  public int getResolution() {
    return resolution;
  }

  /**
   * Setter for the resolution.
   * 
   * @param resolution the resolution to set
   */
  public void setResolution(int resolution) {
    this.resolution = resolution;
  }

  /**
   * Getter for the time of processing.
   *
   * @return timeToProcess time of processing in ms
   */
  public long getTimeToProcess() {
    return timeToProcess;
  }

  /**
   * Setter for the time of processing.
   * 
   * @param timeToProcess the timeToProcess to set
   */
  public void setTimeToProcess(long timeToProcess) {
    this.timeToProcess = timeToProcess;
  }

  /**
   * Get a header for a list of file descriptions.
   * 
   * @return string to display
   */
  public static String headString() {
    final String[] headers = {
        "FILENAME","PROCESSING TIME","VALID","NB PAGES","NB IMAGES","TYPE","RESOLUTION"};
    return String.join(SEP, headers);
  }

  /**
   * Get a string representation of the file description.
   * 
   * @return string to display
   */
  public String toString() {
    List<String> values = new ArrayList<>(10);
    values.add(file.getName());
    values.add(Long.toString(timeToProcess));
    if (isValid) {
      values.add("valid");
      values.add(Integer.toString(nbPages));
      values.add(Integer.toString(nbImages));
      if (isScan) {
        values.add("scanned pdf");
        values.add(Integer.toString(resolution));
      } else {
        values.add("native pdf");
      }
    } else {
      values.add("not valid");

    }
    return String.join(SEP, values);
  }
}
