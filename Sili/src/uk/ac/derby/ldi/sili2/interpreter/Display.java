package uk.ac.derby.ldi.sili2.interpreter;

import uk.ac.derby.ldi.sili2.values.Value;

/** A display manages run-time access to variable and parameter scope where
 * functions may be nested.
 */ 
class Display {

	private final int maximumFunctionNesting = 64;
	private FunctionInvocation[] display = new FunctionInvocation[maximumFunctionNesting];
	private ClassInvocation[] classDisplay = new ClassInvocation[maximumFunctionNesting];
		
	private int currentLevel;

	/** Reference to a slot. */
	class Reference {
		private int displayDepth;
		private int slotNumber;
		
		/** Ctor */
		Reference(int depth, int slot) {
			displayDepth = depth;
			slotNumber = slot;
		}
		
		/** Set value pointed to by this reference. */
		void setValue(Value v) {
			display[displayDepth].setValue(slotNumber, v);
		}
		
		/** Get value pointed to by this reference. */
		Value getValue() {
			return display[displayDepth].getValue(slotNumber);
		}
	}
	
	/** Ctor */
	Display() {
		// root or 0th scope
		currentLevel = 0;
		display[currentLevel] = new FunctionInvocation(new FunctionDefinition("%main", currentLevel));
		classDisplay[currentLevel] = new ClassInvocation(new ClassDefinition("%program", currentLevel));
	}
	
	/** Execute a function in its scope, using a specified parser. */
	Value execute(FunctionInvocation fn, Parser p) {
		int changeLevel = fn.getLevel();
		FunctionInvocation oldContext = display[changeLevel];
		int oldLevel = currentLevel;
		display[changeLevel] = fn;
		currentLevel = changeLevel;
		Value v = display[currentLevel].execute(p);
		display[changeLevel] = oldContext;
		currentLevel = oldLevel;
		return v;
	}
	
	/** Get the current scope nesting level. */
	int getLevel() {
		return currentLevel;
	}
	
	/** Return a Reference to a variable or parameter.  Return null if it doesn't exist. */
	Reference findReference(String name) {
		int level = currentLevel;
		while (level >= 0) {
			int offset = display[level].findSlotNumber(name);
			if (offset >= 0)
				return new Reference(level, offset);
			level--;
		}
		return null;		
	
	}

	/** Create a variable in the current level and return its Reference. */
	Reference defineVariable(String name) {
		return new Reference(currentLevel, display[currentLevel].defineVariable(name));
	}

	/** Find a function.  Return null if it doesn't exist. */
	FunctionDefinition findFunction(String name) {
		int level = currentLevel;
		while (level >= 0) {
			FunctionDefinition definition = display[level].findFunction(name);
			if (definition != null)
				return definition;
			level--;
		}
		return null;
	}
	
	/** Find a class.  Return null if it doesn't exist. */
	ClassDefinition findClass(String name) {
		int level = currentLevel;
		while (level >= 0) {
			ClassDefinition definition = classDisplay[level].findClass(name);
			if (definition != null)
				return definition;
			level--;
		}
		return null;
	}
	
	/** Execute a class in its scope, using a specified parser. */
	Value execute(ClassInvocation classInv, Parser p) {
		System.out.println("ok");
		int changeLevel = classInv.getLevel();
		System.out.println("ok2");
		ClassInvocation oldContext = classDisplay[changeLevel];
		System.out.println("ok3");
		int oldLevel = currentLevel;
		classDisplay[changeLevel] = classInv;
		currentLevel = changeLevel;
		System.out.println("ok4");
		Value v = classDisplay[currentLevel].execute(p);
		System.out.println("ok5");
		classDisplay[changeLevel] = oldContext;
		System.out.println("ok6");
		currentLevel = oldLevel;
		System.out.println("ok7");
		
		return v;
	}

	/** Find a function in the current level.  Return null if it doesn't exist. */
	FunctionDefinition findFunctionInCurrentLevel(String name) {
		return display[currentLevel].findFunction(name);
	}
	
	/** Add a function to the current level. */
	void addFunction(FunctionDefinition definition) {
		display[currentLevel].addFunction(definition);
	}
	
	/** Find a class in the current level.  Return null if it doesn't exist. */
	ClassDefinition findClassInCurrentLevel(String name) {
		return classDisplay[currentLevel].findClass(name);
	}
	
	/** Add a class to the current level */
	void addClass(ClassDefinition definition) {
		classDisplay[currentLevel].addClass(definition);
	}
	
}
