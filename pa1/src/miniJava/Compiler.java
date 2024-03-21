package miniJava;

import java.io.FileInputStream;
import java.io.IOException;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws IOException {
		// TODO: Instantiate the ErrorReporter object
		ErrorReporter reporter = new ErrorReporter();
		// TODO: Check to make sure a file path is given in args
		if (args.length == 0) {
			System.out.println("Error");
			return;
		}
		// TODO: Create the inputStream using new FileInputStream
		FileInputStream inputStream = new FileInputStream(args[0]);
		// TODO: Instantiate the scanner with the input stream and error object
		Scanner scanner = new Scanner(inputStream, reporter);

		// TODO: Instantiate the parser with the scanner and error object
		Parser parser = new Parser(scanner, reporter);
		// TODO: Call the parser's parse function
		AST abstractSyntaxTree = parser.parse();
		ASTDisplay display = new ASTDisplay();
		// TODO: Check if any errors exist, if so, println("Error")
		//  then output the errors
		if (reporter.hasErrors()) {
			//display.showTree(abstractSyntaxTree);
			System.out.println("Error");
			reporter.outputErrors();
		}
		// TODO: If there are no errors, println("Success")
		else {
			//display.showTree(abstractSyntaxTree);
			System.out.println("Success");
		}
	}
}
