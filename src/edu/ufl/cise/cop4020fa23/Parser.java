/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the fall semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */
/*
Program::= Type IDENT ( ParamList ) Block
Block ::= <: (Declaration ; | Statement ;)* :>
ParamList ::= ε | NameDef ( , NameDef ) *
NameDef ::= Type IDENT | Type Dimension IDENT
Type ::= image | pixel | int | string | void | boolean
Declaration::= NameDef | NameDef = Expr
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
PrimaryExpr ::=STRING_LIT | NUM_LIT | IDENT | ( Expr ) | CONST | BOOLEAN_LIT |
 ExpandedPixelExpr
ChannelSelector ::= : red | : green | : blue
PixelSelector ::= [ Expr , Expr ]
ExpandedPixelExpr ::= [ Expr , Expr , Expr ]
Dimension ::= [ Expr , Expr ]
LValue ::= IDENT (PixelSelectorIn | ε ) (ChannelSelector | ε )
Statement::=
LValue = Expr |
 write Expr |
 do GuardedBlock [] GuardedBlock* od |
 if GuardedBlock [] GuardedBlock* if |
^ Expr |
 BlockStatement |
GuardedBlock := Expr -> Block
BlockStatement ::= Block
 */

package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser implements IParser {

	final ILexer lexer;
	private IToken t;
	private ExpressionParser expressionParser;

	public Parser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
		AST e = program();
		if (t.kind() != Kind.EOF) { // Check if the current token is the end of file
			throw new SyntaxException("Unexpected text after end of program: ");
		}
		return e;
	}

	private AST program() throws PLCCompilerException {
		IToken firstToken = t; // Save first token
		IToken typeToken = type(); // Match 'void' type for the program
		IToken nameToken = t;
		consume(Kind.IDENT);
		consume(Kind.LPAREN); // Match '('
		List<NameDef> params = paramList(); // Parse parameter list
		consume(Kind.RPAREN); // Match ')'
		Block block = block(); // Parse the block
		return new Program(firstToken, typeToken, nameToken, params, block);
	}

	private List<NameDef> paramList() throws PLCCompilerException {
		List<NameDef> params = new ArrayList<>();
		while (t.kind() != Kind.RPAREN) { // Continue until we reach the end of the parameter list
			params.add(nameDef());
			if (t.kind() == Kind.COMMA) {
				consume(Kind.COMMA); // Consume the comma and move to the next parameter
			}
		}
		return params;
	}

	private Block block() throws PLCCompilerException {
		consume(Kind.BLOCK_OPEN); // Match the opening of a block '<:'
		List<Block.BlockElem> elems = new ArrayList<>();
		while (t.kind() != Kind.BLOCK_CLOSE && t.kind() != Kind.RES_fi) {
			elems.add(blockElement());
			if (t.kind() == Kind.SEMI) {
				consume(Kind.SEMI);
			} else if (t.kind() == Kind.BLOCK_CLOSE) {
				// If it's neither a semicolon nor BLOCK_CLOSE, it's a syntax error
				throw new SyntaxException("Expected semicolon at the end of statement");
			}

//			else if (t.kind() == Kind.BLOCK_CLOSE){
//				consume(Kind.SEMI);
			//}
		}

		consume(Kind.BLOCK_CLOSE); //Closing blokc
		return new Block(t, elems); // Block ASt empty list
	}

	private Block.BlockElem blockElement() throws PLCCompilerException {
		if (t.kind() == Kind.BLOCK_OPEN){
			//consume(Kind.BLOCK_OPEN);
			if (t.kind() == Kind.BLOCK_CLOSE) {
				// Parse an empty statement block
				return parseEmptyStatementBlock();
			} else {
				Block newBlock = block();
				return new StatementBlock(t, newBlock);
			}
		}
		if (t.kind() == Kind.RES_write) {
			// Parse a write statement
			return parseWriteStatement();
		} else if (isTypeToken(t)) {
			return parseDeclaration();
		}
		else if (t.kind() == Kind.IDENT) {
			return parseAssignmentStatement();
		}
		else if (t.kind() == Kind.RES_do){
			return parseDoStatement();
		}
		else if (t.kind() == Kind.RETURN){
			return parseReturnStatement();
		}
		else if (t.kind() == Kind.RES_if) {
			return parseIfStatement();
		}

		else {

			throw new SyntaxException("Unknown block element type: " + t.kind());
		}
	}
	private StatementBlock parseEmptyStatementBlock() throws PLCCompilerException {

		consume(Kind.BLOCK_CLOSE); // Consume the closing block
		Block block = new Block(t, new ArrayList<>()); // Create an empty Block
		return new StatementBlock(t, block); // Return a new StatementBlock with the empty Block
	}
	private Expr parseConditionalExpr() throws PLCCompilerException {
		IToken firstToken = t; // Save the first token (QUESTION)
		consume(Kind.QUESTION); // Consume Question
		Expr guardExpr = expression(); // Parse guard expression
		consume(Kind.RARROW);
		Expr trueExpr = expression(); // Parse true case expression
		consume(Kind.COMMA); // Consume da comma
		Expr falseExpr = expression(); // Parse false case expression
		return new ConditionalExpr(firstToken, guardExpr, trueExpr, falseExpr);
	}
	private Block.BlockElem parseAssignmentStatement() throws PLCCompilerException {
		LValue lvalue = parseLValue(); // Parse LHS
		consume(Kind.ASSIGN); // Consume the assignment operator
		Expr expr = expression(); // Parse RHS
		return new AssignmentStatement(t, lvalue, expr);
	}
	private ChannelSelector parseChannelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(Kind.COLON);
		IToken colorToken = t;
		switch (colorToken.kind()) {
			case RES_red:
				consume(Kind.RES_red);
				break;
			case RES_green:
				consume(Kind.RES_green);
				break;
			case RES_blue:
				consume(Kind.RES_blue);
				break;
			default:
				throw new SyntaxException("Expected color selector after ':' but found " + colorToken.text());
		}
		return new ChannelSelector(firstToken, colorToken);
	}

	private PixelSelector parsePixelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		consume(Kind.LSQUARE);
		Expr xExpr = expression(); // Parse the x
		consume(Kind.COMMA); // Consume the comma
		Expr yExpr = expression(); // Parse the y
		consume(Kind.RSQUARE); // Consume the RSQUARE
		return new PixelSelector(firstToken, xExpr, yExpr);
	}

	private Block.BlockElem parseWriteStatement() throws PLCCompilerException {
		consume(Kind.RES_write);
		Expr expr = expression();
		return new WriteStatement(t, expr);
	}

	private Block.BlockElem parseDeclaration() throws PLCCompilerException {
		NameDef nameDef = nameDef(); // Name def parse
		Expr initializer = null;
		if (t.kind() == Kind.ASSIGN) {
			consume(Kind.ASSIGN);
			initializer = expression(); // Parse the initializer expr
		}
		return new Declaration(t, nameDef, initializer);
	}

	private boolean isTypeToken(IToken token) {
		// Check if the token is a type keyword
		return token.kind() == Kind.RES_int || token.kind() == Kind.RES_boolean ||
				token.kind() == Kind.RES_image || token.kind() == Kind.RES_pixel ||
				token.kind() == Kind.RES_string || token.kind() == Kind.RES_void;
	}
	private LValue parseLValue() throws PLCCompilerException {
		IToken nameToken = t;
		consume(Kind.IDENT); // Consume the identifier
		PixelSelector pixelSelector = null;
		ChannelSelector channelSelector = null;

		// If there's a pixel selector
		if (t.kind() == Kind.LSQUARE) {
			pixelSelector = parsePixelSelector();
		}
		// If there's a channel selector
		if (t.kind() == Kind.COLON) {
			channelSelector = parseChannelSelector();
		}
		//if (t.kind() == Kind.ASSIGN){

//		}

		return new LValue(nameToken, nameToken, pixelSelector, channelSelector);
	}
	private Expr multiplicativeExpression() throws PLCCompilerException {
		Expr expr = unaryExpression();
		while (t.kind() == Kind.TIMES || t.kind() == Kind.DIV || t.kind() == Kind.MOD) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = unaryExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}

	private Expr additiveExpression() throws PLCCompilerException {
		Expr expr = multiplicativeExpression();
		while (t.kind() == Kind.PLUS || t.kind() == Kind.MINUS) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = multiplicativeExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}

	private Expr comparisonExpression() throws PLCCompilerException {
		Expr expr = powExpression();
		while (isComparisonOperator(t.kind())) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = powExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}
	private boolean isComparisonOperator(Kind kind) {
		return Arrays.asList(Kind.LT, Kind.GT, Kind.EQ,
				Kind.LE, Kind.GE).contains(kind);
	}

	private Expr powExpression() throws PLCCompilerException {
		Expr expr = additiveExpression();
		while (t.kind() == Kind.EXP) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = additiveExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}

	private Expr logicalAndExpression() throws PLCCompilerException {
		Expr expr = comparisonExpression();
		while (t.kind() == Kind.AND || t.kind() == Kind.BITAND) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = comparisonExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}

	private Expr logicalOrExpression() throws PLCCompilerException {
		Expr expr = logicalAndExpression();
		while (t.kind() == Kind.OR || t.kind() == Kind.BITOR) {
			IToken opToken = t;
			consume(t.kind());
			Expr rightExpr = logicalAndExpression();
			expr = new BinaryExpr(opToken, expr, opToken, rightExpr);
		}
		return expr;
	}
	private Expr expression() throws PLCCompilerException {
		return logicalOrExpression();
		//return binaryExpression(); // Start with binary expressions,
	}


	private Expr unaryExpression() throws PLCCompilerException {
		//System.out.println("Current Token in unaryExpression: " + t.kind()); // Debugging print

		if (Arrays.asList(Kind.RES_width, Kind.RES_height, Kind.BANG, Kind.MINUS).contains(t.kind())) {
			IToken unaryOperator = t; // Store the unary operator token
			consume(t.kind()); // Consume the operator
			Expr operand = primaryExpr(); // Parse the operand using primaryExpr()
			return new UnaryExpr(unaryOperator, unaryOperator, operand); // Create a new UnaryExpr AST node
		} else {
			return postfixExpr(); // If it's not a unary expression, parse it as a primary expression
		}
	}

	private Expr postfixExpr() throws PLCCompilerException {

		Expr left = primaryExpr();
		PixelSelector pixel = null;
		ChannelSelector selector = null;
		IToken firstToken = t;
		//t = lexer.next();
		IToken op = t;
		switch (op.kind()) {
			case LSQUARE -> {
				pixel = parsePixelSelector();
				if (t.kind() == Kind.COLON) {
					selector = parseChannelSelector();
					//consume(t.kind());
				}
				return new PostfixExpr(firstToken, left, pixel, selector);
			}
			case COLON -> {
				selector = parseChannelSelector();
				return new PostfixExpr(firstToken, left, pixel, selector);
			}
		}
		return left;
	}

	private Expr primaryExpr() throws PLCCompilerException {
		//Expr primary;
		switch (t.kind()) {
			case IDENT:
				IToken identToken = t;
				consume(Kind.IDENT);
				return new IdentExpr(identToken);
			case QUESTION:
				return parseConditionalExpr();
			case NUM_LIT:
				IToken numToken = t;
				consume(Kind.NUM_LIT);
				return new NumLitExpr(numToken);
				//break;
			case CONST:
				IToken constToken = t;
				consume(Kind.CONST); // Consume the constant
				return new ConstExpr(constToken); // Create a ConstExpr AST node
			case LSQUARE:
				consume(Kind.LSQUARE); // Consume the '['
				Expr red = expression(); // Parse the red component
				consume(Kind.COMMA);
				Expr green = expression(); // Parse the green component
				consume(Kind.COMMA);
				Expr blue = expression(); // Parse the blue component
				consume(Kind.RSQUARE);
				return new ExpandedPixelExpr(t, red, green, blue); // Create an ExpandedPixelExpr AST node
			case BOOLEAN_LIT:
				IToken boolToken = t;
				consume(Kind.BOOLEAN_LIT);
				return new BooleanLitExpr(boolToken);
			case STRING_LIT:
				IToken stringToken = t;
				consume(Kind.STRING_LIT);
				return new StringLitExpr(stringToken);
			case MINUS:
				return  unaryExpression();
			case LPAREN:
				consume(Kind.LPAREN);
				Expr expr = expression(); // Parse the expression inside the parentheses
				consume(Kind.RPAREN);
				return expr;
			case BANG:
				return unaryExpression();
			case RES_width:
				return unaryExpression();
			case RES_height:
				return unaryExpression();

			default:
				throw new SyntaxException("Unsupported primary expression type" + t.kind());
		}
		//return primary;
	}
	private Block.BlockElem parseDoStatement() throws PLCCompilerException {
		consume(Kind.RES_do); // Consume 'do' keyword
		List<GuardedBlock> guardedBlocks = new ArrayList<>();
//First guarded block
		guardedBlocks.add(parseGuardedBlock());

		while (t.kind() == Kind.BOX) {
			consume(Kind.BOX); // Consume the '[]'
			guardedBlocks.add(parseGuardedBlock());
		}

		consume(Kind.RES_od);
		return new DoStatement(t, guardedBlocks);
	}

	private GuardedBlock parseGuardedBlock() throws PLCCompilerException {
		Expr guard = expression(); // Parse the guard expression
		consume(Kind.RARROW); // Consume '->' token
		Block block = block(); // Parse the block of statements
		return new GuardedBlock(t, guard, block);
	}

	private Block.BlockElem parseIfStatement() throws PLCCompilerException {
		consume(Kind.RES_if); // Consume if
		List<GuardedBlock> guardedBlocks = new ArrayList<>();

		// Parse the first guarded block
		guardedBlocks.add(parseGuardedBlock());

		while (t.kind() == Kind.BOX) {
			consume(Kind.BOX); // Consume the '[]'
			guardedBlocks.add(parseGuardedBlock());
		}

		consume(Kind.RES_fi); // Consume 'fi' keyword
		return new IfStatement(t, guardedBlocks);
	}

	private ReturnStatement parseReturnStatement() throws PLCCompilerException {
		IToken firstToken = t;
		consume(Kind.RETURN);
		Expr e = expression();
		return new ReturnStatement(firstToken, e);
	}

	private Block.BlockElem parseStatementBlock() throws PLCCompilerException {
		consume(Kind.BLOCK_OPEN); // Consume the '<:'
		Block block = new Block(t, new ArrayList<>()); // New empty Block
		consume(Kind.BLOCK_CLOSE); // Consume the ':>'
		return new StatementBlock(t, block);
	}

	private NameDef nameDef() throws PLCCompilerException {
		IToken firstToken = t; // Save the first token to pass to the NameDef constructor
		IToken typeToken = type();// Parse the type token
		//consume(typeToken.kind());
		Dimension dimension = null; // Initialize dimension

		if (t.kind() == Kind.LSQUARE) {
			dimension = dimension(); // Parse the dimension
		}
		//IToken identToken = consume(Kind.IDENT);
		IToken identToken = t;
		consume(Kind.IDENT);
		return new NameDef(firstToken, typeToken, dimension, identToken); // Construct the NameDef AST node
	}
	private Dimension dimension() throws PLCCompilerException {
		consume(Kind.LSQUARE);
		Expr widthExpr = expression(); // Parse width
		consume(Kind.COMMA);
		Expr heightExpr = expression(); // Parse height
		consume(Kind.RSQUARE);
		return new Dimension(t, widthExpr, heightExpr); // New dimension AST
	}
	private IToken type() throws PLCCompilerException {
		if (t.kind() == Kind.RES_int || t.kind() == Kind.RES_boolean || t.kind() == Kind.RES_image ||
				t.kind() == Kind.RES_pixel || t.kind() == Kind.RES_string || t.kind() == Kind.RES_void) {
			IToken typeToken = t;
			consume(t.kind()); // Consume the type token
			return typeToken;
		} else {
			throw new SyntaxException("Error on type() method" + t.kind());
		}
	}

	private void consume(Kind expectedKind) throws SyntaxException, LexicalException {
		if (t.kind() == expectedKind) {

			t = lexer.next(); // Consume the token and get the next one

		} else {
			throw new SyntaxException("Expected a different token" + t.kind());
		}
	}

}
