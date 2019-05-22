package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Vector;

public class InterfaceDefinition {
	private String name;
	private Vector<InterfaceFunction> functions = new Vector<InterfaceFunction>(); 
	
	public InterfaceDefinition() {}
	
	public InterfaceDefinition(String name, Vector<InterfaceFunction> functions) {
		this.name = name;
		this.functions = functions;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setFunctions(Vector<InterfaceFunction> functions) {
		this.functions = functions;
	}
	
	public String getName() {
		return name;
	}
	
	public Vector<InterfaceFunction> getFunctions() {
		return functions;
	}
	
	public void addFunction(InterfaceFunction function) {
		functions.add(function);
	}
	
	public InterfaceFunction getFunction(String functionName) {
		for (var function : functions) {
			if (function.getName().equals(functionName)) return function;
		}
		
		return null;
	}
	
	public InterfaceFunction getFunction(InterfaceFunction func) {
		for (var function : functions) {
			if (function.equals(func)) return function;
		}
		
		return null;
	}
}
