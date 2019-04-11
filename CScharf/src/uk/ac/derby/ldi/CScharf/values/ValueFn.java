package uk.ac.derby.ldi.CScharf.values;

import uk.ac.derby.ldi.CScharf.interpreter.*;

public class ValueFn extends ValueAbstract {
	private FunctionDefinition funcDef = null;
	
	public String getName() {
		return "Function";
	}
	
	public int compare(Value v) {
		//return v.compare(v)
		//return v.compareTo(((FunctionDefinition)o).name);
		return 0;
	}
	
	public void setFunctionDefinition(FunctionDefinition funcDef) {
		this.funcDef = funcDef;
	}
	
	public void invokeFunction() {
		FunctionInvocation funcInvoke = new FunctionInvocation(this.funcDef);
	}
	
//	public void setFunctionBody(SimpleNode node) {
//		System.out.println("Setting fn body.");
//		ASTFunctionBody = node;
//	}
//	
//	public void setFunctionReturnExpression(SimpleNode node) {
//		System.out.println("Setting fn ret.");
//		ASTFunctionReturnExpression = node;
//	}
//	
//	public boolean hasReturn() {
//		return (ASTFunctionReturnExpression != null);
//	}
//	
//	public String getSignature() {
//		return (hasReturn() ? "value " : "") + getName() + "(" + parmSignature + ")";
//	}
//	
//	/** Get count of parameters. */
//	public int getParameterCount() {
//		return parameters.size();
//	}
//	
//	/** Get the name of the ith parameter. */
//	String getParameterName(int i) {
//		return parameters.get(i);
//	}
//	
//	/** Define a parameter. */
//	public void defineParameter(String name) {
//		if (parameters.contains(name))
//			throw new ExceptionSemantic("Parameter " + name + " already exists in function " + getName());
//		parameters.add(name);
//		parmSignature += ((parmSignature.length()==0) ? name : (", " + name));
//		defineVariable(name);
//	}
//	
//	/** Define a variable.  Return its slot number. */
//	private int defineVariable(String name) {
//		Integer slot = slots.get(name);
//		if (slot != null)
//			return slot.intValue();
//		int slotNumber = slots.size();
//		slots.put(name, Integer.valueOf(slotNumber));
//		return slotNumber;
//	}
}
