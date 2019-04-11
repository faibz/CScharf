package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;

import uk.ac.derby.ldi.CScharf.interpreter.ClassDefinition;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueClass extends ValueAbstract {
	private Map<String, Value> variables = new HashMap<String, Value>();
	private ClassDefinition classDef = null;
	
	public ValueClass()	{
	}
	
	public String getName() {
		return "Class";
	}
	
	public ClassDefinition getClassDefinition() {
		return classDef;
	}
	
	public void setClassDefinition(ClassDefinition classDef) {
		this.classDef = classDef;
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