package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;
import edu.ufl.cise.cop4020fa23.DynamicJavaCompileAndExecute.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;
import edu.ufl.cise.cop4020fa23.runtime.ImageOps;


import static edu.ufl.cise.cop4020fa23.Kind.EQ;
import static edu.ufl.cise.cop4020fa23.Kind.RES_int;


public class CodeGenVisitor implements ASTVisitor {

    private StringBuilder generatedCode = new StringBuilder();
    private HashMap<String, Integer> scope = new HashMap<>();
    private int currentScopeLevel = 0;
    private boolean alreadyUsed = false;


    public String getGeneratedCode() {
        return generatedCode.toString();
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        System.out.println("visit assignment ");
        LValue lValue = assignmentStatement.getlValue();
        Expr rValue = assignmentStatement.getE();
        System.out.println("Rvalue type");
        System.out.println(rValue.getType());
        //NameDef type = pixel

        Type lValueType = lValue.getType(); // Assuming you have a way to get the type of LValue
        Type rValueType = rValue.getType(); // Assuming you have a way to get the type of the expression
        //System.out.println(lValueType);
        NameDef lnameDef = lValue.getNameDef();

        if (lnameDef == null) {

            if (lValue.getPixelSelector() != null) {
                PixelSelector lPixelSelector = lValue.getPixelSelector();
                Expr xExpr = lPixelSelector.xExpr();
                Expr rExpr = lPixelSelector.yExpr();

            }
        }
        else {
            lValueType = lnameDef.getType();
            // rValueType = rnameDef.getType();
            System.out.println(lValueType);
            System.out.println(rValueType);
        }

        if (lValueType == Type.PIXEL && rValueType == Type.INT && lValue.getChannelSelector() == null) {
            String varName = lValue.getName();
            generatedCode.append(varName).append(" = PixelOps.pack(");
            rValue.visit(this, arg); // Visit the integer expression
            generatedCode.append(", ");
            rValue.visit(this, arg); // Repeat for green
            generatedCode.append(", ");
            rValue.visit(this, arg); // Repeat for blue
            generatedCode.append(");");
        }
        else if (lValue.getChannelSelector() != null){
            ChannelSelector channelSelector = lValue.getChannelSelector();
            Kind channel = channelSelector.color();
            String varName = lValue.getName(); // Pixel varname

            // Build Assignment Statement
            generatedCode.append(varName).append(" = PixelOps.set");

            // Append the correct channel method
            switch (channel) {
                case RES_red:
                    generatedCode.append("Red");
                    break;
                case RES_green:
                    generatedCode.append("Green");
                    break;
                case RES_blue:
                    generatedCode.append("Blue");
                    break;
                default:
                    throw new PLCCompilerException("Unsupported channel: " + channel);
            }

            // Complete the assignment statement
            generatedCode.append("(").append(varName).append(", ");
            rValue.visit(this, arg); // Append the new value for channel
            generatedCode.append(");");

        }
        else if (lValueType == Type.IMAGE && rValue instanceof BinaryExpr) {
            System.out.println("here");
            BinaryExpr binaryExpr = (BinaryExpr) rValue;
            Kind operator = binaryExpr.getOp().kind();

            // Handling specific image operations
            if (operator == Kind.PLUS && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.IMAGE) {
                // Assuming PLUS is for image addition
                String varName = lValue.getName();
                generatedCode.append("ImageOps.copyInto((ImageOps.binaryImageImageOp(ImageOps.OP.PLUS, ");
               // generatedCode.append(varName).append(" = ImageOps.copyInto((ImageOps.binaryImageImageOp(ImageOps.OP.PLUS, ");
                binaryExpr.getLeftExpr().visit(this, arg);
                generatedCode.append(", ");
                binaryExpr.getRightExpr().visit(this, arg);
                generatedCode.append(")), ");
                generatedCode.append(varName);
                generatedCode.append(");");
            }
            else if (operator == Kind.TIMES && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.INT){
                System.out.println("We made it");
                generatedCode.append("ImageOps.copyInto((ImageOps.binaryImageScalarOp(ImageOps.OP.TIMES,");
                binaryExpr.getLeftExpr().visit(this, arg);
                generatedCode.append(", ");
                binaryExpr.getRightExpr().visit(this, arg);
                generatedCode.append("))");
                generatedCode.append(", ");
                binaryExpr.getLeftExpr().visit(this, arg);
                generatedCode.append(")");

            }
        }
        else if (lValueType == Type.IMAGE && rValue instanceof ExpandedPixelExpr) {
            ExpandedPixelExpr expandedPixelExpr = (ExpandedPixelExpr) rValue;
            System.out.println("Image + ExpandedPixelExpr");
           // ImageOps.setAllPixels(b$2,PixelOps.pack(0,255,0));
            generatedCode.append("ImageOps.setAllPixels(");
            lValue.visit(this,arg);
            generatedCode.append(",");
            generatedCode.append("PixelOps.pack(");

            System.out.println(lValue.getNameDef());
            expandedPixelExpr.getRed().visit(this,arg);
            generatedCode.append(",");
            expandedPixelExpr.getGreen().visit(this,arg);
            generatedCode.append(",");
            expandedPixelExpr.getBlue().visit(this,arg);
            generatedCode.append("))");

            //System.out.println("GENERATED CODE\n");
           // System.out.println(generatedCode);
        }
        else {
            System.out.println("In else case");
            lValue.visit(this, arg);
            System.out.println("appending =");
            generatedCode.append(" = ");
            rValue.visit(this, arg);
            generatedCode.append(";");
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        System.out.println("CG Visit Binary ");
        Kind operator = binaryExpr.getOp().kind();
        Expr leftExpr = binaryExpr.getLeftExpr();
        Expr rightExpr = binaryExpr.getRightExpr();

        generatedCode.append("(");  // Start parentheses
        //System.out.println(operator.toString());
        //Adding two pixels together using binaryPackedPixelPixelOP...
       // System.out.println(leftExpr.getType().toString() + " " + rightExpr.getType().toString());
        if (isPixelExpr(leftExpr, rightExpr)) {
            System.out.println("PixelExpr = TRUE");
            // Handle binary operations involving two pixels

            //generatedCode.append(operator.name());
            switch (operator.name()){
                case "EQ":
                    generatedCode.append("ImageOps.binaryPackedPixelBooleanOp(ImageOps.");
                    generatedCode.append("BoolOP.EQUALS");
                    break;
                case "TIMES":
                    System.out.println("CASE TIMES");
                default:
                    generatedCode.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.");
                    generatedCode.append("OP.");
                    generatedCode.append(operator.name());
                    break;
            }
            generatedCode.append(", ");
            leftExpr.visit(this, arg); // Visit the left pixel expression
            generatedCode.append(", ");
            rightExpr.visit(this, arg); // Visit the right pixel expression
            generatedCode.append(")");
        } //else if (isImageAndPixelOrIntOperation(leftExpr, rightExpr)) {

        //}
        else if (operator == Kind.EXP) {
            generatedCode.append("(int)Math.pow(");
            binaryExpr.getLeftExpr().visit(this, arg);
            generatedCode.append(", ");
            binaryExpr.getRightExpr().visit(this, arg);
            generatedCode.append(")");
        }
        else if (operator == Kind.TIMES && leftExpr.getType() == Type.PIXEL && rightExpr.getType() == Type.INT){
//p$2=(ImageOps.binaryPackedPixelIntOp(ImageOps.OP.TIMES,p$2,3));
            generatedCode.append("ImageOps.binaryPackedPixelIntOp(ImageOps.OP.TIMES, ");
            leftExpr.visit(this, arg);  // Visit the left pixel expression
            generatedCode.append(", ");
            rightExpr.visit(this, arg); // Visit the right integer expression
            generatedCode.append(")");
        }
        else if (operator == Kind.DIV && leftExpr.getType() == Type.PIXEL && rightExpr.getType() == Type.INT){
//p$2=(ImageOps.binaryPackedPixelIntOp(ImageOps.OP.TIMES,p$2,3));
            generatedCode.append("ImageOps.binaryPackedPixelIntOp(ImageOps.OP.DIV, ");
            leftExpr.visit(this, arg);  // Visit the left pixel expression
            generatedCode.append(", ");
            rightExpr.visit(this, arg); // Visit the right integer expression
            generatedCode.append(")");
        }
        else if (operator == Kind.RES_pixel){
            System.out.println("kind pixel");
        }
        else if (operator == Kind.EQ) {
            binaryExpr.getLeftExpr().visit(this, arg);  // Visit left
            generatedCode.append(" == "); //
            binaryExpr.getRightExpr().visit(this, arg); // Visit right
        }
        else if (operator == Kind.PLUS && leftExpr.getType() == Type.IMAGE && rightExpr.getType() == Type.IMAGE){

            generatedCode.append("ImageOps.copyInto((ImageOps.binaryImageImageOp(ImageOps.OP.PLUS,");
            leftExpr.visit(this, arg);  // Visit the left pixel expression
            generatedCode.append(", ");
            rightExpr.visit(this, arg); // Visit the right integer expression
            generatedCode.append("))");
            generatedCode.append(", ");
            rightExpr.visit(this, arg);
            generatedCode.append(")");


        }
        else if (operator == Kind.TIMES && leftExpr.getType() == Type.IMAGE && rightExpr.getType() == Type.IMAGE){
            System.out.println("TIMES BHIT");
            //ImageOps.copyInto((ImageOps.binaryImageScalarOp(ImageOps.OP.TIMES,i$2,3)),i$2);


        }
        else {

            binaryExpr.getLeftExpr().visit(this, arg);
            //generatedCode.append(" & 0x00FFFFFF) + ");
            String operatorSymbol = getOperatorSymbol(operator);
            //System.out.println(operatorSymbol);
            generatedCode.append(" ").append(operatorSymbol).append(" ");
            binaryExpr.getRightExpr().visit(this, arg);
            //generatedCode.append(") | 0xFF000000");
        }

        generatedCode.append(")");  // Close parentheses
        return null;
    }
    private boolean isPixelExpr(Expr left, Expr right) throws PLCCompilerException{

        if (left.getType() == Type.PIXEL && right.getType() == Type.PIXEL){
            System.out.println("True");
            return true;
        }
        return isPixelOrColorConstant(left) || isPixelOrColorConstant(right);

    }

    private boolean isPixelOrColorConstant(Expr expr) {
        return expr instanceof ExpandedPixelExpr || isColorConstant(expr);
    }
    private boolean isColorConstant(Expr expr) {

        if (expr instanceof ConstExpr) {
            String constName = ((ConstExpr) expr).getName().toUpperCase();
            return constName.equals("RED") || constName.equals("GREEN") || constName.equals("BLUE");
        }
        return false;
    }

    private String getOperatorSymbol(Kind kind) throws PLCCompilerException {
        switch (kind) {
            case PLUS: return "+";
            case MINUS: return "-";
            case GT: return ">";
            case TIMES: return "*";
            case OR: return "||";
            case BITOR: return "|";
            case LT: return "<";
            case LE: return "<=";
            case GE: return ">=";
            default: throw new PLCCompilerException("Unsupported operator: " + kind);
        }
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        System.out.println("Visit block");
        currentScopeLevel++;
        generatedCode.append("{\n"); // Start block
        for (AST statement : block.getElems()) {
            statement.visit(this, arg); // Visit each statement
            generatedCode.append(";\n"); // End each statement with a semicolon
        }
        generatedCode.append("}\n"); // End block
        currentScopeLevel--;
        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return visitBlock(statementBlock.getBlock(), arg);
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        Kind colorKind = channelSelector.color();
        System.out.println("visited channel selector");

        Expr baseExpr = (Expr) arg; // This should be the expression for the pixel selection
        System.out.println(baseExpr.toString());
        // Wrap the pixel selection expression with the appropriate PixelOps method
        switch (colorKind) {
            case RES_red:
                generatedCode.append("PixelOps.red(");
                break;
            case RES_green:
                generatedCode.append("PixelOps.green(");
                break;
            case RES_blue:
                generatedCode.append("PixelOps.blue(");
                break;
            default:
                throw new PLCCompilerException("Unexpected color");
        }

        baseExpr.visit(this, arg); // This should generate the ImageOps.getRGB call
        generatedCode.append(")"); // Close the PixelOps method call

        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        generatedCode.append("(");
        conditionalExpr.getGuardExpr().visit(this, arg);  // Condition part
        generatedCode.append(") ? ");
        conditionalExpr.getTrueExpr().visit(this, arg);  // Vtrue part
        generatedCode.append(" : ");
        conditionalExpr.getFalseExpr().visit(this, arg);  // false part
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        //String varName = nameDef.getJavaName(); // Adjust as necessary for your variable naming
        String varName = nameDef.getName();
        Type varType = nameDef.getType();

        System.out.println("varType: " + varType);
        System.out.println("Initializer is BinaryExpr: " + (declaration.getInitializer() instanceof BinaryExpr));
        System.out.println(declaration.getInitializer());
        if (varType != Type.IMAGE || (varType == Type.IMAGE && declaration.getInitializer() instanceof BinaryExpr)) {
            // Append type declaration for non-images or images initialized by a binary expression
            alreadyUsed = true;
            generatedCode.append(translateType(varType)).append(" ").append(nameDef.getName());
        }

        //generatedCode.append(translateType(varType)).append(" ").append(varName);


        if (declaration.getInitializer() != null) {
            Expr initializer = declaration.getInitializer();

            if (varType == Type.IMAGE) {
                // Check if initializer is a binary expression, implying dimensions
                if (initializer instanceof BinaryExpr) {
                    System.out.println("Instance of binary \n");
                    processImageOperation(initializer, arg, varName, alreadyUsed);

                    // Handle binary operation (e.g., image result = a + b)

                }else {
                    Dimension dimension = nameDef.getDimension();
                    if (dimension != null) {
                        if (declaration.getInitializer()!=null){System.out.println(declaration.getInitializer().getType().toString());}
                        System.out.println("nah we in here");


                        if (declaration.getInitializer().getType() == Type.IMAGE && varType == Type.IMAGE){

                            generatedCode.append("BufferedImage ").append(varName);
                            generatedCode.append(" = ImageOps.copyAndResize(");
                        }
                        else {
                            generatedCode.append("BufferedImage ").append(varName).append(" = FileURLIO.readImage(");
                        }

                        initializer.visit(this,arg);
                        generatedCode.append(",");
                        dimension.getWidth().visit(this,arg);
                        generatedCode.append(",");
                        dimension.getHeight().visit(this,arg);
                        //generatedCode.append(identExpr.getName());
                        generatedCode.append(")");
                    }
                    else {
                        System.out.println("IN THE FIRST LOOP");
                        //generatedCode.append("final BufferedImage ").append(varName).append(" = ImageOps.makeImage(");
                        System.out.println(initializer.toString());
                        //dimension.getWidth().visit(this, arg);  // Visit width expression
                        if (declaration.getInitializer()!=null){System.out.println(declaration.getInitializer().getType().toString());}
                        if (varType == Type.IMAGE && declaration.getInitializer().getType() == Type.IMAGE){
                            generatedCode.append("BufferedImage ").append(varName).append(" = ImageOps.cloneImage(");
                        }
                        else {
                            System.out.println("WE in this bitch");
                            generatedCode.append("BufferedImage ").append(varName).append(" = FileURLIO.readImage(");

                        }
                        declaration.getInitializer().visit(this,arg);
                        //generatedCode.append(identExpr.getName());
                        generatedCode.append(")");
                       // NameDef tempName = (NameDef) initializer.visit(this,arg);
                        //generatedCode.append(", ");
                        //dimension.getHeight().visit(this, arg); // Visit height expression

                    }

                }
            } else {
                // Handle non-image type initializations
                generatedCode.append(" = ");
                initializer.visit(this, arg);
            }
        }  else {
            System.out.println("ELSE OPTION\n");
            if (nameDef.getType() == Type.IMAGE) {
                System.out.println("Type Image in visit declaration");
                Dimension dimension = nameDef.getDimension();
                if (dimension == null) {

                    System.out.println("Null Dimension");

                    generatedCode.append("BufferedImage ").append(varName).append(" = FileURLIO.readImage(");
                    declaration.getInitializer().visit(this,arg);
                    //generatedCode.append(identExpr.getName());
                    generatedCode.append(")");
                }
                else {
                    System.out.println("Not null dimension");
                    generatedCode.append("final BufferedImage ").append(varName).append(" = ImageOps.makeImage(");
                    dimension.getWidth().visit(this, arg);  // Visit width expression
                    generatedCode.append(", ");
                    dimension.getHeight().visit(this, arg); // Visit height expression
                    generatedCode.append(")");
                }

            }
        }

        alreadyUsed = false;
        generatedCode.append(";");
        return null;
    }
    private void processImageOperation(Expr initializer, Object arg, String varName, boolean alrUsed) throws PLCCompilerException {
        BinaryExpr binaryExpr = (BinaryExpr) initializer;
        Kind operator = binaryExpr.getOp().kind();

        // Generate code for specific image operations

        if (operator == Kind.PLUS && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.IMAGE) {
            // Generate code for image addition
            if (!alrUsed){
                generatedCode.append("BufferedImage ").append(varName);
            }
            generatedCode.append(" = ImageOps.cloneImage((");
            generatedCode.append("ImageOps.binaryImageImageOp(ImageOps.OP.PLUS, ");
            binaryExpr.getLeftExpr().visit(this, arg);
            generatedCode.append(", ");
            binaryExpr.getRightExpr().visit(this, arg);
            generatedCode.append(")))");
        }
     }
    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        System.out.println("Entered dimension");
        Expr w = dimension.getWidth();
        Expr h = dimension.getHeight();

        Object width = w.visit(this,arg);
        Object height = h.visit(this,arg);


        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        System.out.println("ENTERED DO STATEMENT");

        // Declare the continue variable
        generatedCode.append("boolean continue$0 = false;\n");

        // Start the loop
        generatedCode.append("while (!continue$0) {\n");
        generatedCode.append("    continue$0 = true;\n");

        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            // Visit the guarded block, which handles the condition and the block of code
            guardedBlock.visit(this, arg);
        }

        // End the loop
        generatedCode.append("}\n");

        return null;
    }

    private String uniqueVarName(String base) {
        // Generate a unique variable name
        return base + "_" + UUID.randomUUID().toString().replace("-", "");
    }
    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        //(ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS,0xffff0000,PixelOps.pack(33,33,33)));
        System.out.println("Expanded  visitng");
        System.out.println(expandedPixelExpr.toString());
        generatedCode.append("PixelOps.pack(");
        expandedPixelExpr.getRed().visit(this, arg);
        generatedCode.append(", ");

        expandedPixelExpr.getGreen().visit(this, arg);
        generatedCode.append(", ");

        expandedPixelExpr.getBlue().visit(this, arg);
        generatedCode.append(")");
        //generatedCode.append("& 0x0000FFFF");

        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        // Generate the if condition for the guarded block
        generatedCode.append("    if (");
        guardedBlock.getGuard().visit(this, arg); // Append the guard condition to generatedCode
        generatedCode.append(") {\n");

        // Visit the block of the guarded block
        guardedBlock.getBlock().visit(this, arg);

        // Check if the block ends with a return statement
        if (!blockEndsWithReturn(guardedBlock.getBlock())) {
            // Only set continue$0 to false if the block does not end with a return statement
            generatedCode.append("        continue$0 = false;\n");
        }

        generatedCode.append("    }\n");

        return null;
    }
    private boolean blockEndsWithReturn(Block block) {
        List<Block.BlockElem> elements = block.getElems();

        if (!elements.isEmpty()) {
            Block.BlockElem lastElement = elements.get(elements.size() - 1);

            // Assuming 'ReturnStatement' is a subclass of BlockElem that represents return statements
            // You'll need to replace 'ReturnStatement' with the actual class name used in your AST
            return lastElement instanceof ReturnStatement;
        }

        return false;
    }
    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {

        String varName = identExpr.getName();
        System.out.println("Visit ident " + varName);

        NameDef nameDef = identExpr.getNameDef(); // Get linked NameDef
        //String javaName = nameDef.getJavaName();
       // System.out.println("Java name " + javaName);



        if (isJavaReservedKeyword(varName)) {
            varName += "_";
        }



        //String resolvedVarName = varName;
        //String resolvedVarName = getScopedVariableName(varName);
        //for (int currScope = currentScopeLevel; currScope >= 0; currScope--) {
        //    String scopedVarName = varName + "_" + currScope;
        //    if (scope.containsKey(scopedVarName)) {
       //         resolvedVarName = scopedVarName;
        //        break;
       //     }
      //  }
        //System.out.println(resolvedVarName);
        generatedCode.append(varName);
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        System.out.println("visited if");
        List<GuardedBlock> guardedBlocks = ifStatement.getGuardedBlocks();
        boolean isFirstBlock = true;

        for (GuardedBlock guardedBlock : guardedBlocks) {
            Expr guard = guardedBlock.getGuard();
            Block block = guardedBlock.getBlock();

            if (isFirstBlock) {
                generatedCode.append("if (");
                isFirstBlock = false;
            } else {
                // For "else if" or "else" blocks
                generatedCode.append("else ");
                if (!isAlwaysTrue(guard)) {
                    generatedCode.append("if (");
                }
            }

            if (!isAlwaysTrue(guard)) {
                guard.visit(this, arg);
                generatedCode.append(") ");
            }

            generatedCode.append("{\n");
            block.visit(this, arg);
            generatedCode.append("}\n");
        }

        return null;
    }
    private boolean isAlwaysTrue(Expr guard) {
        return false;
    }
    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        System.out.println("visit lvalue");
        String varName = lValue.getName();
        //System.out.println(lValue.getType().toString());
        if (isJavaReservedKeyword(varName)) {
            varName += "_";
        }
        String scopedVarName = getScopedVariableName(varName);
        generatedCode.append(lValue.getNameDef().getName());
        //generatedCode.append(scopedVarName);
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        System.out.println("visit name def");
       return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        generatedCode.append(numLitExpr.getText());
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        String colorName = pixelSelector.toString().toUpperCase();
        System.out.println("visited pixelselector");
        System.out.println(colorName.toString());
        switch (colorName) {
            case "RED":

                generatedCode.append("edu.ufl.cise.cop4020fa23.runtime.PixelOps.pack(255, 0, 0)");
                break;
            case "GREEN":

                generatedCode.append("edu.ufl.cise.cop4020fa23.runtime.PixelOps.pack(0, 255, 0)");
                break;
            case "BLUE":

                generatedCode.append("edu.ufl.cise.cop4020fa23.runtime.PixelOps.pack(0, 0, 255)");
                break;

            default:
                //System.out.println(pixelSelector.xExpr().getType().toString());
                if (pixelSelector.xExpr().getType() == Type.INT){
                    //generatedCode.append("edu.ufl.cise.cop4020fa23.runtime.PixelOps.pack(");

                }
                //throw new PLCCompilerException("Unsupported color: " + colorName);
        }
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        //If its a pixel
        System.out.println("Visit post fix expr");

        Expr primaryExpr = postfixExpr.primary();
        //primaryExpr.visit(this, arg);
        // check if there is a channel selector         expandedPixelExpr.getRed().visit(this, arg);
        //Before check f PixelSelector != null && ChannelSelector ==null
        if (postfixExpr.channel() == null && postfixExpr.pixel() !=null) {
            System.out.println("ImageOps.getRGB");
            generatedCode.append("ImageOps.getRGB(");

            primaryExpr.visit(this, arg); // This should append the image reference to generatedCode
            generatedCode.append(",");

            PixelSelector pixelSelector = postfixExpr.pixel();
            Expr xExpr = pixelSelector.xExpr(); // Assuming X() method returns the x coordinate expression
            Expr yExpr = pixelSelector.yExpr(); // Assuming Y() method returns the y coordinate expression
            xExpr.visit(this, arg); // This should append the x coordinate to generatedCode
            generatedCode.append(",");
            yExpr.visit(this, arg); // This should append the y coordinate to generatedCode

            generatedCode.append(")");

        }
        //return PixelOps.red(ImageOps.getRGB(i$2,x$1,y$1));
        else if (postfixExpr.channel() != null && postfixExpr.pixel() != null){
            //If PixelSelector != null && ChannelSelector != null,generate code to get the value of the pixel at theindicated location and to invoke PixelOps.red, PixelOps.green, or PixelOps.blue.
            System.out.println("Both Pixel and Channel exist");
            switch (postfixExpr.channel().color()) {
                case RES_red:
                    generatedCode.append("PixelOps.red(");
                    break;
                case RES_green:
                    generatedCode.append("PixelOps.green(");
                    break;
                case RES_blue:
                    generatedCode.append("PixelOps.blue(");
                    break;
                default:
                    throw new PLCCompilerException("Unexpected color");
            }

            // Now build and append the 'ImageOps.getRGB(' part
            generatedCode.append("ImageOps.getRGB(");
            postfixExpr.primary().visit(this, arg); // Append the image reference
            generatedCode.append(", ");
            postfixExpr.pixel().xExpr().visit(this, arg); // Append the x coordinate
            generatedCode.append(", ");
            postfixExpr.pixel().yExpr().visit(this, arg); // Append the y coordinate
            generatedCode.append("))"); // Close both the PixelOps and ImageOps calls

        }
        else if (postfixExpr.channel() != null && postfixExpr.pixel() ==null && primaryExpr.getType() != Type.PIXEL){
            System.out.println("Channel exists but Pixel Selector is null");
            System.out.println(primaryExpr.getType().toString());
            ChannelSelector channelSelector = postfixExpr.channel();
            Kind colorKind = channelSelector.color();

            switch (colorKind) {
                case RES_red:
                    generatedCode.append("ImageOps.extractRed(");
                    break;
                case RES_green:
                    generatedCode.append("ImageOps.extractGreen(");
                    break;
                case RES_blue:
                    generatedCode.append("ImageOps.extractBlue(");
                    break;
                default:
                    throw new PLCCompilerException("Unexpected color channel");
            }

            postfixExpr.primary().visit(this, arg); // Assuming this appends the image reference
            generatedCode.append(")"); // Close the method call
        }
        else{
            if (postfixExpr.channel() != null) {
                System.out.println("Channel exists");
                ChannelSelector channelSelector = postfixExpr.channel();
                visitChannelSelector(channelSelector, primaryExpr);


            }
            if (postfixExpr.pixel() != null) {
                System.out.println("Pixel exists");
                PixelSelector pixelSelector = postfixExpr.pixel();
                visitPixelSelector(pixelSelector, primaryExpr);
            }
        }
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        System.out.println("ENTERING CODE GEN \n\n");
        //PACKAGE NAME
        String packageName = (String) arg;
        String className = program.getNameToken().text();

        // Append the package
        if (packageName != null && !packageName.isEmpty()) {
            generatedCode.append("package ").append(packageName).append(";\n\n");
        }
//CONSOLE IO and PIXEL IO
        generatedCode.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n\n");
        generatedCode.append("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n\n");
        generatedCode.append("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n\n");
        generatedCode.append("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;\n\n");
        generatedCode.append("import java.awt.image.BufferedImage;\n\n");
        //Begin class def
        generatedCode.append("public class ").append(className).append(" {\n");
        Type returnType = convertTokenTypeToType(program.getTypeToken());
        //Type returnType = visitNameDef(program, arg);
        generatedCode.append("    public static ").append(translateType(returnType)).append(" apply(");

        // Handle param
        List<NameDef> params = program.getParams();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) generatedCode.append(", "); //IF MULTIPLE PARAMS
            NameDef param = params.get(i);
            String paramName = sanitizeIdentifier(param.getName());
            String paramType = translateType(convertTokenTypeToType(param.getTypeToken()));
            generatedCode.append(paramType).append(" ").append(paramName);
        }

        generatedCode.append(") {\n");

        // BODY OF BLOCK
        program.getBlock().visit(this, null);

        generatedCode.append("    }\n");

        // End class
        generatedCode.append("}\n");

        return generatedCode.toString();
    }
    private String sanitizeIdentifier(String identifier) {
        if (isJavaReservedKeyword(identifier)) {
            return identifier + "_";
        }
        return identifier;
    }

    private boolean isJavaReservedKeyword(String word) {
        System.out.println("visit is javaReserverekeyword");
        // List of Java reserved words
        Set<String> reservedWords = Set.of(
                "abstract", "continue", "for", "new", "switch",
                "assert", "default", "goto", "package", "synchronized",
                "boolean", "do", "if", "private", "this",
                "break", "double", "implements", "protected", "throw",
                "byte", "else", "import", "public", "throws",
                "case", "enum", "instanceof", "return", "transient",
                "catch", "extends", "int", "short", "try",
                "char", "final", "interface", "static", "void",
                "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while",
                "true", "false", "null"
                // might be some missing DOUBLE CHECK***
        );
        return reservedWords.contains(word);
    }
    private Type convertTokenTypeToType(IToken typeToken) {
        System.out.println("ConvertToken entered");
        switch (typeToken.kind()) {
            case RES_int:
                return Type.INT;
            case RES_boolean:
                return Type.BOOLEAN;
            case RES_void:
                return Type.VOID;
            case RES_string:
                return Type.STRING;
                //ADD MORE FOR WHATEVERS MISSING
            case RES_pixel:
                System.out.println("poxel");
                return Type.PIXEL;
            case RES_image:
                //System.out.println("Image 1");
                return Type.IMAGE;
            default:
                throw new IllegalArgumentException("Unknown type token: " + typeToken.text());
        }
    }


    private String translateType(Type type) {
        //TRANSLATES TO STRING
        System.out.println("translating type");
        System.out.println(type.toString());
        if (type == Type.INT) {
            return "int";
        } else if (type == Type.BOOLEAN) {
            return "boolean";
        }
        else if (type == Type.STRING){
            return "String";
        }
        else if (type == Type.PIXEL){
            return "int";
        }
        else if (type == Type.IMAGE){

            return "BufferedImage";
        }
        System.out.println("returning void");
        return "void"; // void = default
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        System.out.println("visit return");
        generatedCode.append("return ");
        if (returnStatement.getE() != null) {
            System.out.println(returnStatement.getE().toString());
            returnStatement.getE().visit(this, arg);
        } else {

            throw new PLCCompilerException("Return statement must have an expression in a non-void function.");
        }
        //generatedCode.append(";");
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("visit string");
        generatedCode.append(stringLitExpr.getText());
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        System.out.println("visit unary");
        Kind op = unaryExpr.getOp();
        if (op == Kind.MINUS) {
            if (unaryExpr.getExpr() instanceof UnaryExpr &&
                    ((UnaryExpr) unaryExpr.getExpr()).getOp() == Kind.MINUS) {
                // case for --a ect
                ((UnaryExpr) unaryExpr.getExpr()).getExpr().visit(this, arg);
            } else {
                // SINGLE MINUS
                generatedCode.append("-(");
                unaryExpr.getExpr().visit(this, arg);
                generatedCode.append(")");
            }

        } else if (op == Kind.BANG){

            generatedCode.append("!");
            unaryExpr.getExpr().visit(this,arg);
        }
        else if (op == Kind.RES_width){
            //generatedCode.append("")
            generatedCode.append("(");
            unaryExpr.getExpr().visit(this,arg);
            generatedCode.append(".getWidth())");

        }
        else if ( op == Kind.RES_height){
            generatedCode.append("(");
            unaryExpr.getExpr().visit(this,arg);
            generatedCode.append(".getHeight())");
        }
        else {
            //continue later
        }
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        System.out.println("Visit write statement");
        generatedCode.append("ConsoleIO.write");

        if (writeStatement.getExpr() instanceof IdentExpr){

            IdentExpr identExpr = (IdentExpr) writeStatement.getExpr();
            NameDef nameDef = identExpr.getNameDef(); // Get linked NameDef
            if (nameDef.getType() == Type.PIXEL){
                generatedCode.append("Pixel(");
            }
            else {
                generatedCode.append("(");
            }
        }
        else {
            generatedCode.append("(");
        }

        writeStatement.getExpr().visit(this, arg);
        generatedCode.append(");");
        return null;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("visit boolean");
        String literalValue = booleanLitExpr.getText().equalsIgnoreCase("TRUE") ? "true" : "false";
        generatedCode.append(literalValue);
        return null;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        System.out.println("Visit const");
        char checker = constExpr.getName().toCharArray()[0];
        if (checker == 'Z'){
            //System.out.println("Z");
            generatedCode.append(255);
        }
        String constName = constExpr.getName();

        switch (constName){
            case "RED":
                //generatedCode.append("java.awt.Color.RED.getRGB()");
                generatedCode.append("0x" + Integer.toHexString(Color.RED.getRGB()));
                break;
                //return Color.RED.getRGB();
            case "GREEN":
                System.out.println("in green");
                generatedCode.append("0x" + Integer.toHexString(Color.GREEN.getRGB()));
                break;
            case "BLUE":
                generatedCode.append("0x" + Integer.toHexString(Color.BLUE.getRGB()));
                break;
            case "PINK":
                generatedCode.append("0x" + Integer.toHexString(Color.PINK.getRGB()));
                break;
            case "MAGENTA":
                generatedCode.append("0x" + Integer.toHexString(Color.MAGENTA.getRGB()));
                break;
            case "CYAN":
                generatedCode.append("0x" + Integer.toHexString(Color.CYAN.getRGB()));
                break;
            case "BLACK":
                generatedCode.append("0x" + Integer.toHexString(Color.BLACK.getRGB()));
                break;
            case "LIGHT_GRAY":
                generatedCode.append("0x" + Integer.toHexString(Color.LIGHT_GRAY.getRGB()));
                break;
            case "YELLOW":
                generatedCode.append("0x" + Integer.toHexString(Color.YELLOW.getRGB()));
                break;
            case "ORANGE":
                generatedCode.append("0x" + Integer.toHexString(Color.ORANGE.getRGB()));
                break;
            case "WHITE":
                generatedCode.append("0x" + Integer.toHexString(Color.WHITE.getRGB()));
                break;
            case "GRAY":
                generatedCode.append("0x" + Integer.toHexString(Color.GRAY.getRGB()));
                break;
            case "DARK_GRAY":
                generatedCode.append("0x" + Integer.toHexString(Color.DARK_GRAY.getRGB()));
                break;
            case "WIDTH":


        }
        return null;

    }
    private String getScopedVariableName(String varName) {
        for (int currScope = currentScopeLevel; currScope >= 0; currScope--) {
            String scopedVarName = varName + "_" + currScope;
            if (scope.containsKey(scopedVarName)) {
                return scopedVarName;
            }
        }
        return varName;  // orignal name
    }
}
