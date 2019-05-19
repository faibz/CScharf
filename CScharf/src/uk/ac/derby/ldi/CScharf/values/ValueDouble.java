package uk.ac.derby.ldi.CScharf.values;

public class ValueDouble extends ValueAbstract {

	private double internalValue;
	
	public ValueDouble(double b) {
		internalValue = b;
	}
	
	public ValueDouble(float b) {
		internalValue = (double) b;
	}
	
	public String getName() {
		return "rational";
	}
	
	/** Convert this to a primitive float. */
	public float floatValue() {
		return (float) internalValue;
	}
	
	/** Convert this to a primitive double */
	public double doubleValue() {
		return (double) internalValue;
	}
	
	/** Convert this to a primitive String. */
	public String stringValue() {
		return "" + internalValue;
	}

	public int compare(Value v) {
		if (internalValue == v.floatValue())
			return 0;
		else if (internalValue > v.floatValue())
			return 1;
		else
			return -1;
	}
	
	public Value add(Value v) {
		return new ValueDouble(internalValue + v.doubleValue());
	}

	public Value subtract(Value v) {
		return new ValueDouble(internalValue - v.floatValue());
	}

	public Value mult(Value v) {
		return new ValueDouble(internalValue * v.floatValue());
	}

	public Value div(Value v) {
		return new ValueDouble(internalValue / v.floatValue());
	}
	
	public Value mod(Value v) {
		return new ValueDouble(internalValue % v.floatValue());
	}

	public Value unary_plus() {
		return new ValueDouble(internalValue);
	}

	public Value unary_minus() {
		return new ValueDouble(-internalValue);
	}
	
	public String toString() {
		return "" + internalValue;
	}
}
