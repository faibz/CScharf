package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rits.cloning.Cloner;

import uk.ac.derby.ldi.CScharf.interpreter.ClassDefinition;
import uk.ac.derby.ldi.CScharf.interpreter.ClassVariable;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;
import uk.ac.derby.ldi.CScharf.interpreter.FunctionDefinition;

public class ValueClass extends ValueAbstract implements ValueContainer {
	private UUID id = UUID.randomUUID();
	private Map<String, ClassVariable> variables = null;
	private Map<String, FunctionDefinition> functions = null;
	private Map<String, ClassDefinition> classes = null;
	
	private ClassDefinition classDefinition = null;
	
	//private Cloner cloner = new Cloner();
	
	public ValueClass()	{}
	
	public ValueClass(ClassDefinition classDef) {
		classDefinition = classDef;
		
		processVariables(classDef.getVariables());
		processFunctions(classDef.getFunctions());
		processClasses(classDef.getClasses());
	}

	private void processVariables(HashMap<String, ClassVariable> variables) {
		System.out.println("hello?");
		//this.variables = cloner.deepClone(variables);
		if (variables == null || variables.isEmpty()) return;
		Gson gson = new Gson();
		String jsonString = gson.toJson(variables);
		java.lang.reflect.Type type = new TypeToken<HashMap<String, ClassVariable>>(){}.getType();
		this.variables = gson.fromJson(jsonString, type);
	}

	private void processFunctions(HashMap<String, FunctionDefinition> functions) {
		//this.functions = cloner.deepClone(functions);
//		if (functions == null || functions.isEmpty()) return;
//		
//		Gson gson = new Gson();
//		String jsonString = gson.toJson(functions);
//		java.lang.reflect.Type type = new TypeToken<HashMap<String, FunctionDefinition>>(){}.getType();
//		this.functions = gson.fromJson(jsonString, type);
	}
	
	private void processClasses(HashMap<String, ClassDefinition> classes) {
		//this.classes = new Cloner().deepClone(classes);
//		if (classes == null || classes.isEmpty()) return;
//		Gson gson = new Gson();
//		String jsonString = gson.toJson(classes);
//		java.lang.reflect.Type type = new TypeToken<HashMap<String, ClassDefinition>>(){}.getType();
//		this.classes = gson.fromJson(jsonString, type);
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
		
		variables.get(name).setValue(value);
	}
	
	public Value getVariable(String name) {
		if (!variables.containsKey(name)) {
			throw new ExceptionSemantic("Variable " + name + " does not exist in class " + getName() + ".");
		}
		
		return variables.get(name).getValue();
	}
		
	
	public String toString() {
		//TODO put var data in here too
		
		System.out.println("Flex val: " + variables.get("flex").getValue().longValue());
		
		return id.toString();
	}

}