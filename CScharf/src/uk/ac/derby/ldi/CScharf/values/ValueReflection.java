package uk.ac.derby.ldi.CScharf.values;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

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
	
	public Value invokeMethod(String name, ArrayList<Value> arguments) {
		System.out.println("Searching for method named: " + name);
		var expectedParamTypes = new ArrayList<Class<?>>();
		var realArgs = new ArrayList<Object>();
		
		for (var val : arguments) {
			var javaClass = CScharfUtil.getJavaClassFromValueClass(val.getClass());
			expectedParamTypes.add(javaClass);
			realArgs.add(CScharfUtil.getJavaValueFromValueType(val));
		}
		
		var paramArray = expectedParamTypes.toArray(new Class[0]);

		try {
			var method = innerClass.getMethod(name, paramArray);
			System.out.println("FOUND METHOD");
			method.setAccessible(true); //necessary?
			try {
				var obj = method.invoke(innerInstance, realArgs.toArray());
				
				if (obj == null) {
					System.out.println("obj == null");
					return null;
				}
				
				System.out.println(obj);
				
				//var returnedValue = CScharfUtil.getValueTypeFromJavaValue(obj);
				
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (NoSuchMethodException e) {
			System.out.println("no such method");			
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
