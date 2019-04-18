package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.parser.ast.SimpleNode;
import uk.ac.derby.ldi.CScharf.values.Value;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.Serializable;

/** This class captures information about the class currently being defined. */

public class ClassDefinition implements Comparable<Object>, Serializable {
	private static final long serialVersionUID = 0;

	private String name;
	private String varSignature = "";
	private HashMap<String, ClassVariable> variables = new HashMap<String, ClassVariable>();
	private HashMap<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
	private HashMap<String, ClassDefinition> classes = new HashMap<String, ClassDefinition>();
	
	//Should I bother with this? Could just rummage around in the functions hashmap looking at signatures
	private FunctionDefinition constructor = null;
	private SimpleNode ASTClassBody = null;
	private int depth;
	
	/** Ctor for class definition. */
	ClassDefinition(String className, int level) {
		name = className;
		depth = level;
	}
	
	//copy constructor
	public ClassDefinition(ClassDefinition value) {
		this.name = value.getName();
		this.varSignature = value.getSignature();
		this.variables = value.getVariablesCopy();
		this.functions = value.getFunctionsCopy();
		this.classes = value.getClassesCopy();
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
	
	/** Get the name of this class. */
	public String getName() {
		return name;
	}
	
	public HashMap<String, ClassVariable> getVariables() {
		return variables;
	}
	
	public HashMap<String, FunctionDefinition> getFunctions() {
		return functions;
	}
	
	public HashMap<String, ClassDefinition> getClasses() {
		return classes;
	}
	
	public HashMap<String, ClassVariable> getVariablesCopy() {
		HashMap<String, ClassVariable> copy = new HashMap<String, ClassVariable>();
		
		for(Map.Entry<String, ClassVariable> entry : variables.entrySet()) {
			copy.put(entry.getKey(), new ClassVariable(entry.getValue()));
		}
		
		return copy;
	}
	
	public HashMap<String, FunctionDefinition> getFunctionsCopy() {
		HashMap<String, FunctionDefinition> copy = new HashMap<String, FunctionDefinition>();
		
		for(Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
			copy.put(entry.getKey(), new FunctionDefinition(entry.getValue()));
		}
		
		return copy;
	}
	
	public HashMap<String, ClassDefinition> getClassesCopy() {
		HashMap<String, ClassDefinition> copy = new HashMap<String, ClassDefinition>();
		
		for(Map.Entry<String, ClassDefinition> entry : classes.entrySet()) {
			copy.put(entry.getKey(), new ClassDefinition(entry.getValue()));
		}
		
		return copy;
	}
	
	/** Set the body of this class.
	 * child 0 - constructor
	 * child 1-x - assignment/fndef/classdef
	 *  */
	void setClassBody(SimpleNode node) {
		ASTClassBody = node;
	}
	
	/** Get the function body of this class. */
	SimpleNode getClassBody() {
		return ASTClassBody;
	}

	/** Get the signature of this class. */
	String getSignature() {
		return getName() + "(" + varSignature + ")";
	}
		
	/** Comparison operator. Classes of the same name are the same. */
	public int compareTo(Object o) {
		return name.compareTo(((ClassDefinition)o).name);
	}
	
	/** Get count of variables. */
	int getVariableCount() {
		return variables.size();
	}
		
	/** Define a variable. */
	void defineVariable(String type, String name, boolean constant, boolean priv, Value defaultValue) {
		if (variables.containsKey(name))
			throw new ExceptionSemantic("Variable " + name + " already exists in class " + getName());
		ClassVariable classVar = new ClassVariable(constant, priv, CScharfUtil.getClassFromString(type), defaultValue);
		variables.put(name, classVar);
		varSignature += ((varSignature.length()==0) ? name : (", " + name));
	}
	
	/** Add an inner function definition. */
	void addFunction(FunctionDefinition definition) {
		functions.put(definition.getName(), definition);
	}
	
	void addConstructor(FunctionDefinition definition) {
		constructor = definition;
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
