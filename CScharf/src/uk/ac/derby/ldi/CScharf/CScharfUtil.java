package uk.ac.derby.ldi.CScharf;

import java.lang.reflect.Array;
import java.util.HashMap;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;
import uk.ac.derby.ldi.CScharf.values.Value;
import uk.ac.derby.ldi.CScharf.values.ValueAnonymousType;
import uk.ac.derby.ldi.CScharf.values.ValueArray;
import uk.ac.derby.ldi.CScharf.values.ValueBoolean;
import uk.ac.derby.ldi.CScharf.values.ValueClass;
import uk.ac.derby.ldi.CScharf.values.ValueFn;
import uk.ac.derby.ldi.CScharf.values.ValueInteger;
import uk.ac.derby.ldi.CScharf.values.ValueRational;
import uk.ac.derby.ldi.CScharf.values.ValueReflection;
import uk.ac.derby.ldi.CScharf.values.ValueString;

/** Convenient runner for the CScharf interpreter. */

public class CScharfUtil {

	static final HashMap<Class<?>, Value> defaultValues = new HashMap<Class<?>, Value>();
	
	static {
		defaultValues.put(ValueInteger.class, new ValueInteger(0));
		defaultValues.put(ValueRational.class, new ValueRational(0.0));
		defaultValues.put(ValueBoolean.class, new ValueBoolean(false));
		defaultValues.put(ValueString.class, new ValueString(""));
		defaultValues.put(ValueAnonymousType.class, new ValueAnonymousType());
		defaultValues.put(ValueFn.class, new ValueFn());
		defaultValues.put(ValueArray.class, new ValueArray());
		defaultValues.put(ValueClass.class, new ValueClass());
		defaultValues.put(ValueReflection.class, new ValueReflection());
	}
	
	public static final Class<?> getClassFromString(String type) {
		switch(type) {
			case "int":
				return ValueInteger.class;
			case "float":
				return ValueRational.class;
			case "bool":
				return ValueBoolean.class;
			case "string":
				return ValueString.class;
			case "anon":
				return ValueAnonymousType.class;
			case "func":
				return ValueFn.class;
			case "array":
				return ValueArray.class;
			case "instance":
				return ValueClass.class;
			case "reflection":
				return ValueReflection.class;
			case "void":
				return null;
			default:
				throw new ExceptionSemantic("Invalid type specified.");
		}
	}
	
	public static final String getStringFromClass(Class<?> type) {
		if (type.equals(ValueInteger.class)) return "int";
		else if (type.equals(ValueRational.class)) return "float";
		else if (type.equals(ValueBoolean.class)) return "bool";
		else if (type.equals(ValueString.class)) return "string";
		else if (type.equals(ValueAnonymousType.class)) return "anon";
		else if (type.equals(ValueFn.class)) return "func";
		else if (type.equals(ValueArray.class)) return "array";
		else if (type.equals(ValueClass.class)) return "instance";
		else if (type.equals(ValueReflection.class)) return "reflection";
		else throw new ExceptionSemantic("Could not resolve value to a type.");
	}
	
	public static final Class<?> getJavaClassFromValueClass(Class<?> type) {
		if (type.equals(ValueInteger.class)) return Integer.class;
		else if (type.equals(ValueRational.class)) return float.class;
		else if (type.equals(ValueBoolean.class)) return boolean.class;
		else if (type.equals(ValueString.class)) return String.class;
		else if (type.equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java class.");
		else if (type.equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java class.");
		else if (type.equals(ValueArray.class)) return Array.class;
		else if (type.equals(ValueClass.class)) return Class.class;
		else if (type.equals(ValueReflection.class)) return Class.class; //Will this complicate things?
		else throw new ExceptionSemantic("Could not resolve value " + type + " to a Java class.");
	}
		
	public static final Value getDefaultValueForClass(Class<?> type) {
		return defaultValues.get(type);
	}
}
