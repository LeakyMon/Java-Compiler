package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.awt.*;
import java.sql.SQLOutput;
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
//checks to see if its alone or a full one
    private boolean isSingleColorComponentContext = false;
    private boolean isIMG = false;


    private void check(boolean condition, AST node, String message) throws TypeCheckException {
        if (!condition){
            throw new TypeCheckException(node.firstToken.sourceLocation(), message);
        }
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {

        String name = assignmentStatement.getlValue().getName();

        NameDef nameDef = checkScopes(name);
        //st.insert(nameDef.getName(), nameDef);
        //S//tring scopedName = generateScopedName(name);
        // nameDef.setJavaName(scopedName); // Assuming setJavaName changes the name in NameDef
        //nameDef.setJavaName(scopedName);


        currentSide = "left";

        assignmentStatement.getlValue().setNameDef(nameDef);


        LValue lValue = assignmentStatement.getlValue();
        Type lValueType = (Type) lValue.visit(this, arg);
        //System.out.println(lValueType.toString());
        if (lValueType == Type.IMAGE){
            isIMG = true;
        }
        PixelSelector p = lValue.getPixelSelector();
        //System.out.println("Swtiching to right");
        currentSide = "right";
        Expr rValue = assignmentStatement.getE();

        Type rValueType = (Type) rValue.visit(this, arg);
        isIMG = false;
        //If types match
        if (lValueType == Type.PIXEL && rValueType == Type.INT) {
            lValue.setType(Type.PIXEL);
            rValue.setType(Type.INT);

        } else if (lValueType != rValueType) {
           // System.out.println(lValueType.toString() + " " +rValueType.toString());

            throw new TypeCheckException("Type mismatch in assignment. Expected: " + lValueType + ", Found: " + rValueType);
        }
        currentSide = "left";
        Type idType = nameDef.getType();
        //setting the name Def so that we can link them
        return idType;
    }

    public NameDef checkScopes(String name) throws TypeCheckException{
        System.out.println("Checking Scope...");
        int temp = currentScopeLevel;
        NameDef nameDef = null;
        boolean foundInAnyScope = false;

        while (temp >= 0) {
            String tempName = name + "_" + temp;
            nameDef = st.lookupInCurrScope(tempName, temp);
            if (nameDef != null) {
                // Variable found in the current or a higher scope
                foundInAnyScope = true;
                // If the variable is in the current scope or initialized in a higher scope, return it
                if (nameDef.isInitialized()) {
                    System.out.println("Found and Initialized");
                    return nameDef;
                }
                else if (currentScopeLevel >= temp && currentSide == "left"){
                    System.out.println("Found in higher or equal scope and the side is left");
                    nameDef.setInitialized(true);
                    nameDef.setJavaName(tempName);
                    return nameDef;
                }
                else if (nameDef.getType() == Type.IMAGE){
                    System.out.println("Found its an image");
                    nameDef.setInitialized(true);
                    return nameDef;
                }



            }
            temp--;
        }

        if (!foundInAnyScope) {
            if (isInPixelSelectorContext || isInExpandedPixelExprContext){
                return nameDef;
            }

            throw new TypeCheckException("Variable " + name + " not declared in the current or any outer scopes");
        }


        return nameDef;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        //System.out.println("visitng binary in typecheck");
        Type isBinary = null;
        isSingleColorComponentContext = true; // If the binary operation is on single color components
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        //System.out.println(leftType.toString());
        currentSide="right";
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        //System.out.println(rightType.toString());
        Kind operator = binaryExpr.getOp().kind();
        isSingleColorComponentContext = false; // Reset after visiting

        if (leftType == Type.IMAGE && rightType == Type.INT && operator == Kind.TIMES) {
            // The result of multiplying an image by a scalar is an image
            binaryExpr.setType(Type.IMAGE);
            return Type.IMAGE;
        }


        if (leftType == null || rightType == null) {
            throw new PLCCompilerException("Type of one of the binary expression sides is null");
        }


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
                if (leftType == Type.PIXEL && rightType == Type.PIXEL) {
                    // Assuming the operation is valid, the result type is PIXEL
                    isBinary = Type.PIXEL;
                }
                if (leftType == Type.PIXEL && rightType == Type.INT) {
                    // Assuming the operation is valid, the result type is PIXEL
                    isBinary = Type.PIXEL;
                }
                if (leftType == Type.IMAGE && rightType == Type.IMAGE){
                    isBinary = Type.IMAGE;
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
        currentSide = "left";
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
        System.out.println("Visited Decl");
        String name = declaration.getNameDef().getName();
        NameDef nameDef = declaration.getNameDef();

        String scopedName = generateScopedName(name);
       // nameDef.setJavaName(scopedName); // Assuming setJavaName changes the name in NameDef
        nameDef.setJavaName(scopedName);
        st.insert(scopedName, nameDef);
        System.out.println("Inserted " + scopedName);
        st.addDeclaredVariable(nameDef.getName());
        declaringVariables.put(name, currentScopeLevel);
        // Process initializer

        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            Type initializerType = (Type) initializer.visit(this, arg);

            if (declaration.getNameDef().getType() == Type.IMAGE) {
                if (initializerType != Type.STRING && initializerType != Type.IMAGE) {
                    throw new TypeCheckException(declaration.firstToken.sourceLocation(),
                            "Type mismatch in initialization of " + name + ". Expected STRING || IMAGE.");
                }
            } else if (initializerType != declaration.getNameDef().getType()) {
                throw new TypeCheckException(declaration.firstToken.sourceLocation(),
                        "Type mismatch in initialization of " + name);
            }
            nameDef.setInitialized(true);
        }

        declaringVariables.remove(name);

        // Process dimension
        Dimension dimension = declaration.getNameDef().getDimension();
        if (dimension != null) {
            dimension.visit(this, arg);
        }
        return declaration;
    }

    private String generateScopedName(String baseName) {
        // This could be more sophisticated based on your scope handling
        return baseName + "_" + currentScopeLevel;
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
        //syntheticVariables.clear();
        return Type.PIXEL;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        //System.out.println("TC visitn expanded ");
        isInExpandedPixelExprContext = true;
        //System.out.println(expandedPixelExpr.getType().toString());
        //System.out.println(expandedPixelExpr.toString());
        Expr red = expandedPixelExpr.getRed();
        Expr green = expandedPixelExpr.getBlue();
        Expr blue = expandedPixelExpr.getGreen();

        Type redType = (Type) red.visit(this, arg);
        Type greenType = (Type) green.visit(this, arg);
        Type blueType = (Type) blue.visit(this, arg);

        //System.out.println(redType.toString());


        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new TypeCheckException(expandedPixelExpr.firstToken.sourceLocation(),
                    "Red, green, and blue components of a pixel must be of type INT.");
        }

        isInExpandedPixelExprContext = false;
        //syntheticVariables.clear();
        if (isSingleColorComponentContext) {
            return Type.INT;  // Single color component context
        } else {
            //return Type.IMAGE;
            if (isIMG){
                return Type.IMAGE;
            }
            return Type.PIXEL;  // Full pixel context
        }
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
        NameDef nameDef = checkScopes(name);
        System.out.println(name);

        if (nameDef == null) {
            if (currentSide == "left") {
                //If its a pixelSelector or an expandedpixelexpr
                if ((isInPixelSelectorContext || isInExpandedPixelExprContext)) {
                    syntheticVariables.put(name, new SyntheticNameDef(name));
                    identExpr.setType(Type.INT);
                    st.removeVar(name);
                    return Type.INT;
                } else {
                    throw new TypeCheckException("first Undeclared or out of scope variable: " + name);
                }
            } else {
                if ((isInPixelSelectorContext || isInExpandedPixelExprContext) && !st.isVariableEverDeclared(name)) {
                    syntheticVariables.put(name, new SyntheticNameDef(name));
                    identExpr.setType(Type.INT);
                    return Type.INT;
                } else {
                    if (!isSyntheticVariable(name)){
                        throw new TypeCheckException("secondary Undeclared or out of scope variable: " + name);
                    }
                    else {
                    }
                }
            }
        }
        //Self assigned variable
        //System.out.println("current level " + currentScopeLevel);
        if (declaringVariables.containsKey(name) && name == nameDef.getJavaName()) {
            int declaredScopeLevel = declaringVariables.get(name);
            if (declaredScopeLevel == currentScopeLevel) {
                if (st.isInCurrentScope(name) && st.isDeclaredInLowerScope(name)) {
                    //variable redeclared in different scope
                } else {
                    // This is a genuine case where a variable is used in its initializer in the same scope.
                    throw new TypeCheckException("Variable '" + name + "' used in its initializer");
                }
            }
        }
        Type idType = nameDef.getType();
        //setting the name Def so that we can link them
        identExpr.setNameDef(nameDef);
        identExpr.setType(idType);
        return idType;
    }


    public boolean isSyntheticVariable(String name){
        st.setCurrentScope(currentScopeLevel);
        //    private Map<String, NameDef> syntheticVariables = new HashMap<>();
        if (syntheticVariables.containsKey(name) && st.isInCurrentScope(name)){
            return true;
        }
        else {
            return false;
        }
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
        NameDef nameDef = lValue.getNameDef();

        //if (nameDef == null) {
          //  throw new TypeCheckException("Undefined identifier in LValue: " + name);
       // }

        Type idType = nameDef.getType();

        // If pixelSelector
        if (lValue.getPixelSelector() != null) {
            // Make sure its type image
            if (idType != Type.IMAGE) {
                throw new TypeCheckException("Pixel selector can only be applied to images.");
            }
            return lValue.getPixelSelector().visit(this, idType);
        }
        // Link LValue to its NameDef potentially might need to add above
        lValue.setNameDef(nameDef);
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


        String newName = nameDef.getName();
        newName += "_" + currentScopeLevel;
        System.out.println(currentScopeLevel);

        if (st.isInCurrentScope(newName)){
            throw new TypeCheckException("Exact same name declaration");
        }
        else {

            st.insert(newName, nameDef);
            System.out.println(newName);
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
            // If there's also a PixelSelector and the primary type is an image, the result should be an INT
            if (postfixExpr.pixel() != null && primaryType == Type.IMAGE) {
                postfixExpr.setType(Type.INT); // Extracting a color channel from a specific pixel of an image
                return Type.INT;
            } else {
                Type channelType = (Type) postfixExpr.channel().visit(this, primaryType);
                postfixExpr.setType(channelType);  // Set the type as the result of the channel selection
                return channelType;
            }
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
       // System.out.println("Hello");
       // System.out.println(returnStatement.toString());
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

        Type channelExtractionType = getTypeOfChannelExtraction(returnValue);
        if (channelExtractionType != null) {
            returnType = channelExtractionType; // Set return type based on the channel extraction
        }


        //System.out.println(root.getName());
        String tempReturnType = root.getName();
        //System.out.println(tempReturnType.toString());
        switch (tempReturnType){
            case "PixelSum":
                return Type.PIXEL;
            case "Pixel":
                return Type.PIXEL;
            case "colors":
                return Type.IMAGE;
            default:
                System.out.println("No type found Expected: " + root.getName());
        }
        if (root != null && root.getType() != returnType) {
            System.out.println();
            throw new TypeCheckException(returnStatement.firstToken.sourceLocation(),
                    "Return type mismatch. Expected: " + root.getType() + ", Found: " + returnType);
        }

       // System.out.println(returnType);

        return returnType;
    }

    private Type getTypeOfChannelExtraction(Expr expr) throws PLCCompilerException {
        if (expr instanceof PostfixExpr) {
            PostfixExpr postfixExpr = (PostfixExpr) expr;

            // Check if the postfix expression has a channel selector
            if (postfixExpr.channel() != null) {
                Expr primaryExpr = postfixExpr.primary();

                // Check the type of the primary expression
                Type primaryType = (Type) primaryExpr.visit(this, null); // Visit to get the type

                // Check for PixelSelector to determine the context of the channel extraction
                if (postfixExpr.pixel() != null) {
                    // If we are extracting a channel from a specific pixel in an image
                    if (primaryType == Type.IMAGE) {
                        return Type.INT; // The result is an int (the channel value of the specific pixel)
                    }
                } else {
                    // If the channel extraction is from the whole image
                    if (primaryType == Type.IMAGE) {
                        return Type.IMAGE; // The result is an image (a new image with the extracted channel)
                    }
                }

                if (primaryType == Type.PIXEL) {
                    return Type.INT; // Channel extraction from a pixel returns an int
                }

                // Handle other types or throw an exception if needed
                throw new PLCCompilerException("Invalid type for channel extraction");
            }
        }
        return null; // Not a channel extraction
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
                throw new TypeCheckException("Width/Height unary operators are only used to IMAGE type.");

            case BANG:
                if (operandType == Type.BOOLEAN) {
                    unaryExpr.setType(Type.BOOLEAN);
                    return Type.BOOLEAN;
                }
                throw new TypeCheckException("Logical NOT operator is only used for BOOLEAN type.");

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
           switch (constExpr.getName()) {
               //Maybe type image?? come back *******
               case "RED":
                   isConst = Type.PIXEL;
                   break;
               case "GREEN":
                   isConst = Type.PIXEL;
                   break;
               case "BLUE":
                   isConst = Type.PIXEL;
                   break;
               case "PINK":
                   isConst = Type.PIXEL;
                   break;
               case "MAGENTA":
                   isConst = Type.PIXEL;
                   break;
               case "CYAN":
                   isConst = Type.PIXEL;

                   break;
               case "BLACK":
                   isConst = Type.PIXEL;

                   break;
               case "LIGHT_GRAY":
                   isConst = Type.PIXEL;

                   break;
               case "YELLOW":
                   isConst = Type.PIXEL;

                   break;
               case "ORANGE":
                   isConst = Type.PIXEL;

                   break;
               case "WHITE":
                   isConst = Type.PIXEL;

                   break;
               case "GRAY":
                   isConst = Type.PIXEL;

                   break;
               case "DARK_GRAY":
                   isConst = Type.PIXEL;

                   break;

           }
       }
        System.out.println("Const name: ");
        System.out.println(constExpr.getName());

        return isConst;
    }

}
