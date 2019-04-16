package uk.ac.derby.ldi.CScharf.interpreter;

public class ClassVariable {
	boolean constant = false;
	boolean priv = false;
	Class type = null;
	
	public ClassVariable(boolean constant, boolean priv, Class type) {
		this.constant = constant;
		this.priv = priv;
		this.type = type;
	}
	
	public String toString() {
		return "Constant=" + constant + "|Private=" + priv + "|Type=" + type;
	}
}
