
package edu.ufl.cise.cop4020fa23;
import java.nio.channels.Channel;
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
		return conditionalExpr();
	}

	private Expr conditionalExpr() throws PLCCompilerException {
		if (t.kind() == QUESTION) {
			IToken firstToken = t;
			consume(QUESTION);
			Expr condition = expr();
			consume(RARROW);
			Expr trueExpr = expr();
			consume(COMMA); //
			Expr falseExpr = expr(); // Parse the false
			return new ConditionalExpr(firstToken,condition, trueExpr, falseExpr);
		} else {
			return logicalOrExpr();
		}
	}

	private Expr logicalOrExpr() throws PLCCompilerException {
		Expr left = logicalAndExpr();
		while (t.kind() == OR ){ // OR  = logical or
			IToken opToken = t;
			consume(t.kind());
			Expr right = logicalAndExpr();
			left = new BinaryExpr(opToken, left, opToken, right);
		}
		return left;
	}
	private Expr logicalAndExpr() throws PLCCompilerException {
		Expr left = comparisonExpr();
		while (t.kind() == AND) {
			IToken opToken = t;
			consume(t.kind());
			Expr right = comparisonExpr();
			left = new BinaryExpr(opToken, left, opToken, right);
		}
		return left;
	}
	private Expr comparisonExpr() throws PLCCompilerException {
		Expr left = powExpr();
		while (t.kind() == BITOR || t.kind() == BITAND || t.kind() == EQ  || t.kind()== MOD || t.kind() == AND || t.kind() == GE ){
			IToken opToken = t;
			consume(t.kind());
			Expr right = powExpr();
			left = new BinaryExpr(opToken,left,opToken,right);
		}
		return left;
	}
	private Expr powExpr() throws PLCCompilerException {
		Expr left = additiveExpr();
		while (t.kind() == EXP){
			IToken opToken = t;
			consume(EXP);
			Expr right = powExpr();
			left = new BinaryExpr(opToken, left, opToken, right);
		}
		return left;

	}
	private Expr additiveExpr() throws PLCCompilerException {
		Expr left = multiplicativeExpr();
		while (t.kind() == PLUS || t.kind() == MINUS) {
			IToken opToken = t;
			consume(t.kind());
			Expr right = multiplicativeExpr();
			left = new BinaryExpr(opToken, left, opToken, right);
		}
		return left;
	}

	private Expr multiplicativeExpr() throws PLCCompilerException {
		Expr left = unaryExpr();
		while (t.kind() == TIMES || t.kind() == DIV || t.kind() == MOD) {
			IToken opToken = t;
			consume(t.kind());
			Expr right = unaryExpr();
			left = new BinaryExpr(opToken, left, opToken, right);
		}
		return left;
	}
	private Expr unaryExpr() throws PLCCompilerException {
		IToken opToken = t;
		//Expr expr = null;
		//Expr left = PostfixExpr();
		if (t.kind() == BANG || t.kind() == MINUS || t.kind() == RES_width) {
			consume(t.kind()); // Consume the unary operator
			Expr operand = PostfixExpr();// Parse the operand, which could be a NumLitExpr
			return new UnaryExpr(opToken, opToken, operand);
		} else {
			//expr = PostfixExpr();
			 return PostfixExpr();
		}
		//return left;
	}
	private Expr PostfixExpr() throws PLCCompilerException {

		Expr left = primary();
		PixelSelector pixel = null;
		ChannelSelector selector = null;
		IToken firstToken = t;
		//t = lexer.next();
		IToken op = t;
		switch (op.kind()) {
			case LSQUARE -> {
				pixel = pixelSelector();
				if (t.kind() == Kind.COLON) {
					selector = channelSelector();
					consume(t.kind());
				}
				return new PostfixExpr(firstToken, left, pixel, selector);
			}
			case COLON -> {
				selector = channelSelector();
				return new PostfixExpr(firstToken, left, pixel, selector);
			}
		}
		return left;
	}

	private ChannelSelector channelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(COLON);
		IToken colorToken = t;
		if (colorToken.kind() == RES_red) {
			consume(RES_red);
		} else if (colorToken.kind() == RES_green) {
			consume(RES_green);
		} else if (colorToken.kind() == RES_blue) {
			consume(RES_blue);
		} else {
			throw new SyntaxException("Expected color selector after ':' but found " + colorToken.text());
		}
		return new ChannelSelector(firstToken, colorToken);
	}
	private PixelSelector pixelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(LSQUARE);
		Expr xExpr = expr(); // Parse the x
		consume(COMMA); // Consume the comma
		Expr yExpr = expr(); // Parse the y
		consume(RSQUARE); // Consume the RSQUARE
		return new PixelSelector(firstToken, xExpr, yExpr);
	}
	private Expr primary() throws PLCCompilerException {
		IToken firstToken = t;
		Expr expr;
		switch (firstToken.kind()) {
			case STRING_LIT:
				expr = new StringLitExpr(t);
				consume(STRING_LIT);
				return expr;

			case NUM_LIT:
				expr = new NumLitExpr(t);
				consume(NUM_LIT);
				return expr;

			case BOOLEAN_LIT:
				expr = new BooleanLitExpr(t);
				consume(BOOLEAN_LIT);
				return expr;

			case CONST:
				expr = new ConstExpr(t);
				consume(CONST);
				return expr;

			case IDENT:
				expr = new IdentExpr(t);
				consume(IDENT);
				return expr;
			case MINUS:
				return unaryExpr();

			case LPAREN:
				//ChannelSelector channel = null;
				consume(LPAREN);
				Expr innerParen = expr();
				//consume(RPAREN);
				if (t.kind() != Kind.RPAREN){
					throw new SyntaxException("Missing Right Paren");
				}
				consume(RPAREN);
				//innerParen = PostfixExpr(innerParen);
				return innerParen;
				//return expr = new IdentExpr(t);
				//return PostfixExpr(innerParen);

			case LSQUARE:
				return expandedPixelExpr();
				//return handleBracketedExprs();

			default:
				throw new SyntaxException("Syntax Error: Unexpected token " + firstToken.text());
		}
	}

	private Expr expandedPixelExpr() throws PLCCompilerException {
			IToken firstToken = t;
			consume(LSQUARE);
			Expr xExpr = expr();
			consume(IDENT);
			consume(COMMA);
			Expr yExpr = expr();
			consume(IDENT);
			consume(COMMA);
			Expr zExpr = expr();
			consume(IDENT);
			consume(RSQUARE);
			return new ExpandedPixelExpr(firstToken, xExpr, yExpr, zExpr); //
	}

	private void consume(Kind expected) throws LexicalException {
		if (t.kind() == expected) {
			t = lexer.next();
		} else {
			//throw new SyntaxException("Syntax Error" + t.kind());
		}
	}
}



