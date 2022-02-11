package fr.bnf.toolslab;

public class DimensionInfo {

	public static final DimensionInfo EMPTY = new DimensionInfo(0, 0);
	
	public long x;
	public long y;

	public DimensionInfo(long x, long y) {
		this.x = x;
		this.y = y;
	}

	public boolean approximate(DimensionInfo dim2, double percent) {
		double deltaX = 100.0 * Math.abs(this.x - dim2.x) / this.x;
		double deltaY = 100.0 * Math.abs(this.y - dim2.y) / this.y;
		return (deltaX <= percent) && (deltaY <= percent);
	}

	public boolean contains(DimensionInfo dim2) {
		return this.x >= dim2.x && this.y >= dim2.y;
	}

	public String toString() {
		return toString("Dimension");
	}

	public String toString(String attr) {
		return "[" + x + 'x' + y + ']';
	}
}
