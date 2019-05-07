package uk.ac.derby.ldi.CScharf.values;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import uk.ac.derby.ldi.CScharf.CScharfUtil;
import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueReflection extends ValueAbstract {
	private Class<?> innerClass = null;
	private Object innerInstance = null;
	private String name = "";
	
	public ValueReflection() {}
	
	public ValueReflection(Object instance) {
		innerInstance = instance;
		innerClass = instance.getClass();
	}
	
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
	
	public void setInstance(Object instance) {
		innerInstance = instance;
	}
	
	public Object getInstance() {
		return innerInstance;
	}
	
	public void setClassType(Class<?> type) {
		innerClass = type;
	}
	
	public Class<?> getClassType() {
		return innerClass;
	}
	
	public Value invokeMethod(String name, ArrayList<Value> arguments) {
		var expectedParamTypes = new ArrayList<Class<?>>();
		var realArgs = new ArrayList<Object>();
		
		for (var val : arguments) {
			var javaClass = CScharfUtil.getJavaClassFromValueClass(val.getClass());
			expectedParamTypes.add(javaClass);
			realArgs.add(CScharfUtil.getJavaValueFromValueType(val));
		}

		try {
			var method = innerClass.getMethod(name, expectedParamTypes.toArray(new Class[0]));

			try {
				var obj = method.invoke(innerInstance, realArgs.toArray());
				
				if (obj == null) {
					return null;
				}
				
				return CScharfUtil.getValueTypeFromJavaValue(obj);
				
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Could not access " + name + ".");
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Invalid arguments provided to " + name + ".");
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				throw new ExceptionSemantic("Invocation target invalid.");
			}
			
		} catch (NoSuchMethodException e) {	
			e.printStackTrace();
			throw new ExceptionSemantic("Could not find " + name + ".");
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new ExceptionSemantic("Security exception.");
		}
	}
}
