package uk.ac.derby.ldi.CScharf;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;
import uk.ac.derby.ldi.CScharf.values.ValueAnonymousType;
import uk.ac.derby.ldi.CScharf.values.ValueArray;
import uk.ac.derby.ldi.CScharf.values.ValueBoolean;
import uk.ac.derby.ldi.CScharf.values.ValueFn;
import uk.ac.derby.ldi.CScharf.values.ValueInteger;
import uk.ac.derby.ldi.CScharf.values.ValueRational;
import uk.ac.derby.ldi.CScharf.values.ValueString;

/** Convenient runner for the CScharf interpreter. */

public class CScharfUtil {
	public static Class<?> getClassFromString(String type) {
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
		case "function":
			return ValueFn.class;
		case "array":
			return ValueArray.class;
		default:
			throw new ExceptionSemantic("Invalid type specified.");
	}
	}
}
