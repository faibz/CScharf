package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.values.Value;

public class ClassVariable {
	private boolean constant = false;
	private boolean priv = false;
	private Class<?> type = null;
	private Value value = null;
	
	public ClassVariable(boolean constant, boolean priv, Class<?> type) {
		this.constant = constant;
		this.priv = priv;
		this.type = type;
		this.value = CScharfUtil.getDefaultValueForClass(type);
	}
	
	public ClassVariable(boolean constant, boolean priv, Class<?> type, Value defaultValue) {
		this.constant = constant;
		this.priv = priv;
		this.type = type;
		this.value = defaultValue;
	}
	
	//copy constructor
	public ClassVariable(ClassVariable val) {
		this.constant = val.constant;
		this.priv = val.priv;
		this.type = val.type;
		this.value = val.value;
	}

	public Value getValue() {
		return value;
	}
	
	public void setValue(Value val) {
		value = val;
	}
	
	public String toString() {
		return "Constant=" + constant + "|Private=" + priv + "|Type=" + type;
	}
}
