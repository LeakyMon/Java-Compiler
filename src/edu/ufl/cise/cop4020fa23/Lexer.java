
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
import java.sql.SQLOutput;

public class Lexer implements ILexer {

	//Create variables for position, column, line, and source
	private int pos;
	private int column;
	private int line;
	private final char[] source;
	private boolean newlineseen;
	//VARIABLE TO HELP HANDLE COMMENTS
	private final static char EOF_CHAR = '\0';

	public Lexer(String input)
	{
		//Initialize variables
		this.source = input.toCharArray();
		this.pos = 0;
		this.line = 1;
		this.column = 1;
		this.newlineseen = false;
	}
	@Override
	public Token next() throws LexicalException {
		while (true) {

			skipWhiteSpace();
			if (isEOF()) {
				return new Token(Kind.EOF, pos, 0, source, new SourceLocation(line, column));
			}
			char ch = peek();

			if (ch == '#') { // Start of a comment
				if (peekNext() == '#') { // Confirm it is the comment token ##
					skipComment();
					continue; // Go to the top of the loop after skipping the comment
				}
				else {
					throw new LexicalException("Illegal character '" + ch + "' at line " + line + ", column " + column);

				}
			}



			if (Character.isDigit(ch)) {
				return scanNumber();
			}
			//Check if letter/word
			if (Character.isLetter(ch)) {
				return scanIdentifierOrKeyword();
			}
			//Switch case for each character
			switch (ch) {
				case ',':
					increment();
					return new Token(Kind.COMMA, pos - 1, 1, source, new SourceLocation(line, column - 1));
				//CASES WITH DOUBLE POSIBILITES IE BOX, <<, ECT...
				case '[':
					increment();
					//If double bracket
					if (peek() == ']') {
						increment();
						return new Token(Kind.BOX, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.LSQUARE, pos - 1, 1, source, new SourceLocation(line, column));
				case '<':
					increment();
					if (peek() == '=') {
						increment();
						return new Token(Kind.LE, pos - 2, 2, source, new SourceLocation(line, column));
					} else if (peek() == ':') {
						increment();
						return new Token(Kind.BLOCK_OPEN, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.LT, pos - 1, 1, source, new SourceLocation(line, column));
				case '=':
					increment();
					if (peek() == '=') {
						increment();
						return new Token(Kind.EQ, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.ASSIGN, pos - 1, 1, source, new SourceLocation(line, column));
				case '-':
					increment();
					if (peek() == '>') {
						increment();
						return new Token(Kind.RARROW, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.MINUS, pos - 1, 1, source, new SourceLocation(line, column));
				case '*':
					increment();
					if (peek() == '*'){
						increment();
						return new Token(Kind.EXP, pos-2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.TIMES, pos - 1, 1, source, new SourceLocation(line, column));
				case '|':
					increment();
					if (peek() == '|'){
						increment();
						return new Token(Kind.OR, pos -2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.BITOR, pos - 1, 1, source, new SourceLocation(line, column));
					//&& Case NOT WORKING
				case '&':
					int startColumn = column; // G
					int startLine = line;
					increment();
					if (peek() == '&') {
						increment();
						//START COLUMN AND LINE TO ACCOUNT FOR STARTING POSITION OF &&
						return new Token(Kind.AND, pos - 2, 2, source, new SourceLocation(startLine, startColumn));
					}
					return new Token(Kind.BITAND, pos - 1, 1, source, new SourceLocation(startLine, startColumn));

				case '^':
					increment();
					return new Token(Kind.RETURN, pos - 1, 1, source, new SourceLocation(line, column));
				case '(':
					increment();
					return new Token(Kind.LPAREN, pos - 1, 1, source, new SourceLocation(line, column));
				case ')':
					increment();
					return new Token(Kind.RPAREN, pos - 1, 1, source, new SourceLocation(line, column));
				case '+':
					increment();
					return new Token(Kind.PLUS, pos - 1, 1, source, new SourceLocation(line, column));
				case '>':
					increment();
					if (peek() == '=') {
						increment();
						return new Token(Kind.GE, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.GT, pos - 1, 1, source, new SourceLocation(line, column));
				case ']':
					increment();
					return new Token(Kind.RSQUARE, pos - 1, 1, source, new SourceLocation(line, column));
				case '%':
					increment();
					return new Token(Kind.MOD, pos - 1, 1, source, new SourceLocation(line, column));

				case '/':
					increment();
					return new Token(Kind.DIV, pos - 1, 1, source, new SourceLocation(line, column));
				case '?':
					increment();
					return new Token(Kind.QUESTION, pos - 1, 1, source, new SourceLocation(line, column));
				case '!':
					increment();
					return new Token(Kind.BANG, pos - 1, 1, source, new SourceLocation(line, column));
				case ';':
					increment();
					return new Token(Kind.SEMI, pos - 1, 1, source, new SourceLocation(line, column - 1));
				case ':':
					increment();
					if (peek() == '>'){
						increment();
						return new Token(Kind.BLOCK_CLOSE, pos - 2, 2, source, new SourceLocation(line, column));
					}
					return new Token(Kind.COLON, pos -1, 1, source, new SourceLocation(line, column));
					//STRING LITERALS
				case '"':
					return scanString();

				default:
					throw new LexicalException("Unrecognized character '" + ch + "' at position " + pos);
			}
		}
	}
	//SKIP COMMENTS
	private void skipComment() {
		while (peek() != '\n' && peek() != EOF_CHAR) {
			increment(); // This consumes the character.
		}
		if (peek() == '\n') { // Only increment if there is a newline character to consume.
			increment(); // This consumes the newline
		}

	}
	//DOUBLE PEEK
	private char peekNext() {
		return (pos + 1) < source.length ? source[pos + 1] : EOF_CHAR;
	}
	//SKIPWHITESPACE
	private void skipWhiteSpace() {
		while (!isEOF() && Character.isWhitespace(peek())) {
			increment();
			//if (peek() == '\n') {
			//	column = 0;
			//	line++; // Reset column number at the start of a new line
			//	increment();  // Consume the newline character
			//} else {
			//	increment();  // Consume other whitespace characters
			//}
		}
		handleNewLineSeen();
	}
	//SCAN NUMBER
	private Token scanNumber() throws LexicalException {
		int startPos = pos;
		if (peek() == '0') { // If the number starts with 0, treat it as a separate token
			increment();
			return new Token(Kind.NUM_LIT, startPos, pos - startPos, source, new SourceLocation(line, column));
		}
		while (Character.isDigit(peek())) {
			increment();
		}
		String numStr = new String(source, startPos, pos - startPos);
		//TO MAKE SURE INTEGER IS IN RANGE
		try {
			Integer.parseInt(numStr);
		} catch (NumberFormatException e) {
			throw new LexicalException("Number out of range at line " + line + ", column " + column);
		}
		return new Token(Kind.NUM_LIT, startPos, pos - startPos, source, new SourceLocation(line, column));
	}
	private Token scanIdentifierOrKeyword() throws LexicalException {
		int startPos = pos;
		int startColumn = column - (pos - startPos); //Pos - startPos to find length
		if (!Character.isLetter(peek())) {
			throw new LexicalException("Unterminated string. Line: " + line + ", Column: " + column);
		}
		while (Character.isLetterOrDigit(peek()) || peek() == '_') {
			increment();
		}
		String identifier = new String(source, startPos, pos - startPos);
		if (identifier.equals("TRUE") || identifier.equals("FALSE")) {
			return new Token(Kind.BOOLEAN_LIT, startPos, pos - startPos, source, new SourceLocation(line, startColumn));
		}
		if (checkReservedWords(identifier)){
			if (identifier.equals("red")){return new Token(Kind.RES_red, startPos, pos - startPos, source, new SourceLocation(line, startColumn));}
			else if (identifier.equals("blue")){return new Token(Kind.RES_blue, startPos, pos - startPos, source, new SourceLocation(line, startColumn));}
			else if (identifier.equals("green")){return new Token(Kind.RES_green, startPos, pos - startPos, source, new SourceLocation(line, startColumn));}
		}

		if (isConstant(identifier)) {
			return new Token(Kind.CONST, startPos, pos - startPos, source, new SourceLocation(line, startColumn));
		}

		//IF THE TOKEN IS AN IDENTIFIER
		return new Token(Kind.IDENT, startPos, pos - startPos, source, new SourceLocation(line, startColumn));
	}

	private boolean checkReservedWords(String identifier){
		boolean temp = false;
		if (identifier.equals("red") || identifier.equals("blue") || identifier.equals("green")){

			temp = true;
			return temp;
		}
		return temp;
	}

	private boolean isConstant(String identifier) {
		//SWITCH CASE TO FIND THE IDENTIFIER IF ITS CONSTANT
		switch(identifier) {
			case "Z":
			case "BLACK":
			case "BLUE":
			case "CYAN":
			case "DARK_GRAY":
			case "GRAY":
			case "GREEN":
			case "LIGHT_GRAY":
			case "MAGENTA":
			case "ORANGE":
			case "PINK":
			case "RED":
			case "WHITE":
			case "YELLOW":
				return true;
			default:
				return false;
		}
	}

	private Token scanString() throws LexicalException {
		int startPos = pos;
		increment(); // to account for the opening quote mark
		StringBuilder stringValue = new StringBuilder();
		while (peek() != '"' && !isEOF()) {
			if (isInvalidChar(peek())) {
				throw new LexicalException("Illegal character in string literal at line: " + line + ", column: " + column);
			}
			if (peek() == '\\' && peekNext() == 's') {
				stringValue.append(' '); // Handle \s escape sequence as space
				increment(); // Skip backslash
				increment(); // Skip 's'
			}
			else if (peek() == '\\' && peekNext() == '\\') {
				stringValue.append(' '); // Handle \s escape sequence as space
				increment(); // Skip backslash
				increment(); // Skip 's'
			}
			else if (peek() == '\n' || peek() == '\r') {
				// If you encounter a newline or carriage return before the closing quote, throw an exception
				throw new LexicalException("String literal not closed on the same line");
			}

			else {
				stringValue.append(source[pos]);
				increment();
			}
			//increment();
		}
		if (isEOF()) {
			throw new LexicalException("Unterminated string. Line: " + line + ", Column: " + column);
		}
		increment(); // consume the closing quote


	//	String stringValue = new String(source, startPos, pos - startPos); // +1 and -2 to exclude quotes

		//String stringValue = new String(source, startPos + 1, pos - startPos - 2); // +1 and -2 to exclude quotes
		return new Token(Kind.STRING_LIT, startPos, pos - startPos, source, new SourceLocation(line, column));
	}

	private char peek() {
		if (isEOF()) return EOF_CHAR;
		char curr = getSourcePos();
		return curr;
	}
	//Helper method to return sourcepos
	private char getSourcePos(){
		return source[pos];
	}
	private void increment() {
		pos++; // Move to the next char pos
		if (pos < source.length && source[pos] == '\n') {
			//line++;
			//column = 1; // Column Reset
			newlineseen = true;
		} else {
			if (!newlineseen) {
				column++;
			}
			//column++; // Increment column for next char
		}
	}
	private void handleNewLineSeen(){
		if (newlineseen){
			line++;
			column = 1;
			newlineseen = false;
		}
	}
	private boolean isInvalidChar(char c){
		String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ$_+0123456789@#&%";
		if (validChars.indexOf(c) == -1) {

			return true;
		}
		return false; // It is a valid character
	}
	private boolean isEOF() {
		return pos >= source.length;
	}

}