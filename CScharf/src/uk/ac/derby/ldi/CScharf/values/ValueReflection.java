package uk.ac.derby.ldi.CScharf.values;

import java.util.Vector;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueReflection extends ValueAbstract implements ValueContainer {
	private Class<?> innerClass = null;
	private Object innerInstance = null;
	private String name = "";
	
	public ValueReflection() {}
	
	public ValueReflection(Class<?> type, Object obj) {
		innerClass = type;
		innerInstance = obj;
	}
	
	public int compare(Value v) {
		if (!(v instanceof ValueReflection)) {
			throw new ExceptionSemantic("Reflection instances can only be compared to other reflection instances.");	
		}
		
		return 1;
	}	
	
	public String getName() {
		return name;
	}

	public Value getVariable(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void invokeMethod(String name, Vector<Value> arguments) {
		System.out.println("HI");
		var expectedParamTypes = new Vector<Class<?>>();
		
		for (var val : arguments) {
			expectedParamTypes.add(CScharfUtil.getJavaClassFromValueClass(val.getClass()));
		}
		
		for (var val : expectedParamTypes) {
			System.out.println(val);
		}
		
		//innerClass.getMethod(name, expectedParamTypes.toArray(box));
		try {
			var paramArray = (Class<?>[]) expectedParamTypes.toArray();
			System.out.println("cast complete");
			innerClass.getMethod(name, paramArray);
			System.out.println("FOUND METHOD");
		} catch (NoSuchMethodException e) {
			System.out.println("no such method");
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

}