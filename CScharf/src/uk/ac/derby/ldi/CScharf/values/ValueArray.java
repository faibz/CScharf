package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.UUID;

import uk.ac.derby.ldi.CScharf.CScharfUtil;

public class ValueArray extends ValueAbstract {
	private UUID id = UUID.randomUUID();
	private int length = 0;
	private Class<?> type;
	private HashMap<Integer, Value> data = new HashMap<Integer, Value>();

	public ValueArray(String type, int length) {
		this.type = CScharfUtil.getClassFromString(type);
		this.length = length;
	}

	public String getName() {
		return "Array";
	}

	public int compare(Value v) {
		return 0;
	}
	
	public void putValue(int index, Value val) {
		if(validIndex(index) && val.getClass().equals(type)) {
			data.put(index, val);
		}
	}
	
	public Value getValue(int index) {
		if(validIndex(index) && data.containsKey(index)) {
			return data.get(index);
		} else
			return null;
	}

	private boolean validIndex(int index) {
		return index >= 0 && index < length;
	}
}