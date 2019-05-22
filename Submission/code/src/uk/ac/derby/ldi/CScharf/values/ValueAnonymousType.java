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
	
	/* Only called when initialising an object. Anonymous type members are immutable. */
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
				
		for(var key: variables.keySet()) {
			memberStrings = memberStrings.concat(key + "=" + variables.get(key) + ",");
		}
		
		return id + "{" + memberStrings.substring(0, memberStrings.length() - (memberStrings.length() > 0 ? 1 : 0)) + "}";
	}
	
	public String stringValue() {
		return toString();
	}

	public void setVariable(String name, Value value) {
		throw new ExceptionSemantic("Cannot edit members of ValueAnonymousType; members are immutable");
	}
}