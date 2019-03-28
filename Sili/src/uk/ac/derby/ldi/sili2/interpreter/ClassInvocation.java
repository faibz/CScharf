package uk.ac.derby.ldi.sili2.interpreter;

import java.util.Vector;

import uk.ac.derby.ldi.sili2.values.Value;
import uk.ac.derby.ldi.sili2.values.ValueClass;

/** Function invocation context. */
class ClassInvocation implements Invocation {

	private ClassDefinition classDefinition;
	private int argumentCount = 0;
	private Vector<Value> slots;
	
	private final void setSlot(int n, Value v) {
		if (n >= slots.size())
			slots.setSize(n + 1);
		slots.set(n, v);
	}
	
	/** Ctor for user-defined class. */
	ClassInvocation(ClassDefinition classDef) {
		classDefinition = classDef;
		slots = new Vector<Value>(classDefinition.getLocalCount());
	}
	
	/** Get the level of the associated class. */
	int getLevel() {
		return classDefinition.getLevel();
	}
	
	/** Set an argument value. */
	void setArgument(Value v) {
		if (argumentCount >= classDefinition.getParameterCount())
			throw new ExceptionSemantic("Class " + classDefinition.getSignature() + " expected " + classDefinition.getParameterCount() + " arguments but got " + (argumentCount + 1) + ".");
		// First slots are always arguments
		setSlot(argumentCount++, v);
	}
	
	/** Check argument count. */
	void checkArgumentCount() {
		if (argumentCount < classDefinition.getParameterCount())
			throw new ExceptionSemantic("Class " + classDefinition.getSignature() + " expected " + classDefinition.getParameterCount() + " arguments but got " + (argumentCount + 1) + ".");		
	}
	
	/** Execute this invocation. */
	Value execute(Parser parser) {
		System.out.println("Hi");
		parser.doChildren(classDefinition.getClassBody(), null);

		return new ValueClass("TempClass");
	}

	/** Get the slot number of a given variable or parameter name.  Return -1 if not found. */
	int findSlotNumber(String name) {
		return classDefinition.getLocalSlotNumber(name);
	}
	
	/** Get a variable or parameter value given a slot number. */
	Value getValue(int slotNumber) {
		return slots.get(slotNumber);
	}

	/** Given a slot number, set its value. */
	void setValue(int slotNumber, Value value) {
		setSlot(slotNumber, value);
	}

	/** Define a variable in the class definition.  Return its slot number. */
	int defineVariable(String name) {
		return classDefinition.defineVariable(name);
	}
	
	/** Add a class definition. */
	void addClass(ClassDefinition definition) {
		classDefinition.addClass(definition);
	}
	
	/** Find a class definition.  Return null if it doesn't exist. */
	ClassDefinition findClass(String name) {
		return classDefinition.findClass(name);
	}
	
}
