package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Stack;
import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.parser.ast.*;
import uk.ac.derby.ldi.CScharf.values.*;

public class Parser implements CScharfVisitor {	
	// Scope display handler
	private Display scope = new Display();
	private Stack<ValueClass> openValueClasses = new Stack<ValueClass>();
	
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
		//Pre block execution
		var preExistingVariables = scope.getAccessibleVariables();
		var preExistingFunctions = scope.getAccessibleFunctions();
		
		//Execution
		var executionResult =  doChildren(node, data);
		
		//Post block execution
		var variablesAvailableAfterExecution = scope.getAccessibleVariables();
		var functionsAvailableAfterExecution = scope.getAccessibleFunctions();
				
		variablesAvailableAfterExecution.removeAll(preExistingVariables);
		functionsAvailableAfterExecution.removeAll(preExistingFunctions);
		
		for (var newVariable : variablesAvailableAfterExecution) {
			scope.removeVariable(newVariable);
		}
		
		for (var newFunction : functionsAvailableAfterExecution) {
			scope.removeFunction(newFunction.getName());
		}
		
		return executionResult;
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		if (node.optimised != null)
			return data;
		
		var fnname = getTokenOfChild(node, 1);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		
		var currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		
		doChild(node, 2, currentFunctionDefinition);
		scope.addFunction(currentFunctionDefinition);
		currentFunctionDefinition.setFunctionBody(getChild(node, 3));
		if (node.fnHasReturn) {
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 4));
		}
		
		currentFunctionDefinition.setReturnType(CScharfUtil.getClassFromString(getTokenOfChild(node, 0)));
		
		node.optimised = currentFunctionDefinition;
		return data;
	}
		
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		var currentDefinition = (FunctionDefinition)data;
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
		int stackLength = openValueClasses.size();

		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			var fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			
			if (fndef == null) {
				Display.Reference ref = scope.findReference(fnname);
				if (ref != null) {
					Value val = ref.getValue();
					
					if (val instanceof ValueClass) {
						var valClass = findValueClass(node.jjtGetChild(0), (ValueClass) val);
						openValueClasses.push(valClass);
						
						var classDef = valClass.getClassDefinition();

						if (classDef != null) {
							fndef = classDef.findFunction(getTokenOfChild((SimpleNode) node.jjtGetChild(0), node.jjtGetChild(0).jjtGetNumChildren() - 1));
							fnname = fndef.getName();
						} else {
							if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) {
								val = doChild(node, 0);
							}
							
							if (val.getClass() == ValueFn.class) {
								fndef = ((ValueFn)val).getFunctionDefinition();
							} else {
								throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function.");
							}
						}
					} else {					
						if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) { //alt: if (val instanceof ValueContainer)
							val = doChild(node, 0);
						}
						
						if (val.getClass() == ValueFn.class) {
							fndef = ((ValueFn)val).getFunctionDefinition();
						} else {
							throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function.");
						}
					}
				} else {
					ValueClass openValClass = null;
					
					for (var i = openValueClasses.size() - 1; i >= 0; --i) {
						var classDef = openValueClasses.elementAt(i).getClassDefinition();
						
						if (classDef != null) {
							if (classDef.findFunction(fnname) != null) {
								fndef = classDef.findFunction(fnname);
							} else {
								var variable = openValueClasses.elementAt(i).getVariable(fnname);
								if (variable != null && variable instanceof ValueClass) {
									openValClass = findValueClass(node.jjtGetChild(0), (ValueClass) variable);
										
									var classDefinition = openValClass.getClassDefinition();
	
									if (classDefinition != null) {
										fndef = classDefinition.findFunction(getTokenOfChild((SimpleNode) node.jjtGetChild(0), node.jjtGetChild(0).jjtGetNumChildren() - 1));
										fnname = fndef.getName();
									} else {
										if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) {
											variable = doChild(node, 0);
										}
										
										if (variable.getClass() == ValueFn.class) {
											fndef = ((ValueFn)variable).getFunctionDefinition();
										} else {
											throw new ExceptionSemantic("Cannot invoke a value of type: " + variable.getClass() + " like a function.");
										}
									}
								}
							}
						} 
					}
					
					openValueClasses.push(openValClass);
					
					if (fndef == null) {
						throw new ExceptionSemantic("Function " + fnname + " is undefined.");
					}
				}
			}
			
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
				
		var newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		var possibleValue = scope.execute(newInvocation, this);
		
		//var executionResult = scope.execute(newInvocation, this);
		
		if (openValueClasses.size() > stackLength)
			openValueClasses.pop();
				
		if (fndef.hasReturn()) {
			var returnType = fndef.getReturnType();
			 if (returnType != null) {
				 if (possibleValue.getClass() != returnType)
					 throw new ExceptionSemantic("Cannot return value of type " + possibleValue.getClass() + " from a function with a return type of " + returnType);
			 } else {
				 throw new ExceptionSemantic("Cannot return a value from a void method.");
			 }
		}
		
		return data;
	}
	
	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		int stackLength = openValueClasses.size();
		
		FunctionDefinition fndef;
		
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			var fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
						
			if (fndef == null) {
				Display.Reference ref = scope.findReference(fnname);
				if (ref != null) {
					Value val = ref.getValue();
					
					if (val instanceof ValueClass) {
						var valClass = findValueClass(node.jjtGetChild(0), (ValueClass) val);
						openValueClasses.push(valClass);
						
						var classDef = valClass.getClassDefinition();

						if (classDef != null) {
							fndef = classDef.findFunction(getTokenOfChild((SimpleNode) node.jjtGetChild(0), node.jjtGetChild(0).jjtGetNumChildren() - 1));
							fnname = fndef.getName();
						} else {
							if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) {
								val = doChild(node, 0);
							}
							
							if (val.getClass() == ValueFn.class) {
								fndef = ((ValueFn)val).getFunctionDefinition();
							} else {
								throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function.");
							}
						}
					} else {					
						if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) {
							val = doChild(node, 0);
						}
						
						if (val.getClass() == ValueFn.class) {
							fndef = ((ValueFn)val).getFunctionDefinition();
						} else {
							throw new ExceptionSemantic("Cannot invoke a value of type: " + val.getClass() + " like a function.");
						}
					}
				} else {
					ValueClass openValClass = null;
					
					for (var i = openValueClasses.size() - 1; i >= 0; --i) {
						var classDef = openValueClasses.elementAt(i).getClassDefinition();
						
						if (classDef != null) {
							if (classDef.findFunction(fnname) != null) {
								fndef = classDef.findFunction(fnname);
							} else {
								var variable = openValueClasses.elementAt(i).getVariable(fnname);
								if (variable != null && variable instanceof ValueClass) {
									openValClass = findValueClass(node.jjtGetChild(0), (ValueClass) variable);
										
									var classDefinition = openValClass.getClassDefinition();
	
									if (classDefinition != null) {
										fndef = classDefinition.findFunction(getTokenOfChild((SimpleNode) node.jjtGetChild(0), node.jjtGetChild(0).jjtGetNumChildren() - 1));
										fnname = fndef.getName();
									} else {
										if (node.jjtGetChild(0).jjtGetNumChildren() >= 1) {
											variable = doChild(node, 0);
										}
										
										if (variable.getClass() == ValueFn.class) {
											fndef = ((ValueFn)variable).getFunctionDefinition();
										} else {
											throw new ExceptionSemantic("Cannot invoke a value of type: " + variable.getClass() + " like a function.");
										}
									}
								}
							}
						} 
					}
					
					openValueClasses.push(openValClass);
					
					if (fndef == null) {
						throw new ExceptionSemantic("Function " + fnname + " is undefined.");
					}
				}
			}
			
			if (!fndef.hasReturn())
				throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else {
			fndef = (FunctionDefinition)node.optimised;
		}
		
		var newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		
		var executionResult = scope.execute(newInvocation, this);
		
		if (openValueClasses.size() > stackLength)
			openValueClasses.pop();
				
		if (fndef.getReturnType() != executionResult.getClass())
			throw new ExceptionSemantic("Cannot return value of type " + executionResult.getClass() + " from a function with a return type of " + fndef.getReturnType());
		
		return executionResult;
	}
		
	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		var newInvocation = (FunctionInvocation)data;
		
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}
	
	// Execute an IF 
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		var hopefullyValueBoolean = doChild(node, 0);
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
				
		var assignmentNode = (SimpleNode) getChild(node, 0);
		if (assignmentNode.jjtGetChild(0) instanceof ASTModifier)
			throw new ExceptionSemantic("Cannot apply const/readonly to variable used for loop initialisation.");
		
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			var hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		
		if (assignmentNode.jjtGetNumChildren() == 3) {
			scope.removeVariable(((SimpleNode)assignmentNode.jjtGetChild(1)).tokenValue);
		}

		return data;
	}
	
	// Execute a WHILE loop
	public Object visit(ASTWhileLoop node, Object data) {
		while (true) {
			var hopefullyValueBoolean = doChild(node, 0);
			
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
	
	// Execute the write statement
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
			if (reference == null) {				
				for(var i = openValueClasses.size() - 1; i >= 0 ; --i) {
					var value = openValueClasses.elementAt(i).getVariable(name);
					if(value != null) {
						return value;
					}
				}

				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			}
				
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		var value = reference.getValue();
		
		//If this is a member access case or index access case (i.e. obj.var/arr[0])
		if (node.jjtGetNumChildren() >= 1) value = processAccess(value, node);
		
		return value;
	}
	
	private ValueClass findValueClass(Node node, ValueClass container) {
		var derefNode = (ASTDereference) node;
		
		int childCount = node.jjtGetNumChildren();
		
		Value currentChild = container;
		
		for (int i = 0; i < childCount - 1; ++i) {
			if (currentChild instanceof ValueContainer) {
				currentChild = processContainerGet(currentChild, derefNode, i);
			} else {
				currentChild = processArrayGet(currentChild, derefNode, i);
			}
		}
		
		if (currentChild instanceof ValueClass) {
			return ((ValueClass) currentChild);
		}
		
		return null;
	}
	
	private Value processAccess(Value value, ASTDereference node) {
		if (value instanceof ValueContainer) {
			var container = (ValueContainer) value;
			var currentChild = container.getVariable(getTokenOfChild(node, 0));
			
			for (var i = 1; i < node.jjtGetNumChildren(); ++i) {
				if (currentChild instanceof ValueContainer) {
					currentChild = processContainerGet(currentChild, node, i);
				} else {
					currentChild = processArrayGet(currentChild, node, i);
				}
			}
			
			return currentChild;
		} else {
			var arr = (ValueArray) value;
			
			var currentChild = arr.getValue((int)(doChild(node, 0).longValue()));
			
			for (var i = 1; i < node.jjtGetNumChildren(); ++i) {
				if (currentChild instanceof ValueContainer) {
					currentChild = processContainerGet(currentChild, node, i);
				} else {
					currentChild = processArrayGet(currentChild, node, i);
				}
			}
			
			return currentChild;
		}
	}
		
	private Value processContainerGet(Value child, ASTDereference node, int index) {
		return ((ValueContainer) child).getVariable(getTokenOfChild(node, index));
	}
	
	private Value processArrayGet(Value child, ASTDereference node, int index) {
		return ((ValueArray) child).getValue((int) doChild(node, index).longValue());
	}
	
	private void processPut(Value container, Value valToAssign, ASTDereference node) {
		int childCount = node.jjtGetNumChildren();
		
		Value currentChild = container;
		
		for (var i = 0; i < childCount - 1; ++i) {
			if (currentChild instanceof ValueContainer) {
				currentChild = processContainerGet(currentChild, node, i);
			} else {
				currentChild = processArrayGet(currentChild, node, i);
			}
		}
		
		if (currentChild instanceof ValueClass) {
			processClassPut(currentChild, valToAssign, node, childCount - 1);
		} else if (currentChild instanceof ValueArray) {
			processArrayPut(currentChild, valToAssign, node, childCount - 1);
		}
	}
	
	private void processClassPut(Value valClass, Value valueToAssign, ASTDereference node, int index) {
		((ValueClass) valClass).setVariable(getTokenOfChild(node, index), valueToAssign);
	}
	
	private void processArrayPut(Value valArray, Value valueToAssign, ASTDereference node, int index) {
		((ValueArray) valArray).putValue((int) doChild(node, index).longValue(), valueToAssign);
	}
	
	// Execute a declaration statement.
	public Object visit(ASTVariableDeclaration node, Object data) {
		int childrenCount = node.jjtGetNumChildren();
		
		var name = getTokenOfChild(node, 0);
		Display.Reference reference = scope.findReference(name);
		if (reference != null)
			throw new ExceptionSemantic("Variable " + name + " already exists.");
		else
			reference = scope.defineVariable(name);
		
		if (childrenCount == 3) {
			//TODO: Modifiers
		}
		
		var specifiedType = CScharfUtil.getClassFromString(getTokenOfChild(node, childrenCount - 2));
		reference.setValue(CScharfUtil.getDefaultValueForClass(specifiedType));
		
		return data;
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
		ValueClass owningClass = null;
		Value existingValue = null;
		boolean classMember = false;
		
		if (node.optimised == null) {
			var name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null) {
				
				for(var valClass : openValueClasses) {
					var value = valClass.getVariable(name);
					if (value != null) {
						 existingValue = value;
						 owningClass = valClass;
						 classMember = true;
					}
				}
				
				if (existingValue == null)
					throw new ExceptionSemantic("Variable " + name + " does not exist yet. Are you missing a declaration?");
			}

			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		var valToAssign = doChild(node, 1);
		
		if (reference == null && existingValue == null) {
			throw new ExceptionSemantic("Variable " + getTokenOfChild(node, 0) + " does not exist in the current context.");
		}
		
		if (existingValue == null)
			existingValue = reference.getValue();
		
		if (valToAssign.getClass().equals(existingValue.getClass()) && node.jjtGetChild(0).jjtGetNumChildren() <= 0) {
			if (classMember) 
				owningClass.setVariable(getTokenOfChild(node, 0), valToAssign);
			else
				reference.setValue(valToAssign);
		} else if (existingValue.getClass() == ValueArray.class || existingValue instanceof ValueContainer) {
			processPut(existingValue, valToAssign, (ASTDereference) node.jjtGetChild(0));
		} else {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + existingValue.getClass() + ". Are you missing a cast?");
		}
		
		return data;
	}

	private Object typedAssignment(ASTAssignment node, Object data) {
		Display.Reference reference;
		
		var childrenCount = node.jjtGetNumChildren();
		
		/*
		 * childrenCount - 1 = value to assign
		 * childrenCount - 2 = name of variable to assign to
		 * childrenCount - 3 = type of variable
		 * childrenCount - 4 = modifier
		 */
				
		if (node.optimised == null) {
			var name = getTokenOfChild(node, childrenCount - 2);
			reference = scope.findReference(name);
			if (reference == null) {
				reference = scope.defineVariable(name);
			}
			else
				throw new ExceptionSemantic("Variable '" + name + "' has already been defined in this scope.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		var valToAssign = doChild(node, childrenCount - 1);
		var specifiedType = CScharfUtil.getClassFromString(getTokenOfChild(node, childrenCount - 3));
		
		if (!valToAssign.getClass().equals(specifiedType)) {
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
		
		if (scope.findClass(node.tokenValue) != null) {
			throw new ExceptionSemantic("Class: " + node.tokenValue + " already exists.");
		}
		
		var classDefinition = new ClassDefinition(node.tokenValue);
		scope.addClass(classDefinition);
		
		doChild(node, node.jjtGetNumChildren() - 1);
		classDefinition.setClassBody(getChild(node, 0));
		
		verifyInheritance(node, classDefinition);
		
		node.optimised = classDefinition;
		
		return data;
	}
	
	private void verifyInheritance(ASTClassDef node, ClassDefinition classDefinition) {
		Vector<InterfaceDefinition> interfacesToInheritFrom = new Vector<InterfaceDefinition>();
		
		for (var i = 0; i < node.jjtGetNumChildren() - 1; ++i) {
			var intDef = scope.findInterface(getTokenOfChild(node, i));
			
			if (intDef == null) throw new ExceptionSemantic("Interface " + getTokenOfChild(node, i) + " does not exist.");
			
			interfacesToInheritFrom.add(intDef);
		}
		
		var functionsInClass = classDefinition.getFunctionsCopy();
		
		for (var intDef : interfacesToInheritFrom) {
			for (var intFunc : intDef.getFunctions()) {
				boolean matchFound = false;
				
				for (var funcDef : functionsInClass.values()) {
					if(funcDef.getName().equals(intFunc.getName()) && funcDef.getReturnType() == funcDef.getReturnType() && funcDef.getParameterTypes().equals(intFunc.getParamTypes())) {
						matchFound = true;
					}
				}
				
				if (!matchFound) throw new ExceptionSemantic("Class " + classDefinition.getName() + " does not implement interface " + intDef.getName() + "'s " + intFunc.getName() + " function.");
			}
		}
	}
	
	public Object visit(ASTClassBody node, Object data) {
		doChild(node, 0);
		
		var classDef = scope.findClassDeep(node.tokenValue);
		
		for(var i = 1; i < node.jjtGetNumChildren(); ++i) {
			SimpleNode classBodyChildNode = getChild(node, i);
			
			if (classBodyChildNode instanceof ASTAssignment) {
				var childrenCount = classBodyChildNode.jjtGetNumChildren();
				var modifier = Modifier.NONE;
				
				if (classBodyChildNode.jjtGetChild(0) instanceof ASTModifier) {
					var modifierNode = (ASTModifier) classBodyChildNode.jjtGetChild(0);
					
					modifier = modifierNode.tokenValue.equals("const") ? Modifier.CONSTANT : Modifier.READONLY;
				}
				
				classDef.defineVariable(getTokenOfChild(classBodyChildNode, childrenCount - 3), getTokenOfChild(classBodyChildNode, childrenCount - 2), modifier, doChild(classBodyChildNode, childrenCount - 1));
			} else if (classBodyChildNode instanceof ASTVariableDeclaration) {
				var childrenCount = classBodyChildNode.jjtGetNumChildren();
				
				var readonly = false;
				
				if (classBodyChildNode.jjtGetChild(0) instanceof ASTModifier) {
					var modifierNode = (ASTModifier) classBodyChildNode.jjtGetChild(0);
					
					if (modifierNode.tokenValue.equals("const")) 
						throw new ExceptionSemantic("Cannot set variable declaration to constant without a definition.");
					
					readonly = true;
				}
				
				classDef.declareVariable(getTokenOfChild(classBodyChildNode, childrenCount - 2), getTokenOfChild(classBodyChildNode, childrenCount - 1), readonly);
			} else if (classBodyChildNode instanceof ASTFnDef) {
				var functionDefinition = new FunctionDefinition(getTokenOfChild(classBodyChildNode, 1), scope.getLevel() + 1);
				
				doChild(classBodyChildNode, 2, functionDefinition);
				functionDefinition.setFunctionBody(getChild(classBodyChildNode, 3));
				if (classBodyChildNode.fnHasReturn) {
					functionDefinition.setFunctionReturnExpression(getChild(classBodyChildNode, 4));
				}
				
				functionDefinition.setReturnType(CScharfUtil.getClassFromString(getTokenOfChild(classBodyChildNode, 0)));
				
				classDef.addFunction(functionDefinition);
			} else if (classBodyChildNode instanceof ASTClassDef) {
				var classDefinition = new ClassDefinition(classBodyChildNode.tokenValue);
				classDef.addClass(classDefinition);
				doChild(classBodyChildNode, classBodyChildNode.jjtGetNumChildren() - 1);
				classDefinition.setClassBody(getChild(classBodyChildNode, 0));
				classDef.addClass(classDefinition);
			}
		}

		return data;
	}
	
	public Object visit(ASTClassConstructor node, Object data) {
		var suppliedClassName = getTokenOfChild(node, 0);
		
		if(!suppliedClassName.equals(node.tokenValue))
			throw new ExceptionSemantic("Constructor for: " + suppliedClassName + " is not valid in class: " + node.tokenValue + ". Are you missing a return type?");
		
		var constructorName = node.tokenValue + " Constructor(";
		var parmListNode = node.jjtGetChild(1);
		
		for (var i = 0; i < parmListNode.jjtGetNumChildren(); i += 2) {
			constructorName = constructorName + getTokenOfChild((SimpleNode) parmListNode, i) + ",";			
		}
		
		constructorName = constructorName.substring(0, constructorName.length() - 1) + ")";
		
		var currentFunctionDefinition = new FunctionDefinition(constructorName, scope.getLevel() + 1);
		doChild(node, 1, currentFunctionDefinition);
		
		scope.addFunction(currentFunctionDefinition);
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		
		var classDef = scope.findClassDeep(suppliedClassName);	
		classDef.addConstructor(currentFunctionDefinition);
				
		return data;
	}
	
	public Object visit(ASTClassInstance node, Object data) {
		FunctionDefinition fndef;
		ValueClass valClass = null;
		
		var classDef = scope.findClass(getTokenOfChild(node, 0));
		
		if (classDef == null) {
			throw new ExceptionSemantic("Class " + getTokenOfChild(node, 0) + " could not be found.");
		}
		
		if (node.jjtGetNumChildren() > 2) {
			for (var i = 1; i < node.jjtGetNumChildren() - 1; ++i) {
				classDef = classDef.findClass(getTokenOfChild(node, i));
			}
		}

		var constructorName = classDef.getName() + " Constructor(";
		
		var argListNode = node.jjtGetChild(1);
		
		for (var i = 0; i < argListNode.jjtGetNumChildren(); ++i) {
			var val = doChild((SimpleNode)argListNode, i);
			constructorName = constructorName + CScharfUtil.getStringFromClass(val.getClass()) + ",";
		}
		
		constructorName = constructorName.substring(0, constructorName.length() - 1) + ")";
		
		valClass = new ValueClass(classDef);
		
		//This allows the constructor to modify the value class variables
		openValueClasses.push(valClass);
		
		fndef = scope.findFunction(constructorName);
		
		if (fndef == null) {
			throw new ExceptionSemantic("Cannot find compatible constructor for class:  " + classDef.getName() + ".");
		}

		var newInvocation = new FunctionInvocation(fndef);
		doChild(node, 1, newInvocation);		
		
		valClass.setInConstructor(true);
		scope.execute(newInvocation, this);
		valClass.setInConstructor(false);
		
		openValueClasses.pop();
		
		return valClass;
	}
	
	// Adds interface to scope
	public Object visit(ASTInterfaceDef node, Object data) {
		var interfaceDefinition = new InterfaceDefinition();
				
		interfaceDefinition.setName(node.tokenValue);
			
		var functionCount = node.jjtGetNumChildren() / 3;
		
		for (var i = 0; i < functionCount; ++i) {
			var interfaceFunc = new InterfaceFunction();
			
			interfaceFunc.setReturnType(CScharfUtil.getClassFromString(getTokenOfChild(node, i)));
			interfaceFunc.setName(getTokenOfChild(node, i + 1));
			
			var parmListNode = (SimpleNode) node.jjtGetChild(i + 2);
			
			for (var j = 0; j < parmListNode.jjtGetNumChildren(); j += 2) {
				interfaceFunc.addParam(CScharfUtil.getClassFromString(getTokenOfChild(parmListNode, j)));
			}
			
			interfaceDefinition.addFunction(interfaceFunc);
		}
				
		scope.addInterface(interfaceDefinition);
		
		return data;
	}
			
	public Object visit(ASTFn node, Object data) {
		var valueFunction = new ValueFn();
		
		var fnname = getTokenOfChild((SimpleNode) node.jjtGetParent(), 1);
		var funcDef = new FunctionDefinition(fnname, scope.getLevel() + 1);
		
		doChild(node, 1, funcDef);
		funcDef.setFunctionBody(getChild(node, 2));
		
		if (node.fnHasReturn) {
			funcDef.setFunctionReturnExpression(getChild(node, 3));
		}
		
		funcDef.setReturnType(CScharfUtil.getClassFromString(getTokenOfChild(node, 0)));
		
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
			var assignmentNode = getChild(node, i);
			obj.addValue(getTokenOfChild(assignmentNode, 0), doChild(assignmentNode, 1));
		}
				
		return node.optimised;
	}

	public Object visit(ASTArray node, Object data) {
		var array = new ValueArray(getTokenOfChild(node, 0), Integer.parseInt(getTokenOfChild(node, 1)));
		
		node.optimised = array;
		
		return node.optimised;
	}
	
	// Process new (e.g. new { ... }, new int[2], etc.)
	public Object visit(ASTNewObj node, Object data) {
		return doChild(node, 0);
	}
}