package uk.ac.derby.ldi.CScharf.values;

import uk.ac.derby.ldi.CScharf.interpreter.*;

public class ValueFn extends ValueAbstract {
	private FunctionDefinition funcDef = null;
	
	public String getName() {
		return "Function";
	}
	
	public int compare(Value v) {
		return 0;
	}
	
	public void setFunctionDefinition(FunctionDefinition funcDef) {
		this.funcDef = funcDef;
	}
	
	public FunctionDefinition getFunctionDefinition() {
		return this.funcDef;
	}
}
