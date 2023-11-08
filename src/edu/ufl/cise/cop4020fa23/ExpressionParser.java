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

		return e;
	}
	private Expr expr() throws PLCCompilerException {
		Expr leftExpr = primary();

		while(t.kind()==PLUS || t.kind() == MINUS){
			IToken opToken = t;
			consume(t.kind());
			//if (t.kind() == PLUS){consume(PLUS);}else if(t.kind() == MINUS){consume(MINUS);}
			//consume(COMMA);
			Expr rightExpr = primary();
			leftExpr = new BinaryExpr(opToken, leftExpr, opToken, rightExpr);
		}

		return leftExpr;

	}
	private Expr primary() throws PLCCompilerException {
		IToken firstToken = t;
		Expr expr;
		switch (firstToken.kind()) {
			case STRING_LIT:
				return new StringLitExpr(t);
			case NUM_LIT:
				return new NumLitExpr(t);
			case MINUS:
				return unaryExpr();
			case BOOLEAN_LIT:
				return new BooleanLitExpr(t);
			case CONST:
			case BANG:
				return unaryExpr();
			case LPAREN:
				consume(LPAREN);
				expr = expr();
				consume(RPAREN);
				if (t.kind() == COLON){
					ChannelSelector channelSelector = channelSelector();
					return new PostfixExpr(firstToken, expr, null, channelSelector);
				}
				return expr;

			case LSQUARE:
				consume(LSQUARE);
				expr = expr();
				consume(RSQUARE);
				if (t.kind() == COLON){
					ChannelSelector channelSelector = channelSelector();
					return new PostfixExpr(firstToken, expr, null, channelSelector);
				}
				return expr;

			case IDENT:
				IdentExpr identExpr = new IdentExpr(t);
				consume(IDENT);
				PixelSelector pixel = null;
				ChannelSelector channel = null;

				if (t.kind() == LSQUARE){
					PixelSelector pixelSelector = pixelSelector();
					return new PostfixExpr(firstToken, identExpr, pixelSelector, null);

				}
				if (t.kind() == COLON){
					ChannelSelector channelSelector = channelSelector();
					//channel = channelSelector();
					return new PostfixExpr(firstToken, identExpr, null, channelSelector);
					//channel = channelSelector();
					//return new PostfixExpr(firstToken, identExpr, pixel, channel);

				}


				//return identExpr;
				//return new PostfixExpr(firstToken, identExpr, pixel, channel);
				return identExpr;

			default:
				throw new SyntaxException("Syntax Error: Unexpected token " + firstToken.text());
		}
	}
	private ChannelSelector channelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(COLON);

		IToken colorToken = t;
		switch (colorToken.kind()) {
			case RES_red:
				consume(RES_red);

				break;
			case RES_green:
				consume(RES_green);
				break;
			case RES_blue:
				consume(RES_blue);
				break;
			default:
				throw new SyntaxException("Expected color selector after ':' but found " + colorToken.text());
		}
		return new ChannelSelector(firstToken, colorToken);
	}
	private PixelSelector pixelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(LSQUARE); // Consume the '['
		Expr xExpr = expr(); // Parse the x
		consume(t.kind());
		consume(COMMA); // Consume the comma
		Expr yExpr = expr(); // the y expression
		consume(RSQUARE);
		return new PixelSelector(firstToken, xExpr, yExpr); //
	}

	private Expr unaryExpr() throws PLCCompilerException{ //Throws PLCCompilerException in order to call primary() function
		IToken opToken = t; //operator token
		//consume(MINUS);
		if (t.kind() == MINUS){
			consume(MINUS);
		}
		else if (t.kind() == BANG){
			consume(BANG);
		}
		else if (t.kind() == PLUS){
			consume(PLUS);
		}
		 // or match any other unary operator you have
		Expr e = primary(); // This will get the operand for the unary operator
		return new UnaryExpr(opToken, opToken, e);

	}

	private void consume(Kind expected) throws LexicalException {
		if (t.kind() == expected) {
			t = lexer.next();
		} else {
			//throw new SyntaxException("Syntax Error" + t.kind());
		}
	}
}
