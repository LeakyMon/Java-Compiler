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

import java.util.Arrays;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import static edu.ufl.cise.cop4020fa23.Kind.*;

/**
 Expr::= ConditionalExpr | LogicalOrExpr
 ConditionalExpr ::= ? Expr -> Expr , Expr
 LogicalOrExpr ::= LogicalAndExpr ( ( | | || ) LogicalAndExpr)*
 LogicalAndExpr ::= ComparisonExpr ( ( & | && ) ComparisonExpr)*
 ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
 PowExpr ::= AdditiveExpr ** PowExpr | AdditiveExpr
 AdditiveExpr ::= MultiplicativeExpr ( ( + | - ) MultiplicativeExpr )*
 MultiplicativeExpr ::= UnaryExpr (( * | / | % ) UnaryExpr)*
 UnaryExpr ::= ( ! | - | width | height) UnaryExpr | PostfixExpr
 PostfixExpr::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
 PrimaryExpr ::=STRING_LIT | NUM_LIT | BOOLEAN_LIT | IDENT | ( Expr ) | CONST |
 ExpandedPixelExpr
 ChannelSelector ::= : red | : green | : blue
 PixelSelector ::= [ Expr , Expr ]
 ExpandedPixelExpr ::= [ Expr , Expr , Expr ]

 */

public class ExpressionParser implements IParser {
	
	final ILexer lexer;
	private IToken t;
	

	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}


	@Override
	public AST parse() throws PLCCompilerException {
		Expr e = expr();
		//check for EOF

		return e;
	}
	private Expr expr() throws PLCCompilerException {return primary();}
	private Expr primary() throws PLCCompilerException {
		IToken firstToken = t;
		switch (firstToken.kind()) {
			case STRING_LIT:
				return new StringLitExpr(t);
				//return stringLit();
			case NUM_LIT:
				return new NumLitExpr(t);
				//return numLit();
			case IDENT:
				return new IdentExpr(t);
			case MINUS:
				//return new UnaryExpr(t);
			case BOOLEAN_LIT:
				return new BooleanLitExpr(t);
			case CONST:

			default:
				throw new SyntaxException("Syntax Error: Unexpected token " + firstToken.text());
		}
	}



}
