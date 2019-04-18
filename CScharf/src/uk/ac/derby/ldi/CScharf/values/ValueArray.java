package uk.ac.derby.ldi.CScharf.values;

import java.util.UUID;
import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueArray extends ValueAbstract {
	private UUID id = UUID.randomUUID();
	private int length = 0;
	private Class<?> type;
	private Vector<Value> data = new Vector<Value>();

	public ValueArray() {}
	
	public ValueArray(String type, int length) {
		this.type = CScharfUtil.getClassFromString(type);
		this.length = length;
		
		for (int i = 0; i < length; ++i) {
			data.add(i, CScharfUtil.getDefaultValueForClass(this.type));
		}
	}

	public String getName() {
		return "Array";
	}

	public int compare(Value v) {
		//TODO: compare all values
		
		return 0;
	}
	
	public void putValue(int index, Value val) {
		if(validIndex(index) && val.getClass().equals(type)) {
			data.add(index, val);
		} else {
			throw new ExceptionSemantic("Index '" + index + "' is out of bounds of the array.");
		}
	}
	
	public Value getValue(int index) {
		if(validIndex(index)) {
			return data.get(index);
		} else {
			throw new ExceptionSemantic("Index '" + index + "' is out of bounds of the array.");
		}
	}

	private boolean validIndex(int index) {
		return index >= 0 && index < length;
	}
	
	public String toString() {
		return id.toString();
	}
}