/*Copyright 2023 by Beverly A Sanders
* 
* This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
* University of Florida during the fall semester 2023 as part of the course project.  
* 
* No other use is authorized. 
* 
* This code may not be posted on a public web site either during or after the course.  
*/
package edu.ufl.cise.cop4020fa23;

import static edu.ufl.cise.cop4020fa23.Kind.EOF;

import java.sql.SQLOutput;
import java.util.regex.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;


public class Lexer implements ILexer {

	String input;
	public Lexer(String input) {this.input = input;}

	@Override
	public IToken next() throws LexicalException {
		Pattern whitespacePattern = Pattern.compile("[\\r\\n]+");
		Pattern letterPattern = Pattern.compile("[A-Za-z]");
		Pattern bracketPattern = Pattern.compile("[\\[\\]{}()<>]");
		Pattern numberPattern = Pattern.compile("-?\\\\d+");

		Matcher matcher =  letterPattern.matcher(input);



		while (matcher.find()){
			System.out.println("Letter literal found at position " + matcher.start() + ": " + matcher.group());
		}

		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
	}




}
