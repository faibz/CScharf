package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueAnonymousType extends ValueAbstract implements ValueContainer {
	private UUID id = UUID.randomUUID();
	private Map<String, Value> variables = new HashMap<String, Value>();

	public String getName() {
		return "Anonymous type";
	}

	public int compare(Value v) {
		return 0;
	}
	
	/* Only called when initialising an object.
	 * Calling this after initialisation is not supported.
	 * Unsure if allowing for values to be re-set (like what is possible with arrays) will be supported. */
	public void addValue(String name, Value value) {
		variables.put(name, value);
	}
	
	public Value getVariable(String name) {
		if (variables.containsKey(name)) {
			return variables.get(name);
		}
		
		throw new ExceptionSemantic("Member variable does not exist.");
	}
	
	public int getVariableCount() {
		return variables.size();
	}
	
	public String toString() {
		var memberStrings = "";
				
		for(Map.Entry<String, Value> variable: variables.entrySet()) {
			memberStrings = memberStrings.concat(variable.getKey() + "=" + variable.getValue() + ",");
		}
		
		return id + "{" + memberStrings.substring(0, memberStrings.length() - 1) + "}";
	}

	public void setVariable(String name, Value value) {
		throw new ExceptionSemantic("Cannot edit members of ValueAnonymousType; members are immutable");
	}
}