package uk.ac.derby.ldi.CScharf.values;

public class ValueFloat extends ValueAbstract {

	private float internalValue;
	
	public ValueFloat(float b) {
		internalValue = b;
	}
	
	public ValueFloat(double obj) {
		internalValue = (float) obj;
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
		return new ValueFloat(internalValue + v.floatValue());
	}

	public Value subtract(Value v) {
		return new ValueFloat(internalValue - v.floatValue());
	}

	public Value mult(Value v) {
		return new ValueFloat(internalValue * v.floatValue());
	}

	public Value div(Value v) {
		return new ValueFloat(internalValue / v.floatValue());
	}
	
	public Value mod(Value v) {
		return new ValueFloat(internalValue % v.floatValue());
	}

	public Value unary_plus() {
		return new ValueFloat(internalValue);
	}

	public Value unary_minus() {
		return new ValueFloat(-internalValue);
	}
	
	public String toString() {
		return "" + internalValue;
	}
}
