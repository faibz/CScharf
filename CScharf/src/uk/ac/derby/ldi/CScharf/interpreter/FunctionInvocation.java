package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Vector;

import uk.ac.derby.ldi.CScharf.values.Value;

/** Function invocation context. */
public class FunctionInvocation implements Invocation {

	private FunctionDefinition function;
	private int argumentCount = 0;
	private Vector<Value> slots;
	
	private final void setSlot(int n, Value v) {
		if (n >= slots.size())
			slots.setSize(n + 1);
		
		Value existingValue = slots.get(n);
		
		if (existingValue != null) {
			if (existingValue.isConst())
				throw new ExceptionSemantic("Cannot re-assign to constant value.");
		}
		
		slots.set(n, v);
	}
	
	/** Ctor for user-defined function. */
	public FunctionInvocation(FunctionDefinition fndef) {
		function = fndef;
		slots = new Vector<Value>(function.getLocalCount());
	}
	
	/** Get the level of the associated function. */
	int getLevel() {
		return function.getLevel();
	}
	
	Vector<String> getSlotKeys() {
		return function.getAllSlotKeys();
	}
	
	void removeSlot(String name) {
		function.removeSlotKey(name);
	}
	
	/** Set an argument value. */
	void setArgument(Value v) {
		if (argumentCount >= function.getParameterCount())
			throw new ExceptionSemantic("Function " + function.getSignature() + " expected " + function.getParameterCount() + " arguments but got " + (argumentCount + 1) + ".");
		
		// First slots are always arguments
		Class<?> parameterType = function.getParameterType(argumentCount);
		Class<?> argType = v.getClass();
		
		if (parameterType != argType) {
			throw new ExceptionSemantic("Cannot assign value of type: " + v.getClass() + " to parameter of type: " + parameterType + ". Are you missing a cast?");
		}
		
		setSlot(argumentCount++, v);
	}
	
	/** Check argument count. */
	void checkArgumentCount() {
		if (argumentCount < function.getParameterCount())
			throw new ExceptionSemantic("Function " + function.getSignature() + " expected " + function.getParameterCount() + " arguments but got " + argumentCount + ".");		
	}
	
	/** Execute this invocation. */
	Value execute(Parser parser) {
		parser.doChildren(function.getFunctionBody(), null);
		if (function.hasReturn())
			return parser.doChild(function.getFunctionReturnExpression(), 0);
		return null;
	}

	/** Get the slot number of a given variable or parameter name.  Return -1 if not found. */
	int findSlotNumber(String name) {
		return function.getLocalSlotNumber(name);
	}
	
	/** Get a variable or parameter value given a slot number. */
	Value getValue(int slotNumber) {
		return slots.get(slotNumber);
	}

	/** Given a slot number, set its value. */
	void setValue(int slotNumber, Value value) {
		setSlot(slotNumber, value);
	}

	/** Define a variable in the function definition.  Return its slot number. */
	int defineVariable(String name) {
		return function.defineVariable(name);
	}
	
	/** Add a function definition. */
	void addFunction(FunctionDefinition definition) {
		function.addFunction(definition);
	}
	
	/** Find a function definition.  Return null if it doesn't exist. */
	FunctionDefinition findFunction(String name) {
		return function.findFunction(name);
	}
	
}
