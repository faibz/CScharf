package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueArray extends ValueAbstract {
	private UUID id = UUID.randomUUID();
	private int length = 0;
	private Class<?> type;
	private HashMap<Integer, Value> data = new HashMap<Integer, Value>();

	public ValueArray() {}
	
	public ValueArray(String type, int length) {
		this.type = CScharfUtil.getClassFromString(type);
		this.length = length;
		
		for (int i = 0; i < length; ++i) {
			data.put(i, CScharfUtil.getDefaultValueForClass(this.type));
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
		if(validIndex(index)) {
			if (val.getClass().equals(type)) {
				//System.out.println("Inserting value " + val + " into index " + index);
				data.put(index, val);
			} else {
				throw new ExceptionSemantic("Cannot assign value of " + val.getClass() + " to array of type " + type);
			}
		} else {
			throw new ExceptionSemantic("Index '" + index + "' is out of bounds of the array. Array length is: " + length);
		}
	}
	
	public Value getValue(int index) {
		if(validIndex(index)) {
			return data.get(index);
		} else {
			throw new ExceptionSemantic("Index '" + index + "' is out of bounds of the array. Array length is: " + length);
		}
	}

	private boolean validIndex(int index) {
		return index >= 0 && index < length;
	}
	
	public String toString() {
		return id.toString();
	}
}