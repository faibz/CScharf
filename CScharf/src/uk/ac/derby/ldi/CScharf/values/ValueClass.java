package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueClass extends ValueAbstract {
	//Can add more stuff e.g. param count, to string, etc.
	private String name = "";
	private Map<String, Value> variables = new HashMap<String, Value>();
	//Map<String, FunctionDefinition> functions = new HashMap<String, FuntionDefinition>();
	
	public ValueClass(String className)	{
		name = className;
	}
	
	public String getName() {
		return name;
	}

	public int compare(Value v) {
		if (!(v instanceof ValueClass)) {
			throw new ExceptionSemantic("Class instances can only be compared to other class instances");	
		}
		
		return 1;
	}
	
	public void addVariable(String name, Value val) {
		if (variables.containsKey(name)) {
			throw new ExceptionSemantic("Parameter " + name + " already exists in class " + getName());
		}
		
		System.out.println("Adding new variable: " + name);
		
		variables.put(name, val);
	}
	
	public void addFunction() {
		
	}

}