package uk.ac.derby.ldi.CScharf;

import java.lang.reflect.Array;
import java.util.HashMap;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;
import uk.ac.derby.ldi.CScharf.values.Value;
import uk.ac.derby.ldi.CScharf.values.ValueAnonymousType;
import uk.ac.derby.ldi.CScharf.values.ValueArray;
import uk.ac.derby.ldi.CScharf.values.ValueBoolean;
import uk.ac.derby.ldi.CScharf.values.ValueClass;
import uk.ac.derby.ldi.CScharf.values.ValueDouble;
import uk.ac.derby.ldi.CScharf.values.ValueFn;
import uk.ac.derby.ldi.CScharf.values.ValueInteger;
import uk.ac.derby.ldi.CScharf.values.ValueFloat;
import uk.ac.derby.ldi.CScharf.values.ValueReflection;
import uk.ac.derby.ldi.CScharf.values.ValueString;

/** Convenient runner for the CScharf interpreter. */

public class CScharfUtil {

	static final HashMap<Class<?>, Value> defaultValues = new HashMap<Class<?>, Value>();
	
	static {
		defaultValues.put(ValueInteger.class, new ValueInteger(0));
		defaultValues.put(ValueFloat.class, new ValueFloat(0.0f));
		defaultValues.put(ValueDouble.class, new ValueDouble(0.0d));
		defaultValues.put(ValueBoolean.class, new ValueBoolean(false));
		defaultValues.put(ValueString.class, new ValueString(""));
		defaultValues.put(ValueAnonymousType.class, new ValueAnonymousType());
		defaultValues.put(ValueFn.class, new ValueFn());
		defaultValues.put(ValueArray.class, new ValueArray());
		defaultValues.put(ValueClass.class, new ValueClass());
		defaultValues.put(ValueReflection.class, new ValueReflection());
	}
	
	/** Returns the default value for a CScharf type. */
	public static final Value getDefaultValueForClass(Class<?> type) {
		return defaultValues.get(type);
	}
	
	/** Returns the CScharf type equivalent of a string. */
	public static final Class<?> getClassFromString(String type) {
		switch(type) {
			case "int":
				return ValueInteger.class;
			case "float":
				return ValueFloat.class;
			case "double":
				return ValueDouble.class;
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
	
	/** Returns the string equivalent of a CScharf type. */
	public static final String getStringFromClass(Class<?> type) {
		if (type.equals(ValueInteger.class)) return "int";
		else if (type.equals(ValueFloat.class)) return "float";
		else if (type.equals(ValueDouble.class)) return "double";
		else if (type.equals(ValueBoolean.class)) return "bool";
		else if (type.equals(ValueString.class)) return "string";
		else if (type.equals(ValueAnonymousType.class)) return "anon";
		else if (type.equals(ValueFn.class)) return "func";
		else if (type.equals(ValueArray.class)) return "array";
		else if (type.equals(ValueClass.class)) return "instance";
		else if (type.equals(ValueReflection.class)) return "reflection";
		else throw new ExceptionSemantic("Could not resolve value to a type.");
	}

	/** Returns a Java class from a CScharf type. */
	public static final Class<?> getJavaClassFromValueClass(Class<?> type) {
		if (type.equals(ValueInteger.class)) return int.class;
		else if (type.equals(ValueFloat.class)) return float.class;
		else if (type.equals(ValueDouble.class)) return double.class;
		else if (type.equals(ValueBoolean.class)) return boolean.class;
		else if (type.equals(ValueString.class)) return String.class;
		else if (type.equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java class.");
		else if (type.equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java class.");
		else if (type.equals(ValueArray.class)) return Array.class;
		else if (type.equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java class.");
		else if (type.equals(ValueReflection.class)) throw new ExceptionSemantic("Cannot resolve ValueReflection to a Java class.");
		else throw new ExceptionSemantic("Could not resolve value " + type + " to a Java class.");
	}
	
	/** Returns a Java class equivalent of a CScharf value. */
	public static final Class<?> getJavaClassFromValue(Value val) {
		var type = val.getClass();
		
		if (type.equals(ValueInteger.class)) return int.class;
		else if (type.equals(ValueFloat.class)) return float.class;
		else if (type.equals(ValueDouble.class)) return double.class;
		else if (type.equals(ValueBoolean.class)) return boolean.class;
		else if (type.equals(ValueString.class)) return String.class;
		else if (type.equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java class.");
		else if (type.equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java class.");
		else if (type.equals(ValueArray.class)) return Array.class;
		else if (type.equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java class.");
		else if (type.equals(ValueReflection.class)) return ((ValueReflection) val).getClassType();
		else throw new ExceptionSemantic("Could not resolve value " + type + " to a Java class.");
	}
	
	/** Converts a CScharf value to a Java value. */
	public static final Object getJavaValueFromValueType(Value val) {
		if (val.getClass().equals(ValueInteger.class)) return (int) val.longValue();
		else if (val.getClass().equals(ValueFloat.class)) return (float) val.floatValue();
		else if (val.getClass().equals(ValueDouble.class)) return (double) val.doubleValue();
		else if (val.getClass().equals(ValueBoolean.class)) return val.booleanValue();
		else if (val.getClass().equals(ValueString.class)) return val.stringValue();
		else if (val.getClass().equals(ValueAnonymousType.class)) throw new ExceptionSemantic("Cannot resolve ValueAnonymousType to a Java value.");
		else if (val.getClass().equals(ValueFn.class)) throw new ExceptionSemantic("Cannot resolve ValueFn to a Java value.");
		else if (val.getClass().equals(ValueArray.class)) throw new ExceptionSemantic("Cannot resolve ValueArray to a Java value.");
		else if (val.getClass().equals(ValueClass.class)) throw new ExceptionSemantic("Cannot resolve ValueClass to a Java value.");
		else if (val.getClass().equals(ValueReflection.class)) return ((ValueReflection) val).getInstance();
		else throw new ExceptionSemantic("Could not resolve value " + val.getClass() + " to a Java class.");
	}
	
	/** Converts a Java value to a CScharf value. */
	public static final Value getValueTypeFromJavaValue(Object obj) {
		if (obj instanceof Integer) return new ValueInteger((int) obj);
		else if (obj instanceof Float) return new ValueFloat((float) obj);
		else if (obj instanceof Double) return new ValueDouble((double) obj);
		else if (obj instanceof Boolean) return new ValueBoolean((boolean) obj);
		else if (obj instanceof String) return new ValueString((String) obj);
		else if (obj instanceof Array) return new ValueArray((Array) obj);
		else return new ValueReflection(obj);
	}
	
	/** Maps primitive Java type classes (int, boolean, etc.) to CScharf type classes. */
	public static final Class<?> getValueClassFromJavaTypeClass(Class<?> type) {
		if (type.equals(int.class)) return ValueInteger.class;
		else if (type.equals(float.class)) return ValueFloat.class;
		else if (type.equals(double.class)) return ValueDouble.class;
		else if (type.equals(String.class)) return ValueString.class;
		
		throw new ExceptionSemantic("Cannot map " + type + " to a CScharf type class.");
	}

	/** Casts a CScharf value to another CScharf value by a type's string equivalent. */
	public static final Value castValueToTypeByString(Value value, String castToType) {
		if (castToType.equals("int")) {
			if (value instanceof ValueInteger) {
				return value;
			} else if (value instanceof ValueFloat) {
				return new ValueInteger((int) value.floatValue());
			} else if (value instanceof ValueDouble) {
				return new ValueInteger((int) value.doubleValue());
			} else if (value instanceof ValueBoolean) {
				if (value.booleanValue()) return new ValueInteger(1);
				return new ValueInteger(0);
			} else if (value instanceof ValueString) {
				return new ValueInteger(Integer.parseInt(value.stringValue()));				
			}
			throw new ExceptionSemantic("Unsupported cast.");
		} else if (castToType.equals("float")) {
			if (value instanceof ValueInteger) {
				return new ValueFloat(value.longValue());
			} else if (value instanceof ValueFloat) {
				return value;
			} else if (value instanceof ValueDouble) {
				return new ValueFloat(value.doubleValue());
			} else if (value instanceof ValueBoolean) {
				if (value.booleanValue()) return new ValueFloat(1.0f);
				return new ValueFloat(0.0f);
			} else if (value instanceof ValueString) {
				return new ValueFloat(Float.parseFloat(value.stringValue()));
			}
			throw new ExceptionSemantic("Unsupported cast.");
			
		} else if (castToType.equals("double")) {
			if (value instanceof ValueInteger) {
				return new ValueDouble(value.longValue());
			} else if (value instanceof ValueFloat) {
				return new ValueDouble(value.doubleValue());
			} else if (value instanceof ValueDouble) {
				return value;
			} else if (value instanceof ValueBoolean) {
				if (value.booleanValue()) return new ValueDouble(1.0d);
				return new ValueDouble(0.0d);
			} else if (value instanceof ValueString) {
				return new ValueDouble(Double.parseDouble(value.stringValue()));
			}
			throw new ExceptionSemantic("Unsupported cast.");
			
		} else if (castToType.equals("bool")) {
			if (value instanceof ValueInteger) {
				return new ValueBoolean((int) value.longValue());
			} else if (value instanceof ValueFloat) {
				return new ValueBoolean((int) value.floatValue());
			} else if (value instanceof ValueDouble) {
				return new ValueBoolean((int) value.doubleValue());
			} else if (value instanceof ValueBoolean) {
				return value;
			} else if (value instanceof ValueString) {
				return new ValueBoolean(value.stringValue().equals("true") ? true : false);
			}
			throw new ExceptionSemantic("Unsupported cast.");
		} else if (castToType.equals("string")) {
			if (value instanceof ValueInteger) {
				return new ValueString(value.stringValue());
			} else if (value instanceof ValueFloat) {
				return new ValueString(value.stringValue());
			} else if (value instanceof ValueDouble) {
				return new ValueString(value.stringValue());
			} else if (value instanceof ValueBoolean) {
				return new ValueString(value.stringValue());
			} else if (value instanceof ValueString) {
				return value;
			}
			throw new ExceptionSemantic("Unsupported cast.");
		}
		
		throw new ExceptionSemantic("Unsupported cast.");
	}
}
