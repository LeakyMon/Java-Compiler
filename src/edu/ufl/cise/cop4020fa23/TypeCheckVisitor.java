package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.*;

public class TypeCheckVisitor implements ASTVisitor {

    SymbolTable st = new SymbolTable();
    Program root;
    String currentSide = "left";
    private boolean isInPixelSelectorContext = false;
    private boolean isInExpandedPixelExprContext = false;
    //private Map<String,Boolean> declaringVariables = new HashMap<>();
    private Map<String, Integer> declaringVariables = new HashMap<>();
    private int currentScopeLevel = 0;
    private boolean isInReturnContext = false;
    private Map<String, NameDef> syntheticVariables = new HashMap<>();


    private void check(boolean condition, AST node, String message) throws TypeCheckException {
        if (!condition){
            throw new TypeCheckException(node.firstToken.sourceLocation(), message);
        }
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        //currentSide left and ride for cases where variable is declared in different scope, but same variable is used in LHS of assignment Statement
        currentSide = "left";
        LValue lValue = assignmentStatement.getlValue();
        Type lValueType = (Type) lValue.visit(this, arg);
        PixelSelector p = lValue.getPixelSelector();

        currentSide = "right";
        Expr rValue = assignmentStatement.getE();

        Type rValueType = (Type) rValue.visit(this, arg);
        //If types match
        if (lValueType != rValueType) {
            throw new TypeCheckException("Type mismatch in assignment. Expected: " + lValueType + ", Found: " + rValueType);
        }
        currentSide = "left";
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type isBinary = null;
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Kind operator = binaryExpr.getOp().kind();

        switch (operator) {
            case PLUS:
            case MINUS:
            case TIMES:
            case DIV:
            case EXP:
                if (leftType == Type.INT && rightType == Type.INT) {
                    isBinary = Type.INT;
                }
                if (leftType == Type.STRING && rightType == Type.STRING) {
                    isBinary = Type.STRING;
                }
                break;
            case AND:
            case EQ: // Replace 'EQUALS' with the actual constant for equality check in your language
                if (leftType == rightType) {
                    isBinary = Type.BOOLEAN;
                    break;
                }
            case OR:
                if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) {
                    isBinary = Type.BOOLEAN;
                    break;
                }
            case LT:
                if (leftType == Type.INT && rightType == Type.INT) {
                    isBinary = Type.BOOLEAN;
                    break;
                }
            case LE:
                if (leftType == Type.INT && rightType == Type.INT) {
                    isBinary = Type.BOOLEAN;
                    break;
                }
            case GE:
                if (leftType == Type.INT && rightType == Type.INT) {
                    isBinary = Type.BOOLEAN;
                    break;
                }

            case GT:
                if (leftType == Type.INT && rightType == Type.INT) {
                isBinary = Type.BOOLEAN;
                break;
            }
        }
        binaryExpr.setType(isBinary);
        return isBinary;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        currentScopeLevel++;
        st.enterScope();
        //System.out.println("Entered scope");
        for (Block.BlockElem stmt : block.getElems()) {
            stmt.visit(this, arg);
        }
        st.leaveScope();
        //System.out.println("leaving block scope");
        currentScopeLevel--;
        return block;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        Block block = statementBlock.getBlock();
        // Visit the block
        block.visit(this, arg);

        return block;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {

       // System.out.println("Type for channel selection: " + arg);

        if (!(arg == Type.PIXEL || arg == Type.IMAGE)) {
            throw new TypeCheckException(channelSelector.firstToken.sourceLocation(),
                    "Channel selection can only be applied to pixel or image types.");
        }

        Kind channel = channelSelector.color();

        if (channel != Kind.RES_red && channel != Kind.RES_blue && channel != Kind.RES_green) {
            throw new TypeCheckException(channelSelector.firstToken.sourceLocation(),
                    "Invalid channel selected: " + channel);
        }

        Type resultType = Type.INT;

        return resultType;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {

        Expr condition = conditionalExpr.getGuardExpr();
        Type conditionType = (Type) condition.visit(this, arg);

        // Check if the condition is of BOOLEAN
        if (conditionType != Type.BOOLEAN) {
            throw new TypeCheckException("Condition in a conditional expression must be of BOOLEAN type.");
        }

       //Visit T and F parts
        Expr trueExpr = conditionalExpr.getTrueExpr();
        Expr falseExpr = conditionalExpr.getFalseExpr();

        Type trueType = (Type) trueExpr.visit(this, arg);
        Type falseType = (Type) falseExpr.visit(this, arg);

        // Check if the types of trueExpr and falseExpr match
        if (trueType != falseType) {
            throw new TypeCheckException("The types of the true and false parts of a conditional expression must match.");
        }

        conditionalExpr.setType(trueType);
        return trueType;

    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        String name = declaration.getNameDef().getName();

        if (st.isInCurrentScope(name)) {
            throw new TypeCheckException("Variable " + name + " already declared in this scope.");
        }


        NameDef nameDef = declaration.getNameDef();
        st.insert(nameDef.getName(), nameDef);
        st.addDeclaredVariable(nameDef.getName());
        //System.out.println("Inserted: " + nameDef.getName());
       // declaringVariables.put(name, true);
        declaringVariables.put(name, currentScopeLevel);

        // Process initializer
        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            Type initializerType = (Type) initializer.visit(this, arg);
            if (declaration.getNameDef().getType() == Type.IMAGE) {
                if (initializerType != Type.STRING && initializerType != Type.IMAGE) {
                    throw new TypeCheckException(declaration.firstToken.sourceLocation(),
                            "Type mismatch in initialization of " + name + ". Expected STRING or IMAGE.");
                }
            } else if (initializerType != declaration.getNameDef().getType()) {
                throw new TypeCheckException(declaration.firstToken.sourceLocation(),
                        "Type mismatch in initialization of " + name);
            }
        }

        declaringVariables.remove(name);

        // Process dimension
        Dimension dimension = declaration.getNameDef().getDimension();
        if (dimension != null) {
            dimension.visit(this, arg);
        }
        return null;
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {

        Type typeW = (Type) dimension.getWidth().visit(this,arg);
        check(typeW == Type.INT, dimension, "Image width must be int");
        Type typeH = (Type) dimension.getHeight().visit(this,arg);
        check(typeH == Type.INT, dimension, "Image height must be int");
        return dimension;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            guardedBlock.visit(this, arg);
        }
        return null;
    }
    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        isInPixelSelectorContext = true;
        //x and y pixels
        Expr xExpr = pixelSelector.xExpr();

        Expr yExpr = pixelSelector.yExpr();

        Type xType = (Type) xExpr.visit(this, arg);
       // System.out.println("xType " + xType.toString());
        if (xType != Type.INT) {
            throw new TypeCheckException(pixelSelector.firstToken.sourceLocation(), "X dimension must be of type INT.");
        }

        // Visit and check the type of y dimension
        Type yType = (Type) yExpr.visit(this, arg);
        if (yType != Type.INT) {
            throw new TypeCheckException(pixelSelector.firstToken.sourceLocation(), "Y dimension must be of type INT.");
        }

        isInPixelSelectorContext = false;
        syntheticVariables.clear();
        return Type.PIXEL;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        isInExpandedPixelExprContext = true;

        Expr red = expandedPixelExpr.getRed();
        Expr green = expandedPixelExpr.getBlue();
        Expr blue = expandedPixelExpr.getGreen();

        Type redType = (Type) red.visit(this, arg);
        Type greenType = (Type) green.visit(this, arg);
        Type blueType = (Type) blue.visit(this, arg);

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new TypeCheckException(expandedPixelExpr.firstToken.sourceLocation(),
                    "Red, green, and blue components of a pixel must be of type INT.");
        }
        isInExpandedPixelExprContext = false;
        syntheticVariables.clear();
        return Type.PIXEL;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Expr condition = guardedBlock.getGuard();
        Type conditionType = (Type) condition.visit(this, arg);
        if (conditionType != Type.BOOLEAN) {
            throw new TypeCheckException("Condition in guarded block must be of BOOLEAN type.");
        }

        Block block = guardedBlock.getBlock();
        block.visit(this, arg);

        return null;

    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        String name = identExpr.getName();
       // boolean isSyntheticContext = isInPixelSelectorContext || isInExpandedPixelExprContext;
       // System.out.println("Visiting IdentExpr: " + name);

        NameDef nameDef = st.lookup(name);

        if (nameDef == null) {
            if (currentSide == "left") {
                //If its a pixelSelector or an expandedpixelexpr
                if ((isInPixelSelectorContext || isInExpandedPixelExprContext)) {
                    //syntheticVariables.put(name, new SyntheticNameDef(name));
                    identExpr.setType(Type.INT);
                    st.removeVar(name);
                    return Type.INT;
                } else {
                    throw new TypeCheckException("Undeclared or out of scope variable: " + name);
                }
            } else {
                if ((isInPixelSelectorContext || isInExpandedPixelExprContext) && !st.isVariableEverDeclared(name)) {
                    syntheticVariables.put(name, new SyntheticNameDef(name));
                    identExpr.setType(Type.INT);
                    return Type.INT;
                } else {
                    throw new TypeCheckException("Undeclared or out of scope variable: " + name);
                }
            }
        }
        //Self assigned variable
        System.out.println("current level" + currentScopeLevel);



        if (declaringVariables.containsKey(name) && declaringVariables.get(name) == currentScopeLevel) {

            if (st.isInCurrentScope(name) && st.isDeclaredInLowerScope(name)){
                throw new TypeCheckException("Variable used in initializer " + name);

            }
            else {
                System.out.println("Hey");
                throw new TypeCheckException("Variable '" + name + "' used in its initializer");
            }
        }

        System.out.println("passing");
        Type idType = nameDef.getType();
        identExpr.setType(idType);
        return idType;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {

        List<GuardedBlock> guardedBlocks = ifStatement.getGuardedBlocks();


        for (GuardedBlock guardedBlock : guardedBlocks) {

            Expr guard = guardedBlock.getGuard();
            Type guardType = (Type) guard.visit(this, arg);

            check(guardType == Type.BOOLEAN, guard, "The condition of a GuardedBlock must be of BOOLEAN type.");

            Block block = guardedBlock.getBlock();
            block.visit(this, arg);
        }


        return null;

    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
       String name = lValue.getName();
        NameDef nameDef = st.lookup(name);

        if (nameDef == null) {
            throw new TypeCheckException("Undefined identifier in LValue: " + name);
        }

        Type idType = nameDef.getType();

        // If pixelSelector
        if (lValue.getPixelSelector() != null) {
            // Make sure its type image
            if (idType != Type.IMAGE) {
                throw new TypeCheckException("Pixel selector can only be applied to images.");
            }
            return lValue.getPixelSelector().visit(this, idType);
        }
        return idType;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        Dimension d = nameDef.getDimension();
        Type currType = nameDef.getType();

        if (d!=null){
            d.visit(this, arg);
            check(currType == Type.IMAGE, nameDef, "Unexpected dimension");

        }
        else check((currType == Type.BOOLEAN || currType == Type.INT ||  currType == Type.STRING || currType== Type.IMAGE || currType == Type.PIXEL ), nameDef,"Invalid type");

        if (st.isInCurrentScope(nameDef.getName())) {
            throw new TypeCheckException("Exact same name declaration");
        }
        else {
            st.insert(nameDef.getName(), nameDef);
        }
        return currType;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.INT;
        numLitExpr.setType(type);
       // System.out.println("visitNumLitExpr: Setting type INT for " + numLitExpr.toString());
        return type;
    }



    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Expr primaryExpr = postfixExpr.primary();
        Type primaryType = (Type) primaryExpr.visit(this, arg);
      //  System.out.println("PostFixExpr " + primaryExpr.toString());
        // Check for PixelSelector
        if (postfixExpr.pixel() != null) {
            primaryType = (Type) postfixExpr.pixel().visit(this, primaryType);
        }

        // If a ChannelSelector is present
        if (postfixExpr.channel() != null) {
            Type channelType = (Type) postfixExpr.channel().visit(this, primaryType);
            postfixExpr.setType(channelType);  // Set the type as the result of the channel selection
            return channelType;
        }

        // If no ChannelSelector is present
        postfixExpr.setType(primaryType);
        return primaryType;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        st.enterScope();
        //System.out.println("entering program scope");
        List<NameDef> params = program.getParams();
        for (NameDef param : params){
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        st.leaveScope();
        //System.out.println("left program scope");
        return type;

    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Expr returnValue = returnStatement.getE(); // Get the return expression
       // System.out.println(returnValue);
        if (returnValue == null) {
            if (root != null && root.getType() != Type.VOID) {
                throw new TypeCheckException(returnStatement.firstToken.sourceLocation(),
                        "Missing return value. Expected: " + root.getType());
            }

            return Type.VOID;
        }
        isInReturnContext = true;

        Type returnType = (Type) returnValue.visit(this, arg);
        isInReturnContext = false;
        if (returnValue instanceof IdentExpr && ((IdentExpr)returnValue).isSynthetic()){
            throw new TypeCheckException(returnStatement.firstToken.sourceLocation(),
                    "Cannot return a synthetic variable: " + ((IdentExpr)returnValue).getName());
        }
        if (root != null && root.getType() != returnType) {
            throw new TypeCheckException(returnStatement.firstToken.sourceLocation(),
                    "Return type mismatch. Expected: " + root.getType() + ", Found: " + returnType);
        }

        //System.out.println(returnType);

        return returnType;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        Type operandType = (Type) unaryExpr.getExpr().visit(this, arg);
        Kind op = unaryExpr.getOp();

        switch (op) {
            case MINUS:
                if (operandType == Type.INT) {
                    unaryExpr.setType(Type.INT);
                    return Type.INT;
                }
                throw new TypeCheckException("Unary minus operator is only applicable to INT type.");

            case RES_width:
            case RES_height:
                if (operandType == Type.IMAGE) {
                    unaryExpr.setType(Type.INT);
                    return Type.INT;
                }
                throw new TypeCheckException("Width/Height unary operators are only applicable to IMAGE type.");

            case BANG:
                if (operandType == Type.BOOLEAN) {
                    unaryExpr.setType(Type.BOOLEAN);
                    return Type.BOOLEAN;
                }
                throw new TypeCheckException("Logical NOT operator is only applicable to BOOLEAN type.");

            default:
                throw new TypeCheckException("Unsupported unary operator: " + op);
        }
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        Type isConst = null;
       char checker = constExpr.getName().toCharArray()[0];
       if (checker =='Z'){
           constExpr.setType(Type.INT);
            isConst = Type.INT;
       }
       else {
           constExpr.setType(Type.PIXEL);
           isConst = Type.PIXEL;
       }
        return isConst;
    }

}
