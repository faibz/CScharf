package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.values.Modifier;
import uk.ac.derby.ldi.CScharf.values.Value;

public class ClassVariable {
	private Modifier modifier = Modifier.NONE;
	private Class<?> type = null;
	private Value value = null;
	
	public ClassVariable(Modifier modifier, Class<?> type) {
		this.modifier = modifier;
		this.type = type;
		this.value = CScharfUtil.getDefaultValueForClass(type);
	}
	
	public ClassVariable(Modifier modifier, Class<?> type, Value defaultValue) {
		this.modifier = modifier;
		this.type = type;
		this.value = defaultValue;
	}
	
	//copy constructor
	public ClassVariable(ClassVariable val) {
		this.modifier = val.modifier;
		this.type = val.type;
		this.value = val.value;
	}

	public Value getValue() {
		return value;
	}
	
	public void setValue(Value val, boolean inConstructor) {
		if (modifier == Modifier.CONSTANT)
			throw new ExceptionSemantic("Cannot set the value of a constant variable");
		
		if (modifier == Modifier.READONLY && !inConstructor) {
			throw new ExceptionSemantic("Cannot modify readonly variable value outside of constructor.");
		}
		
		value = val;
	}
	
	public String toString() {
		return "Modifier=" + modifier.toString() + "|Type=" + type;
	}
}
