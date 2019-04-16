package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
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
		//TODO: Enforce return type
		
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 1);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 2, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 3));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 4));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}
		
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition)data;
		for (int i=0; i<node.jjtGetNumChildren(); i += 2) {
			currentDefinition.defineParameter(getTokenOfChild(node, i), getTokenOfChild(node, i + 1));
		}
		
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
			if (fndef == null) {
				Display.Reference ref = scope.findReference(fnname);
				if (ref != null) {
					Value val = ref.getValue();
					
					if (val.getClass() == ValueFn.class) {
						fndef = ((ValueFn)val).getFunctionDefinition();
					} else {
						throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function");
					}
				} else {
					throw new ExceptionSemantic("Function " + fnname + " is undefined.");
				}
			}
			
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
			if (fndef == null) {
				Display.Reference ref = scope.findReference(fnname);
				if (ref != null) {
					Value val = ref.getValue();
					
					if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) { //alt: if (val.getClass() == ValueAnonymousType.class)
						val = doChild(node, 0);
					}
					
					if (val.getClass() == ValueFn.class) {
						fndef = ((ValueFn)val).getFunctionDefinition();
					} else {
						throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function.");
					}
				} else {
					throw new ExceptionSemantic("Function " + fnname + " is undefined.");
				}
			}
				
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
		
		//If this is a member access case or index access case (i.e. obj.var/arr[0])
		if (node.jjtGetNumChildren() >= 1) value = processAccess(value, node);
		
		return value;
	}
	
	private Value processAccess(Value value, ASTDereference node) {
		if (value.getClass() == ValueAnonymousType.class) {
			ValueAnonymousType obj = (ValueAnonymousType) value;
			Value currentChild = obj.getVariableValue(getTokenOfChild(node, 0));
			
			for (int i = 1; i < node.jjtGetNumChildren(); ++i) {
				if (currentChild.getClass() == ValueAnonymousType.class) {
					currentChild = processAnonType(currentChild, node, i);
				} else {
					currentChild = processArray(currentChild, node, i);
				}
			}
			
			return currentChild;
		} else {
			
			ValueArray arr = (ValueArray) value;
			
			Value currentChild = arr.getValue((int)(doChild(node, 0).longValue()));
			
			for (int i = 1; i < node.jjtGetNumChildren(); ++i) {
				if (currentChild.getClass() == ValueAnonymousType.class) {
					currentChild = processAnonType(currentChild, node, i);
				} else {
					currentChild = processArray(currentChild, node, i);
				}
			}
			
			return currentChild;
		}
	}
	
	private Value processAnonType(Value child, ASTDereference node, int index) {
		return ((ValueAnonymousType) child).getVariableValue(getTokenOfChild(node, index));
	}
	
	private Value processArray(Value child, ASTDereference node, int index) {
		return ((ValueArray) child).getValue((int) doChild(node, index).longValue());
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
				throw new ExceptionSemantic("Variable " + name + " does not exist yet. Are you missing a declaration?");
				//reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value valToAssign = doChild(node, 1);
		
		if (reference == null) {
			throw new ExceptionSemantic("Variable: " + getTokenOfChild(node, 0) + " does not exist in the current context.");
		}
		
		Value existingType = reference.getValue();
		
		//System.out.println("Let's go. " + doChild((SimpleNode)node.jjtGetChild(0), 0).getClass());
		
		if (valToAssign.getClass().equals(existingType.getClass())) {
			reference.setValue(valToAssign);
		} else if (existingType.getClass() == ValueArray.class) {
			Value val = doChild((SimpleNode)node.jjtGetChild(0), 0);
			//System.out
			if (val.getClass() == ValueInteger.class) {
				((ValueArray) existingType).putValue((int)val.longValue(), valToAssign);
			}	
		} else {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + existingType.getClass() + ". Are you missing a cast?");
		}
		
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
			if (reference == null) {
				reference = scope.defineVariable(name);
			}
			else
				throw new ExceptionSemantic("Variable '" + name + "' has already been defined in this scope.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value valToAssign = doChild(node, childrenCount - 1);
		Class specifiedType = CScharfUtil.getClassFromString(getTokenOfChild(node, childrenCount - 3));
		
		if (!valToAssign.getClass().equals(specifiedType)) {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + specifiedType.getClass() + ". Are you missing a cast?");
		}
		
		if (childrenCount == 4 && getTokenOfChild(node, childrenCount - 4).equals("const"))
			valToAssign.setConst();
		
		//System.out.println("Setting value of type: " + valToAssign.getClass());
		
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
		
		if (scope.findClassInCurrentLevel(node.tokenValue) != null) {
			throw new ExceptionSemantic("Class: " + node.tokenValue + " already exists.");
		}
		
		System.out.println("Class name: " + node.tokenValue);
		
		/* 
		 * Notes:
		 * 
		 * Child 0: class body
		 */
		
		ClassDefinition currentClassDefinition = new ClassDefinition(node.tokenValue, scope.getLevel() + 1);
		scope.addClass(currentClassDefinition);
		
		doChild(node, 0);
		currentClassDefinition.setClassBody(getChild(node, 0));
		
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentClassDefinition;
		
		return data;
	}
	
	public Object visit(ASTClassBody node, Object data) {
		//Constructor
		doChild(node, 0);
		
		ClassDefinition classDef = scope.findClass(node.tokenValue); 
		
		for(int i = 1; i < node.jjtGetNumChildren(); ++i) {
			SimpleNode classBodyNode = getChild(node, i);
			
			if (classBodyNode instanceof ASTAssignment) {
				
				/*
				 * childrenCount - 1 = value to assign
				 * childrenCount - 2 = name of variable to assign to
				 * childrenCount - 3 = type of variable
				 * childrenCount - 4 = modifier
				 */
				
				//TODO: allow multiple modifiers e.g. public const
				
				int childrenCount = classBodyNode.jjtGetNumChildren();
				
				classDef.defineVariable(getTokenOfChild(classBodyNode, childrenCount - 3), getTokenOfChild(classBodyNode, childrenCount - 2), false, false);
			} else if (classBodyNode instanceof ASTFnDef) {
				
				//TODO: Create function definitions from the node and add to class
				
				System.out.println("adding function to class");
			} else if (classBodyNode instanceof ASTClassDef) {
				
				//TODO: Create class definitions from the node and add to class
				
				System.out.println("adding class to class");
			}
		}
		
		return data;
	}
	
	public Object visit(ASTClassConstructor node, Object data) {		
		String suppliedClassName = getTokenOfChild(node, 0);
		
		if(!suppliedClassName.equals(node.tokenValue))
			throw new ExceptionSemantic("Constructor for: " + suppliedClassName + " is not valid in class: " + node.tokenValue + ". Are you missing a return type?");
		
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(node.tokenValue + "Constructor", scope.getLevel() + 1);
		doChild(node, 1, currentFunctionDefinition);
				
		//System.out.println("Adding constructor to valid functions");
		//System.out.println("Function name: " + currentFunctionDefinition.getName() + " param count: " + currentFunctionDefinition.getParameterCount() + " param name: " + currentFunctionDefinition.getParameterName(0));
		
		scope.addFunction(currentFunctionDefinition);
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		
		ClassDefinition shouldExist = scope.findClass(suppliedClassName);
		
		shouldExist.addConstructor(currentFunctionDefinition);
		
		//currentFunctionDefinition.setFunctionReturnExpression(node);
		
		return data;
	}
	
	public Object visit(ASTClassInstance node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String className = getTokenOfChild(node, 0);
			String constructorName = className + "Constructor";
			
			ValueClass valClass = new ValueClass(scope.findClass(className));
			
			fndef = scope.findFunction(constructorName);
			if (fndef == null) {
					throw new ExceptionSemantic("Cannot find compatible constructor for class:  " + className + ".");
				}
			// Save it for next time
			node.optimised = fndef;
		} else {
			fndef = (FunctionDefinition)node.optimised;
		}
		
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		
		//TODO: Either make the constructor function return a ValueClass or don't return scope.execute and return valClass from earlier
		
		return scope.execute(newInvocation, this);
	}
			
	public Object visit(ASTFn node, Object data) {	
		ValueFn valueFunction = new ValueFn();
		
		String fnname = getTokenOfChild((SimpleNode) node.jjtGetParent(), 1);
		FunctionDefinition funcDef = new FunctionDefinition(fnname, scope.getLevel() + 1);
		
		//Parameters
		doChild(node, 0, funcDef);
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

	public Object visit(ASTArray node, Object data) {	
		ValueArray array = new ValueArray(getTokenOfChild(node, 0), Integer.parseInt(getTokenOfChild(node, 1)));
		
		node.optimised = array;
		
		return node.optimised;
	}
	
	// Process new
	public Object visit(ASTNewObj node, Object data) {
		return doChild(node, 0);
	}
}
