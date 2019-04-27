package uk.ac.derby.ldi.CScharf.interpreter;

import java.util.Vector;

public class InterfaceFunction {
	private Class<?> returnType;
	private String name;
	private Vector<Class<?>> paramTypes = new Vector<Class<?>>();
	
	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setParams(Vector<Class<?>> paramTypes) {
		this.paramTypes = paramTypes;
	}
	
	public Class<?> getReturnType() {
		return returnType;
	}
	
	public String getName() {
		return name;
	}
	
	public Vector<Class<?>> getParamTypes() {
		return paramTypes;
	}
	
	public void addParam(Class<?> paramType) {
		paramTypes.add(paramType);
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null) return false;
	    if (getClass() != obj.getClass()) return false;
	    
	    InterfaceFunction function = (InterfaceFunction) obj;
	    
	    return returnType == function.returnType 
	    	&& name.equals(function.name)
	    	&& paramTypes.equals(function.paramTypes);
	}
}
