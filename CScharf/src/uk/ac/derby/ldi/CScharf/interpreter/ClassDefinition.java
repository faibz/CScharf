package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Vector;

import uk.ac.derby.ldi.CScharf.parser.ast.SimpleNode;

import java.util.HashMap;
import java.io.Serializable;

/** This class captures information about the class currently being defined. */

class ClassDefinition implements Comparable<Object>, Serializable {
	private static final long serialVersionUID = 0;

	private String name;
	private String parmSignature = "";
	private Vector<String> variables = new Vector<String>();
	private HashMap<String, Integer> slots = new HashMap<String, Integer>();
	private HashMap<String, ClassDefinition> classes = new HashMap<String, ClassDefinition>();
	private HashMap<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
	private SimpleNode ASTClassBody = null;
	private int depth;
	
	/** Ctor for class definition. */
	ClassDefinition(String className, int level) {
		name = className;
		depth = level;
	}
	
	/** Get the depth of this definition.
	 * 0 - root or main scope
	 * 1 - definition inside root or main scope
	 * 2 - definition inside 1
	 * n - etc.
	 */
	int getLevel() {
		return depth;
	}
	
	/** Get the name of this function. */
	String getName() {
		return name;
	}
	
	/** Set the function body of this function. */
	void setClassBody(SimpleNode node) {
		ASTClassBody = node;
	}
	
	/** Get the function body of this function. */
	SimpleNode getClassBody() {
		return ASTClassBody;
	}
		
	/** Get the signature of this function. */
	String getSignature() {
		return getName() + "(" + parmSignature + ")";
	}
		
	/** Comparison operator.  Functions of the same name are the same. */
	public int compareTo(Object o) {
		return name.compareTo(((ClassDefinition)o).name);
	}
	
	/** Get count of parameters. */
	int getParameterCount() {
		return variables.size();
	}
	
	/** Get the name of the ith parameter. */
	String getParameterName(int i) {
		return variables.get(i);
	}
	
	/** Define a variable. */
	void defineVar(String name) {
		if (variables.contains(name))
			throw new ExceptionSemantic("Parameter " + name + " already exists in class " + getName());
		variables.add(name);
		parmSignature += ((parmSignature.length()==0) ? name : (", " + name));
		defineVariable(name);
	}
	
	/** Get count of local variables and parameters. */
	int getLocalCount() {
		return slots.size();
	}
	
	/** Get the storage slot number of a given variable or parm.  Return -1 if it doesn't exist. */
	int getLocalSlotNumber(String name) {
		Integer slot = slots.get(name);
		if (slot == null)
			return -1;
		return slot.intValue();
	}
	
	/** Define a variable.  Return its slot number. */
	int defineVariable(String name) {
		Integer slot = slots.get(name);
		if (slot != null)
			return slot.intValue();
		int slotNumber = slots.size();
		slots.put(name, Integer.valueOf(slotNumber));
		return slotNumber;
	}	
	
	/** Add an inner function definition. */
	void addFunction(FunctionDefinition definition) {
		functions.put(definition.getName(), definition);
	}
	
	/** Find an inner function definition.  Return null if it doesn't exist. */
	FunctionDefinition findFunction(String name) {
		return functions.get(name);
	}
	
	/** Add a nested class definition. */
	void addClass(ClassDefinition definition) {
		classes.put(definition.getName(), definition);
	}
	
	/** Find a nested class definition. return null if it doesn't exist. */
	ClassDefinition findClass(String name) {
		return classes.get(name);
	}

}
