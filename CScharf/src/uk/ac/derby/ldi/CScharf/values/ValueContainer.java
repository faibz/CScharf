package uk.ac.derby.ldi.CScharf.values;

public interface ValueContainer {
	public Value getVariable(String name);
	public void setVariable(String name, Value value);
}
