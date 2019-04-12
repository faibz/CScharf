package uk.ac.derby.ldi.CScharf;

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
	}
	
	public static final Class<?> getClassFromString(String type) {
		switch(type) {
		case "int":
		case "short":
		case "long":
			return ValueInteger.class;
		case "float":
		case "dec":
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
		default:
			throw new ExceptionSemantic("Invalid type specified.");
		}
	}
		
	public static final <T> T getDefaultValueForClass(Class<?> _class) {
		return (T)defaultValues.get(_class);
	}
}
