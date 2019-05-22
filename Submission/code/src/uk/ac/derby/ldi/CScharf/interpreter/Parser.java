package uk.ac.derby.ldi.CScharf.interpreter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.parser.ast.*;
import uk.ac.derby.ldi.CScharf.values.*;
import uk.ac.derby.ldi.CScharf.values.Modifier;

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
		var executionResult = doChildren(node, data);
		
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
		
		var fnname = getTokenOfChild(node, 0);
		fndef = scope.findFunction(fnname);
		
		if (fndef == null) {
			var ref = scope.findReference(fnname);
			
			fndef = findFunctionDefinition(node, ref, fnname);
			
			if (fndef == null) {
				var val = ref.getValue();
				
				if (val instanceof ValueReflection) {
					return processReflectionCall(node, val);
				}
				
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			}
			
			fnname = fndef.getName();
		}
				
		var newInvocation = new FunctionInvocation(fndef);
		doChild(node, 1, newInvocation);
		var possibleValue = scope.execute(newInvocation, this);
		
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

		var fnname = getTokenOfChild(node, 0);
		fndef = scope.findFunction(fnname);
					
		if (fndef == null) {
			var ref = scope.findReference(fnname);
			
			fndef = findFunctionDefinition(node, ref, fnname);
			
			if (fndef == null) {
				var val = ref.getValue();
				
				if (val instanceof ValueReflection) {
					return processReflectionCall(node, val);
				}
				
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			}
			
			fnname = fndef.getName();
		}
		
		if (!fndef.hasReturn())
			throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
	
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
	
	private Value processReflectionCall(SimpleNode node, Value val) {
		var derefNode = getChild(node, 0);
		var argListNode = getChild(node, 1);
		
		var values = new ArrayList<Value>();
		
		for (var i = 0; i < argListNode.jjtGetNumChildren(); ++i) {
			values.add(doChild(argListNode, i));
		}
		
		return ((ValueReflection) val).invokeMethod(getTokenOfChild(derefNode, 0), values);
	}
	
	private FunctionDefinition findFunctionDefinition(SimpleNode node, Display.Reference ref, String funcName) {
		FunctionDefinition fndef = null;
		String fnname = funcName;
		
		if (ref != null) {
			Value val = ref.getValue();
			
			if (val instanceof ValueReflection) {
				return null;
			}
			
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
		}
		
		return fndef;
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
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue()) {
				break;
			}
				
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
	public Object visit(ASTPrint node, Object data) {
		System.out.println(doChild(node, 0));
		return data;
	}
	
	// Dereference a variable or parameter, and return its value.
	public Object visit(ASTDereference node, Object data) {	
		Display.Reference reference;
		
		String name = node.tokenValue;
		reference = scope.findReference(name);
		if (reference == null) {
			for(var i = openValueClasses.size() - 1; i >= 0 ; --i) {
				var valClass = openValueClasses.elementAt(i);
				
				if (node.jjtGetNumChildren() == 0) {
					var value = valClass.getVariable(name);
					if (value != null) {
						return value;
					}
				} else {
					var container = valClass.getVariable(name);
					var value = processGet(container, node);
					
					if (value != null) {
						return value;
					}
				}
			}

			throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
		}
	
		var value = reference.getValue();
		
		//If this is a member access case or index access case (i.e. obj.var/arr[0])
		if (node.jjtGetNumChildren() >= 1) value = processGet(value, node);
		
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
	
	private Value processGet(Value value, ASTDereference node) {
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
		
		if (currentChild instanceof ValueContainer) {
			processContainerPut((ValueContainer) currentChild, valToAssign, node, childCount - 1);
		} else if (currentChild instanceof ValueArray) {
			processArrayPut(currentChild, valToAssign, node, childCount - 1);
		}
	}
	
	private void processContainerPut(ValueContainer valueContainer, Value valueToAssign, ASTDereference node, int index) {
		valueContainer.setVariable(getTokenOfChild(node, index), valueToAssign);
	}
	
	private void processArrayPut(Value valArray, Value valueToAssign, ASTDereference node, int index) {
		((ValueArray) valArray).putValue((int) doChild(node, index).longValue(), valueToAssign);
	}
	
	// Execute a declaration statement.
	public Object visit(ASTVariableDeclaration node, Object data) {
		int childrenCount = node.jjtGetNumChildren();
		
		if (childrenCount == 3) {
			if (getTokenOfChild(node, 0).equals("const")) {
				throw new ExceptionSemantic("Cannot declare a const variable without a definition.");
			} else {
				throw new ExceptionSemantic("Cannot declare a readonly variable outside of a class.");
			}
		}
		
		var name = getTokenOfChild(node, node.jjtGetNumChildren() - 1);
		Display.Reference reference = scope.findReference(name);
		if (reference != null)
			throw new ExceptionSemantic("Variable " + name + " already exists.");
		else
			reference = scope.defineVariable(name);
		
		var specifiedType = CScharfUtil.getClassFromString(getTokenOfChild(node, childrenCount - 2));
		
		if (specifiedType == null) {
			throw new ExceptionSemantic("Cannot use void as a variable type.");
		}
		
		reference.setValue(CScharfUtil.getDefaultValueForClass(specifiedType));
		
		return data;
	}
	
	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		// Given that we could have anything from "const int val = 10;" to "val = 1;"
		// We need to be able to distinguish between types of assignment and throw accurate exceptions
		boolean isIncOrDec = false;
		boolean increment = false;
		boolean prefix = false;
		
		var firstChild = (SimpleNode) node.jjtGetChild(0);
		var secondChild = (SimpleNode) node.jjtGetChild(1);
		
		if (firstChild instanceof ASTIncrementDecrement) {
			isIncOrDec = true;
			if (firstChild.tokenValue.equals("++")) increment = true;
			prefix = true;
		} else if (secondChild instanceof ASTIncrementDecrement) {
			isIncOrDec = true;
			if (secondChild.tokenValue.equals("++")) increment = true;
		}
				
		if (node.jjtGetNumChildren() == 2) {
			if (isIncOrDec) {
				var name = getTokenOfChild(node, prefix ? 1 : 0);
				var derefNode = (ASTDereference) node.jjtGetChild(prefix ? 1 : 0);
				return untypedAssignment(name, null, derefNode, data, true, increment, prefix);
			} else {
				return untypedAssignment(getTokenOfChild(node, 0), doChild(node, 1), (ASTDereference) node.jjtGetChild(0), data, false, false, false);
			}
		} else {
			return typedAssignment(node, data);
		}
	}
	
	private Object untypedAssignment(String name, Value valToAssign, ASTDereference derefNode, Object data, boolean isIncOrDec, boolean increment, boolean prefix) {
		Display.Reference reference;
		ValueClass owningClass = null;
		Value existingValue = null;
		var classMember = false;	
		reference = scope.findReference(name);
		
		if (reference == null) {
			for(var i = openValueClasses.size() - 1; i >= 0 ; --i) {
				var valClass = openValueClasses.elementAt(i);
				var value = valClass.getVariable(name);
				if (value != null) {
					existingValue = value;
					owningClass = valClass;
					classMember = true;
				}
			}
			
			if (existingValue == null) {
				throw new ExceptionSemantic("Variable " + name + " does not exist yet. Are you missing a declaration?");
			}
		}
		
		if (valToAssign == null) {
			if (reference != null && !isIncOrDec) {
				valToAssign = CScharfUtil.getDefaultValueForClass(reference.getValue().getClass());
			} 
		}
	
		if (reference == null && existingValue == null) {
			throw new ExceptionSemantic("Variable " + name + " does not exist in the current context.");
		}
		
		if (existingValue == null)
			existingValue = reference.getValue();
		
		if (valToAssign == null) {
			if (isIncOrDec) {
				if (existingValue != null) {
					if (existingValue instanceof ValueArray || existingValue instanceof ValueContainer) {
						valToAssign = processGet(existingValue, (ASTDereference) derefNode);
						valToAssign = increment ? valToAssign.add(new ValueInteger(1)) : valToAssign.subtract(new ValueInteger(1));
					} else {
						if (increment) valToAssign = existingValue.add(new ValueInteger(1));
						else valToAssign = existingValue.subtract(new ValueInteger(1));
					}
				}
			}
		}
		
		if (valToAssign.getClass().equals(existingValue.getClass()) && derefNode.jjtGetNumChildren() <= 0) {
			if (classMember) 
				owningClass.setVariable(name, valToAssign);
			else
				reference.setValue(valToAssign);
		} else if (existingValue instanceof ValueArray || existingValue instanceof ValueContainer) {
			processPut(existingValue, valToAssign, (ASTDereference) derefNode);
		} else {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + existingValue.getClass() + ". Are you missing a cast?");
		}
		
		return data;
	}

	private Object typedAssignment(ASTAssignment node, Object data) {
		Display.Reference reference;
		
		var childCount = node.jjtGetNumChildren();

		/*
		 * childCount - 1 = value to assign
		 * childCount - 2 = name of variable to assign to
		 * childCount - 3 = type of variable
		 * childCount - 4 = modifier
		 */
				
		var name = getTokenOfChild(node, childCount - 2);
		reference = scope.findReference(name);
		if (reference == null) {
			reference = scope.defineVariable(name);
		}
		else {
			try {
				reference.getValue();
				throw new ExceptionSemantic("Variable '" + name + "' has already been defined in this scope.");
			} catch (Exception e) {
				reference = scope.defineVariable(name);
			}
		}
		
		var valToAssign = doChild(node, childCount - 1);
		var specifiedType = CScharfUtil.getClassFromString(getTokenOfChild(node, childCount - 3));
		
		if (specifiedType == null) {
			throw new ExceptionSemantic("Cannot use void as a variable type.");
		}
		
		if (valToAssign == null) {
			valToAssign = CScharfUtil.getDefaultValueForClass(specifiedType);
		}

		
		if (!valToAssign.getClass().equals(specifiedType)) {
			throw new ExceptionSemantic("Cannot assign value of type: " + valToAssign.getClass() + " to variable of type: " + specifiedType + ". Are you missing a cast?");
		}
		
		if (childCount == 4) {
			if (getTokenOfChild(node, childCount - 4).equals("const")) {
				valToAssign.setConst();
			} else {
				throw new ExceptionSemantic("Cannot declare a readonly variable outside of a class.");
			}
		}
		
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
	
	// ++ (prefix)
	public Object visit(ASTPrefixIncrement node, Object data) {
		var childOfInterest = (SimpleNode) node.jjtGetChild(0).jjtGetChild(0);
		
		if (!(childOfInterest instanceof ASTDereference)) {
			throw new ExceptionSemantic("Incrementation not appropriate in current context.");
		}
		
		var derefNode = (ASTDereference) childOfInterest;
		
		untypedAssignment(childOfInterest.tokenValue, null, derefNode, data, true, true, true);
		
		return doChild(node, 0);
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		return doChild(node, 0).mult(doChild(node, 1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		return doChild(node, 0).div(doChild(node, 1));
	}
	
	// %
	public Object visit(ASTModulo node, Object data) {
		return doChild(node, 0).mod(doChild(node, 1));
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
	
	// Return float literal
	public Object visit(ASTFloat node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueFloat(Float.parseFloat(node.tokenValue));
		return node.optimised;
	}

	// Return double literal
	public Object visit(ASTDouble node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueDouble(Double.parseDouble(node.tokenValue));
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
		var classDef = scope.findClassDeep(node.tokenValue);
		
		for(var i = 0; i < node.jjtGetNumChildren(); ++i) {
			SimpleNode classBodyChildNode = getChild(node, i);
			if (classBodyChildNode instanceof ASTClassConstructor) {
				doChild(node, i);
			} else if (classBodyChildNode instanceof ASTAssignment) {
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
						throw new ExceptionSemantic("Cannot declare a constant variable without a definition.");
					
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
		var argListNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
		
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
			throw new ExceptionSemantic("Could not find compatible constructor for class:  " + classDef.getName() + ".");
		}

		var newInvocation = new FunctionInvocation(fndef);
		doChild(node, node.jjtGetNumChildren() - 1, newInvocation);
		
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
			interfaceFunc.setReturnType(CScharfUtil.getClassFromString(getTokenOfChild(node, i * 3)));

			interfaceFunc.setName(getTokenOfChild(node, i * 3 + 1));
			
			var parmListNode = (SimpleNode) node.jjtGetChild(i * 3 + 2);
			
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
		var funcDef = new FunctionDefinition("", scope.getLevel() + 1);
		
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

	public Object visit(ASTReflection node, Object data) {
		var reflectionBaseNode = getChild(node, 0);
		var action = reflectionBaseNode.tokenValue;
		var path = doChild(reflectionBaseNode, 0).stringValue();
		
		if (action.equals("CONSTRUCT")) {
			return createReflectionInstance(path, node);
		} else {
			return invokeReflectionMethod(path, node);
		}
	}
	
	private ValueReflection createReflectionInstance(String className, ASTReflection node) {
		ValueReflection valReflection = null;
		Class<?> reflectedClass = null;
		
		try {
			reflectedClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ExceptionSemantic("Could not find " + className + ". Verify that full class path is present.");
		}
		
		if (node.jjtGetNumChildren() > 1) {
			var expectedParamTypes = new ArrayList<Class<?>>();
			var realArgs = new ArrayList<Object>();
			
			for (var i = 1; i < node.jjtGetNumChildren(); ++i) {
				var val = doChild(node, i);
				expectedParamTypes.add(CScharfUtil.getJavaClassFromValue(val));
				realArgs.add(CScharfUtil.getJavaValueFromValueType(val));
			}

			try {
				var constructor = reflectedClass.getConstructor(expectedParamTypes.toArray(new Class<?>[0]));
				try {
					valReflection = new ValueReflection(reflectedClass, constructor.newInstance(realArgs.toArray(new Object[0])));
				} catch (InstantiationException e) {
					e.printStackTrace();
					throw new ExceptionSemantic("Could not create instance of " + className + ".");
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					throw new ExceptionSemantic("Could not access constructor of " + className + ".");
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw new ExceptionSemantic("Invalid arguments provided to constructor of " + className + ".");
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					throw new ExceptionSemantic("Invocation target invalid.");
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Could not find constructor of " + className + ".");
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		} else {
			try {
				valReflection = new ValueReflection(reflectedClass, reflectedClass.newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Could not create new instance of " + className + ".");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Could not access constructor of " + className + ".");
			}
		}
		
		return valReflection;
	}
	
	private Value invokeReflectionMethod(String methodPath, ASTReflection node) {
		var indexOfLastDot = methodPath.lastIndexOf('.');
		var classPath = methodPath.substring(0, indexOfLastDot);
		var methodName = methodPath.substring(indexOfLastDot + 1, methodPath.length());
		
		Class<?> reflectedClass = null;
		Method method = null;
		
		var expectedParamTypes = new ArrayList<Class<?>>();
		var realArgs = new ArrayList<Object>();
		
		for (var i = 1; i < node.jjtGetNumChildren(); ++i) {
			var val = doChild(node, i);
			Class<?> javaClass = null;
			
			if(val instanceof ValueReflection) {
				javaClass = ((ValueReflection) val).getInstance().getClass();
			} else {
				javaClass = CScharfUtil.getJavaClassFromValueClass(val.getClass());
			}
			
			expectedParamTypes.add(javaClass);
			realArgs.add(CScharfUtil.getJavaValueFromValueType(val));
		}
		
		try {
			reflectedClass = Class.forName(classPath);
			method = reflectedClass.getMethod(methodName, expectedParamTypes.size() > 0 ? expectedParamTypes.toArray(new Class<?>[0]) : null);
			return CScharfUtil.getValueTypeFromJavaValue(method.invoke(null, realArgs.toArray()));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExceptionSemantic("Could not invoke method " + methodName + ".");
		}
	}

	public Object visit(ASTReflectionBase node, Object data) {
		return data;
	}
	
	public Object visit(ASTCast node, Object data) {
		return data;
	}

	public Object visit(ASTPrimaryExpression node, Object data) {
		if (node.jjtGetNumChildren() == 1) {
			return doChild(node, 0);
		}
				
		Value value = null;	
		
		if (node.jjtGetChild(0) instanceof ASTCast) {
			value = doChild(node, 1);
			
			var castNode = getChild(node, 0);
			
			if (castNode.jjtGetChild(0) instanceof ASTType) {
				value = CScharfUtil.castValueToTypeByString(value, getTokenOfChild(castNode, 0));
			} else if (castNode.jjtGetChild(0) instanceof ASTCharacter) {
				if (!(value instanceof ValueReflection)) {
					throw new ExceptionSemantic("Cannot cast primitive type to a reflection type.");
				} else {
					try {
						((ValueReflection) value).setClassTypeAsSuperClass(Class.forName(doChild(castNode, 0).stringValue()));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			var firstChild = (SimpleNode) node.jjtGetChild(0);
			var secondChild = (SimpleNode) node.jjtGetChild(1);
			var prefixIncDec = firstChild instanceof ASTIncrementDecrement;
			var postfixIncDec = secondChild instanceof ASTIncrementDecrement;
			var increment = prefixIncDec ? firstChild.tokenValue.equals("++") : secondChild.tokenValue.equals("++");
			
			if (prefixIncDec && postfixIncDec) {
				throw new ExceptionSemantic("Cannot use both pre and post fix incrementation/decrementation operators at the same time.");
			}
			
			var derefNode = (ASTDereference) node.jjtGetChild(prefixIncDec ? 1 : 0);
			
			if (postfixIncDec) {
				value = doChild(node, 0);
			}
			
			untypedAssignment(derefNode.tokenValue, null, derefNode, data, true, increment, prefixIncDec);
			
			if (prefixIncDec) {
				value = doChild(node, 1);
			}
		}
	
		return value;
	}

	public Object visit(ASTIncrementDecrement node, Object data) {
		return data;
	}
}