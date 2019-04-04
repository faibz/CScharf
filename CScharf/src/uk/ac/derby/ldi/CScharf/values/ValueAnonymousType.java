package uk.ac.derby.ldi.CScharf.values;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import uk.ac.derby.ldi.CScharf.interpreter.ExceptionSemantic;

public class ValueAnonymousType extends ValueAbstract {
	private UUID id = UUID.randomUUID();
	private Map<String, Value> variables = new HashMap<String, Value>();

	public String getName() {
		return "Dynamic type";
	}

	public int compare(Value v) {
		return 0;
	}
	
	public void addValue(String name, Value value) {
		variables.put(name, value);
	}
	
	public Value getVariableValue(String name) {
		if (variables.containsKey(name)) {
			return variables.get(name);
		}
		
		throw new ExceptionSemantic("Member variable does not exist.");
	}
	
	public int getVariableCount() {
		return variables.size();
	}
	
	public String toString() {
		String memberStrings = "";
				
		for(Map.Entry<String, Value> variable: variables.entrySet()) {
			memberStrings = memberStrings.concat(variable.getKey() + "=" + variable.getValue() + ",");
		}
		
		return id + "{" + memberStrings.substring(0, memberStrings.length() - 1) + "}";
	}
}