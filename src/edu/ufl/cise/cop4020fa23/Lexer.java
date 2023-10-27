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
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import javax.xml.transform.Source;

public class Lexer implements ILexer {

	//Create variables for position, column, line, and source
	private int pos;
	private int column;
	private int line;
	private final char[] source;

	public Lexer(String input)
	{
		//Initialize variables
		this.source = input.toCharArray();
		this.pos = 0;
		this.line = 1;
		this.column = 1;
	}

	@Override
	public Token next() throws LexicalException {
		return new Token(Kind.EOF, pos, 0, source, new SourceLocation(line, column));
	}
}