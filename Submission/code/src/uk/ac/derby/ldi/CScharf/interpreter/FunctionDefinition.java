package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.parser.ast.SimpleNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

/** This class captures information about the function currently being defined.
 * 
 * @author dave
 *
 */
public class FunctionDefinition implements Comparable<Object> {
	private String name;
	private String parmSignature = "";
	private HashMap<String, Class<?>> parameters = new LinkedHashMap<String, Class<?>>();
	private HashMap<String, Integer> slots = new HashMap<String, Integer>();
	private HashMap<String, FunctionDefinition> functions = new HashMap<String, FunctionDefinition>();
	private SimpleNode ASTFunctionBody = null;
	private SimpleNode ASTFunctionReturnExpression = null;
	private int depth;
	private Class<?> returnType = null;
	
	/** Ctor for function definition. */
	FunctionDefinition(String functionName, int level) {
		name = functionName;
		depth = level;
	}
	
	//copy constructor
	public FunctionDefinition(FunctionDefinition value) {
		this.name = value.getName();
		this.parmSignature = value.parmSignature;
		this.parameters = value.parameters;
		this.slots = value.slots;
		this.functions = value.functions;		
		this.ASTFunctionBody = value.ASTFunctionBody;
		this.ASTFunctionReturnExpression = value.ASTFunctionReturnExpression;
		this.depth = value.depth;
		this.returnType = value.returnType;
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
	void setFunctionBody(SimpleNode node) {
		ASTFunctionBody = node;
	}
	
	/** Get the function body of this function. */
	SimpleNode getFunctionBody() {
		return ASTFunctionBody;
	}
	
	/** Set the return expression of this function. */
	void setFunctionReturnExpression(SimpleNode node) {
		ASTFunctionReturnExpression = node;
	}
	
	/** Get the return expression of this function. */
	SimpleNode getFunctionReturnExpression() {
		return ASTFunctionReturnExpression;
	}
	
	/** Get the signature of this function. */
	String getSignature() {
		return (hasReturn() ? "value " : "") + getName() + "(" + parmSignature + ")";
	}
	
	/** True if this function has a return value. */
	boolean hasReturn() {
		return (ASTFunctionReturnExpression != null);
	}
	
	/** Comparison operator.  Functions of the same name are the same. */
	public int compareTo(Object o) {
		return name.compareTo(((FunctionDefinition)o).name);
	}
	
	/** Get count of parameters. */
	int getParameterCount() {
		return parameters.size();
	}
	
	/** Get the name of the ith parameter. */
	String getParameterName(int i) {
		return (String)parameters.keySet().toArray()[i];
	}
	
	Class<?> getParameterType(int i) {
		return (Class<?>)parameters.values().toArray()[i];
	}
	
	Vector<Class<?>> getParameterTypes() {
		return new Vector<Class<?>>(parameters.values());
	}
	
	/** Define a parameter. */
	void defineParameter(String type, String name) {
		if (parameters.containsKey(name))
			throw new ExceptionSemantic("Parameter " + name + " already exists in function " + getName());
		
		parameters.put(name, CScharfUtil.getClassFromString(type));
		parmSignature += ((parmSignature.length()==0) ? type + " " + name : (", " + type + " " + name));
		defineVariable(name);
	}
	
	/** Get count of local variables and parameters. */
	int getLocalCount() {
		return slots.size();
	}
	
	Vector<String> getAllSlotKeys() {
		return new Vector<String>(slots.keySet());
	}
	
	void removeSlotKey(String name) {
		slots.remove(name);
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
		if (slot != null) return slot.intValue();
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
	
	Vector<FunctionDefinition> getFunctions() {
		return new Vector<FunctionDefinition>(functions.values());
	}
	
	void removeFunction(String name) {
		functions.remove(name);
	}

	void setReturnType(Class<?> returnType) {
		if (!hasReturn() && returnType != null) {
			throw new ExceptionSemantic("Cannot set return type for function without return expression.");
		}
		
		this.returnType = returnType;
	}
	
	Class<?> getReturnType() {
		return returnType;
	}
}
