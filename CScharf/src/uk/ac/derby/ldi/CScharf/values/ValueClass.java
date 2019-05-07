package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import uk.ac.derby.ldi.CScharf.interpreter.ClassDefinition;
import uk.ac.derby.ldi.CScharf.interpreter.ClassVariable;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;
import uk.ac.derby.ldi.CScharf.interpreter.FunctionDefinition;

public class ValueClass extends ValueAbstract implements ValueContainer {
	private UUID id = UUID.randomUUID();
	private boolean inConstructor = false;
	private Map<String, ClassVariable> variables = new HashMap<String, ClassVariable>();
	private Map<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
	private Map<String, ClassDefinition> classes = new HashMap<String, ClassDefinition>();
	
	private ClassDefinition classDefinition = null;
		
	public ValueClass()	{}
	
	public ValueClass(ClassDefinition classDef) {
		classDefinition = classDef;
		
		processVariables(classDef.getVariablesCopy());
		processFunctions(classDef.getFunctionsCopy());
		processClasses(classDef.getClassesCopy());
	}
	
	private void processVariables(HashMap<String, ClassVariable> variables) {
		this.variables = variables;
	}
	
	private void processFunctions(HashMap<String, FunctionDefinition> functions) {
		this.functions = functions;
	}
	
	private void processClasses(HashMap<String, ClassDefinition> classes) {
		this.classes = classes;
	}

	public String getName() {
		return classDefinition.getName();
	}
	
	public ClassDefinition getClassDefinition() {
		return classDefinition;
	}
	
	public int compare(Value v) {
		if (!(v instanceof ValueClass)) {
			throw new ExceptionSemantic("Class instances can only be compared to other class instances.");	
		}
		
		return 1;
	}
	
	public void setVariable(String name, Value value) {
		if (!variables.containsKey(name)) {
			throw new ExceptionSemantic("Variable " + name + " does not exist in class " + getName() + ".");
		}

		variables.get(name).setValue(value, inConstructor);
	}
	
	public Value getVariable(String name) {
		if (!variables.containsKey(name)) {
			throw new ExceptionSemantic("Variable " + name + " does not exist in class " + getName() + ".");
		}
		
		return variables.get(name).getValue();
	}
	
	public FunctionDefinition getFunction(String name) {
		if (!functions.containsKey(name)) {
			throw new ExceptionSemantic("Function " + name + " does not exist in class " + getName() + ".");
		}
		
		return functions.get(name);
	}
	
	public ClassDefinition getClass(String name) {
		if (!classes.containsKey(name)) {
			throw new ExceptionSemantic("Class " + name + " does not exist in class " + getName() + ".");
		}
		
		return classes.get(name);
	}
	
	public String toString() {
		//TODO put var data in here too
		
		return id.toString();
	}
	
	public void setInConstructor(boolean val) {
		this.inConstructor = val;
	}

}