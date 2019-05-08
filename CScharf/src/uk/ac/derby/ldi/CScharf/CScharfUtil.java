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
		if (type.equals(ValueInteger.class)) return int.class;
		else if (type.equals(ValueRational.class)) return float.class;
		else if (type.equals(ValueBoolean.class)) return boolean.class;
		else if (type.equals(ValueString.class)) return String.class;
		else if (type.equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java class.");
		else if (type.equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java class.");
		else if (type.equals(ValueArray.class)) throw new ExceptionSemantic("Cannot resolve ValueArray to a Java class.");
		else if (type.equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java class.");
		else if (type.equals(ValueReflection.class)) throw new ExceptionSemantic("Cannot resolve ValueReflection to a Java class.");
		else throw new ExceptionSemantic("Could not resolve value " + type + " to a Java class.");
	}
	
	public static final Class<?> getJavaClassFromValue(Value val) {
		var type = val.getClass();
		
		if (type.equals(ValueInteger.class)) return int.class;
		else if (type.equals(ValueRational.class)) return float.class;
		else if (type.equals(ValueBoolean.class)) return boolean.class;
		else if (type.equals(ValueString.class)) return String.class;
		else if (type.equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java class.");
		else if (type.equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java class.");
		else if (type.equals(ValueArray.class)) throw new ExceptionSemantic("Cannot resolve ValueArray to a Java class.");
		else if (type.equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java class.");
		else if (type.equals(ValueReflection.class)) return ((ValueReflection) val).getClassType();
		else throw new ExceptionSemantic("Could not resolve value " + type + " to a Java class.");
	}
	
	public static final Object getJavaValueFromValueType(Value val) {
		if (val.getClass().equals(ValueInteger.class)) return (int) val.longValue();
		else if (val.getClass().equals(ValueRational.class)) return (float) val.doubleValue();
		else if (val.getClass().equals(ValueBoolean.class)) return val.booleanValue();
		else if (val.getClass().equals(ValueString.class)) return val.stringValue();
		else if (val.getClass().equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java value.");
		else if (val.getClass().equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java value.");
		else if (val.getClass().equals(ValueArray.class)) throw new ExceptionSemantic("TODO?");
		else if (val.getClass().equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java value.");
		else if (val.getClass().equals(ValueReflection.class)) return ((ValueReflection) val).getInstance();
		else throw new ExceptionSemantic("Could not resolve value " + val.getClass() + " to a Java class.");
	}
	
	public static final Value getValueTypeFromJavaValue(Object obj) {
		if (obj instanceof Integer) return new ValueInteger((int) obj);
		else if (obj instanceof Double) return new ValueRational((double) obj);
		else if (obj instanceof Boolean) return new ValueBoolean((boolean) obj);
		else if (obj instanceof String) return new ValueString((String) obj);
		else if (obj instanceof Array) return new ValueArray((Array) obj);
		else return new ValueReflection(obj);
	}
		
	public static final Value getDefaultValueForClass(Class<?> type) {
		return defaultValues.get(type);
	}
}
