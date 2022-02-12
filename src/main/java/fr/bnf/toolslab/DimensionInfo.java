package fr.bnf.toolslab;

/**
 * Utility class to handle dimensions.
 *
 */
public class DimensionInfo {

  public static final DimensionInfo EMPTY = new DimensionInfo(0, 0);

  protected long width;
  protected long height;

  public DimensionInfo(long w, long h) {
    this.width = w;
    this.height = h;
  }

  /**
   * Looks if two dimensions are almost equals (by the given percent).
   * 
   * @param otherDim the other dimension to compare with
   * @param percent tolerance of approximation
   * @return @true@ if dimensions almost equals
   */
  public boolean approximate(DimensionInfo otherDim, double percent) {
    double deltaX = 100.0 * Math.abs(this.width - otherDim.width) / this.width;
    double deltaY = 100.0 * Math.abs(this.height - otherDim.height) / this.height;
    return (deltaX <= percent) && (deltaY <= percent);
  }

  /**
   * Looks if the give dimension is smaller.
   * 
   * @param otherDim the other dimension to compare with
   * @return @true@ if the given dimension is contained in
   */
  public boolean contains(DimensionInfo otherDim) {
    return this.width >= otherDim.width && this.height >= otherDim.height;
  }

  public String toString() {
    return String.format("[%dx%d]", width, height);
  }

  public long getX() {
    return width;
  }

  public long getY() {
    return height;
  }

  public long getWidth() {
    return width;
  }

  public long getHeight() {
    return height;
  }
}
