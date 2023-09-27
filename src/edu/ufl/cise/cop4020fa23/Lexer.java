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

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;


public class Lexer implements ILexer {

	String input;
	public Lexer(String input) {this.input = input;}

	@Override
	public IToken next() throws LexicalException {


		//Create array of string chars
		char [] ch = input.toCharArray();

		for (int i = 0; i < ch.length; i++){

			char curr = ch[i];

			//check for whitespace


			//Letter
			if (curr == ) {

			}






		}


		//Detect whitespace







		return new Token(EOF, 0, 0, null, new SourceLocation(1, 1));
	}




}
