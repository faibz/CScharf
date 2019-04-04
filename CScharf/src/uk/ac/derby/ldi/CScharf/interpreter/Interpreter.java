package uk.ac.derby.ldi.CScharf.interpreter;

import uk.ac.derby.ldi.CScharf.parser.ast.ASTCode;
import uk.ac.derby.ldi.CScharf.parser.ast.CScharf;
import uk.ac.derby.ldi.CScharf.parser.ast.CScharfVisitor;

public class Interpreter {
	
	private static void usage() {
		System.out.println("Usage: CScharf [-d1] < <source>");
		System.out.println("          -d1 -- output AST");
	}
	
	public static void main(String args[]) {
		boolean debugAST = false;
		if (args.length == 1) {
			if (args[0].equals("-d1"))
				debugAST = true;
			else {
				usage();
				return;
			}
		}
		CScharf language = new CScharf(System.in);
		try {
			ASTCode parser = language.code();
			CScharfVisitor nodeVisitor;
			if (debugAST)
				nodeVisitor = new ParserDebugger();
			else
				nodeVisitor = new Parser();
			parser.jjtAccept(nodeVisitor, null);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
	}
}
