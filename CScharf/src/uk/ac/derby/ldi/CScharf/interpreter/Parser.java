package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.parser.ast.*;
import uk.ac.derby.ldi.CScharf.values.*;

public class Parser implements CScharfVisitor {	
	// Scope display handler
	private Display scope = new Display();
	
	// Get the ith child of a given node.
	private static SimpleNode getChild(SimpleNode node, int childIndex) {
		return (SimpleNode)node.jjtGetChild(childIndex);
	}
	
	// Get the token value of the ith child of a given node.
	private static String getTokenOfChild(SimpleNode node, int childIndex) {
		return getChild(node, childIndex).tokenValue;
	}
	
	// Execute a given child of the given node
	private Object doChild(SimpleNode node, int childIndex, Object data) {
		return node.jjtGetChild(childIndex).jjtAccept(this, data);
	}
	
	// Execute a given child of a given node, and return its value as a Value.
	// This is used by the expression evaluation nodes.
	Value doChild(SimpleNode node, int childIndex) {
		return (Value)doChild(node, childIndex, null);
	}
	
	// Execute all children of the given node
	Object doChildren(SimpleNode node, Object data) {
		return node.childrenAccept(this, data);
	}
	
	// Called if one of the following methods is missing...
	public Object visit(SimpleNode node, Object data) {
		System.out.println(node + ": acceptor not implemented in subclass?");
		return data;
	}
	
	// Execute a CScharf program
	public Object visit(ASTCode node, Object data) {
		return doChildren(node, data);	
	}
	
	// Execute a statement
	public Object visit(ASTStatement node, Object data) {
		return doChildren(node, data);	
	}

	// Execute a block
	public Object visit(ASTBlock node, Object data) {
		return doChildren(node, data);	
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 0);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 1, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 3));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}
		
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			currentDefinition.defineParameter(getTokenOfChild(node, i));
		return data;
	}
	
	// Function body
	public Object visit(ASTFnBody node, Object data) {
		return doChildren(node, data);
	}
	
	// Function return expression
	public Object visit(ASTReturnExpression node, Object data) {
		return doChildren(node, data);
	}
	
	// Function call
	public Object visit(ASTCall node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		scope.execute(newInvocation, this);
		return data;
	}
	
	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			if (!fndef.hasReturn())
				throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else {
			fndef = (FunctionDefinition)node.optimised;
		}
		
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		return scope.execute(newInvocation, this);
	}
		
	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		FunctionInvocation newInvocation = (FunctionInvocation)data;
		
		//ClassInvocation newInvocation = (ClassInvocation)data;

		
		
		
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}
	
	// Execute an IF 
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		Value hopefullyValueBoolean = doChild(node, 0);
		if (!(hopefullyValueBoolean instanceof ValueBoolean))
			throw new ExceptionSemantic("The test expression of an if statement must be boolean.");
		if (((ValueBoolean)hopefullyValueBoolean).booleanValue())
			doChild(node, 1);							// if(true), therefore do 'if' statement
		else if (node.ifHasElse)						// does it have an else statement?
			doChild(node, 2);							// if(false), therefore do 'else' statement
		return data;
	}
	
	// Execute a FOR loop
	public Object visit(ASTForLoop node, Object data) {
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			Value hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		return data;
	}
	
	// Execute a WHILE loop
	public Object visit(ASTWhileLoop node, Object data) {
		while (true) {
			Value hopefullyValueBoolean = doChild(node, 0);
			
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a while loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			
			doChild(node, 1);
		}
		
		return data;
	}
	
	// Process an identifier
	// This doesn't do anything, but needs to be here because we need an ASTIdentifier node.
	public Object visit(ASTIdentifier node, Object data) {
		return data;
	}
	
	// Process a type
	// This doesn't do anything, but needs to be here because we need an ASTType node.
	public Object visit(ASTType node, Object data) {
		return data;
	}
	
	// Process a modifier (e.g. const, readonly)
	// This doesn't do anything, but needs to be here because we need an ASTModifier node.
	public Object visit(ASTModifier node, Object data) {
		return data;
	}

	
	// Execute the WRITE statement
	public Object visit(ASTWrite node, Object data) {
		System.out.println(doChild(node, 0));
		return data;
	}
	
	// Dereference a variable or parameter, and return its value.
	public Object visit(ASTDereference node, Object data) {		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = node.tokenValue;
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value value = reference.getValue();
		
		//If this is a member access case (i.e. obj.var)
		if (node.jjtGetNumChildren() == 1) {
			ValueAnonymousType obj = (ValueAnonymousType) value;
			return obj.getVariableValue(getTokenOfChild(node, 0));
		}
		
		return value;
	}
	
	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		// Given that we could have anything from "const int val = 10;" to "val = 1;"
		// We need to be able to distinguish between types of assignment and throw accurate exceptions
		if (node.jjtGetNumChildren() == 2) {
			return untypedAssignment(node, data);
		} else {
			return typedAssignment(node, data);
		}
	}
	
	private Object untypedAssignment(ASTAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value valToAssign = doChild(node, 1);
		
		if (reference == null) {
			throw new ExceptionSemantic("Variable: " + getTokenOfChild(node, 0) + " does not exist in the current context.");
		}
		
		Value existingType = reference.getValue();
		
		if (!valToAssign.getClass().equals(existingType.getClass())) {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + existingType.getClass() + ". Are you missing a cast?");
		}
				
		reference.setValue(valToAssign);
		return data;
	}

	private Object typedAssignment(ASTAssignment node, Object data) {
		Display.Reference reference;
		
		int childrenCount = node.jjtGetNumChildren();
		
		/*
		 * childrenCount - 1 = value to assign
		 * childrenCount - 2 = name of variable to assign to
		 * childrenCount - 3 = type of variable
		 * childrenCount - 4 = modifier
		 */
		
		if (node.optimised == null) {
			String name = getTokenOfChild(node, childrenCount - 2);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			else
				throw new ExceptionSemantic("Variable '" + name + "' has already been defined in this scope.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value valToAssign = doChild(node, childrenCount - 1);
		Value specifiedType;
		
		switch(getTokenOfChild(node, childrenCount - 3)) {
			case "int":
			case "short":
			case "long":
				specifiedType = new ValueInteger(-1);
				break;
			case "float":
			case "dec":
				specifiedType = new ValueRational(-1.0);
				break;
			case "bool":
				specifiedType = new ValueBoolean(false);
				break;
			case "string":
				specifiedType = new ValueString("");
				break;
			case "anon":
				specifiedType = new ValueAnonymousType();
				break;
			case "function":
				specifiedType = new ValueFn();
				break;
			default:
				throw new ExceptionSemantic("Invalid type specified.");
		}
		
		if (!valToAssign.getClass().equals(specifiedType.getClass())) {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + specifiedType.getClass() + ". Are you missing a cast?");
		}
		
		if (childrenCount == 4 && getTokenOfChild(node, childrenCount - 4).equals("const"))
			valToAssign.setConst();
		
		reference.setValue(valToAssign);
		return data;
	}

	public Object visit(ASTTypelessAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		reference.setValue(doChild(node, 1));
		return data;
	}
	
	// OR
	public Object visit(ASTOr node, Object data) {
		return doChild(node, 0).or(doChild(node, 1));
	}

	// AND
	public Object visit(ASTAnd node, Object data) {
		return doChild(node, 0).and(doChild(node, 1));
	}

	// ==
	public Object visit(ASTCompEqual node, Object data) {
		return doChild(node, 0).eq(doChild(node, 1));
	}

	// !=
	public Object visit(ASTCompNequal node, Object data) {
		return doChild(node, 0).neq(doChild(node, 1));
	}

	// >=
	public Object visit(ASTCompGTE node, Object data) {
		return doChild(node, 0).gte(doChild(node, 1));
	}

	// <=
	public Object visit(ASTCompLTE node, Object data) {
		return doChild(node, 0).lte(doChild(node, 1));
	}

	// >
	public Object visit(ASTCompGT node, Object data) {
		return doChild(node, 0).gt(doChild(node, 1));
	}

	// <
	public Object visit(ASTCompLT node, Object data) {
		return doChild(node, 0).lt(doChild(node, 1));
	}

	// +
	public Object visit(ASTAdd node, Object data) {
		return doChild(node, 0).add(doChild(node, 1));
	}

	// -
	public Object visit(ASTSubtract node, Object data) {
		return doChild(node, 0).subtract(doChild(node, 1));
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		return doChild(node, 0).mult(doChild(node, 1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		return doChild(node, 0).div(doChild(node, 1));
	}

	// NOT
	public Object visit(ASTUnaryNot node, Object data) {
		return doChild(node, 0).not();
	}

	// + (unary)
	public Object visit(ASTUnaryPlus node, Object data) {
		return doChild(node, 0).unary_plus();
	}

	// - (unary)
	public Object visit(ASTUnaryMinus node, Object data) {
		return doChild(node, 0).unary_minus();
	}

	// Return string literal
	public Object visit(ASTCharacter node, Object data) {
		if (node.optimised == null)
			node.optimised = ValueString.stripDelimited(node.tokenValue);
		return node.optimised;
	}

	// Return integer literal
	public Object visit(ASTInteger node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueInteger(Long.parseLong(node.tokenValue));
		return node.optimised;
	}

	// Return floating point literal
	public Object visit(ASTRational node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueRational(Double.parseDouble(node.tokenValue));
		return node.optimised;
	}

	// Return true literal
	public Object visit(ASTTrue node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(true);
		return node.optimised;
	}

	// Return false literal
	public Object visit(ASTFalse node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(false);
		return node.optimised;
	}

	// Quit application
	public Object visit(ASTQuit node, Object data) {
		System.exit(0);
		
		return null;
	}

	public Object visit(ASTClassDef node, Object data) {
		if (node.optimised != null)
			return data;
		
		System.out.println("ASTClassDef");
		
		String className = getTokenOfChild(node, 0); 
		
		ValueClass value = new ValueClass(className);
		
		System.out.println("Made ValueClass " + className);
		
		if (scope.findClassInCurrentLevel(className) != null)
			throw new ExceptionSemantic("Class " + className + " already exists.");
		
		ClassDefinition currentClassDefinition = new ClassDefinition(className, scope.getLevel() + 1);
		
		System.out.println("Created class definition");
		
		//TODO: this		
		// Child 1 - function definition parameter list
		//doChild(node, 1, currentFunctionDefinition);
		
		// Add to available classes
		scope.addClass(currentClassDefinition);
		
		System.out.println("Added class to scope");
		
		// Child 2 - class body
		currentClassDefinition.setClassBody(getChild(node, 1));
		
		System.out.println("Processed class body.");
		
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentClassDefinition;
		
		//doChildren(node, data);
		
		//String name = getTokenOfChild(node, 2);
		//System.out.println(name);
		
//		FunctionDefinition temp = new FunctionDefinition("temp", 0);
//		doChild(node, 1, temp);
//		
//		for (int i = 0; i < temp.getParameterCount(); ++i)
//		{
//			value.addVariable(temp.getParameterName(i), new ValueInteger(1));
//		}
//		
		/*
		 * TODO:
		 * ADD TO AVAILABLE CLASSES SEE: scope.addFuntion(...) make addClass(...)?
		 * 
		 * Notes:
		 * 
		 * Child 0 - class name
		 * Child 1...? - functions, variables, and ctor??
		 */
		
		System.out.println("Finished ASTClassDef");
		
		node.optimised = value;
		
		return data;
	}
	
	public Object visit(ASTClassBody node, Object data) {
		return doChildren(node, data);
	}
	
	public Object visit(ASTClassInvoke node, Object data) {
		/*
		 * Check scope and node.optimised to see if class is available. Throw exception if not.
		 * 
		 * Notes:
		 * 
		 * Child 0 - class name
		 * Child 1 - arg list
		 */
		
		String className = getTokenOfChild(node, 0);
		
		ValueClass val = new ValueClass(className);
		
		System.out.println("Creating object from class: " + className);
		
		Display.Reference reference;
		reference = scope.defineVariable(val.getName());
		
		//node.optimised = reference;
		
		reference.setValue(val);
				
		ClassDefinition classDef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			classDef = scope.findClass(className);
			if (classDef == null)
				throw new ExceptionSemantic("Class " + className + " is undefined.");
			// Save it for next time
			node.optimised = classDef;
		} else {
			classDef = (ClassDefinition)node.optimised;
		}
		
		
		
		ClassInvocation classInv = new ClassInvocation(classDef);
		
//		// Child 1 - arglist
//		FunctionInvocation fInv = new FunctionInvocation(new FunctionDefinition("temp", 0));
//		doChild(node, 1, fInv);
		
		//FunctionInvocation plex = new FunctionInvocation(new FunctionDefinition("temp", 0));
		
		//doChild(node, 1, plex);
		doChild(node, 1, classInv);
		
		System.out.println("Finished class init");
		
		// Execute
		return scope.execute(classInv, this);
	}
		
	public Object visit(ASTFn node, Object data) {	
		/* 
		 * Notes:
		 * 
		 * Child 0 - param list
		 * Child 1 - fn body
		 * Child 2 - ret expression
		 */
		
//		ValueFn valFn = new ValueFn();
//		
//		if (node.optimised == null)
//			node.optimised = valFn;
//		else
//			return node.optimised;
//		
//		//Param list
//		//getTokenOfChild(node, 0);
//		//doChild(node, 0, valFn);
//		
//		FunctionDefinition temp = new FunctionDefinition("temp", 0);
//		doChild(node, 0, temp);
//		
//		for (int i = 0; i < temp.getParameterCount(); ++i)
//		{
//			valFn.defineParameter(temp.getParameterName(i));
//		}
//		
//		//func body
//		valFn.setFunctionBody(getChild(node, 1));
//		
//		if (node.fnHasReturn)
//			valFn.setFunctionReturnExpression(getChild(node, 2));
//				
//		return node.optimised;
		
		ValueFn valueFunction = new ValueFn();
		
		String fnname = getTokenOfChild((SimpleNode) node.jjtGetParent(), 1);
		FunctionDefinition funcDef = new FunctionDefinition(fnname, scope.getLevel() + 1);
		
		//Parameters
		doChild(node, 0, funcDef);
		scope.addFunction(funcDef);
		funcDef.setFunctionBody(getChild(node, 1));
		
		if (node.fnHasReturn)
			funcDef.setFunctionReturnExpression(getChild(node, 2));
		
		valueFunction.setFunctionDefinition(funcDef);
		node.optimised = valueFunction;
		
		return node.optimised;
	}

	public Object visit(ASTAnon node, Object data) {
		ValueAnonymousType obj = new ValueAnonymousType();
		
		if (node.optimised == null)
			node.optimised = obj;
		
		int children = node.jjtGetNumChildren();
		
		for(int i = 0; i < children; ++i) {
			SimpleNode assignmentNode = getChild(node, i);
			obj.addValue(getTokenOfChild(assignmentNode, 0), doChild(assignmentNode, 1));
		}
				
		return node.optimised;
	}

//	public Object visit(ASTObjectMemberAccessor node, Object data) {		
//		Value val = new ValueObject();
//		if (node.optimised != null)
//			return node.optimised;
//		
//		Value hopefullyValueObject = doChild(node, 0);
//		
//		if (!(hopefullyValueObject instanceof ValueObject)) {
//			throw new ExceptionSemantic("Cannot access member variable of non-object.");
//		}
//		
//		ValueObject obj = (ValueObject) hopefullyValueObject;
//		
//		val = obj.getVariableValue(getTokenOfChild(node, 1));
//		
//		node.optimised = val;
//		
//		//System.out.println(val.toString());
//		
//		return node.optimised;
//	}
}
