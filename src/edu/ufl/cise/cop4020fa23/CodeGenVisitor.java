package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;

import edu.ufl.cise.cop4020fa23.DynamicJavaCompileAndExecute.*;
import java.util.*;
import java.util.stream.Collectors;

public class CodeGenVisitor implements ASTVisitor {

    private StringBuilder generatedCode = new StringBuilder();
    private HashMap<String, Integer> scope = new HashMap<>();
    private int currentScopeLevel = 0;

    public String getGeneratedCode() {
        return generatedCode.toString();
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        String varName = getScopedVariableName(lValue.getName());
        generatedCode.append(varName).append(" = ");

        // RHS
        assignmentStatement.getE().visit(this, arg);
        generatedCode.append(";");
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Kind operator = binaryExpr.getOp().kind();
        generatedCode.append("(");  // Start parentheses

        if (operator == Kind.EXP) {
            generatedCode.append("(int)Math.pow(");
            binaryExpr.getLeftExpr().visit(this, arg);
            generatedCode.append(", ");
            binaryExpr.getRightExpr().visit(this, arg);
            generatedCode.append(")");
        }
        else if (operator == Kind.EQ) {
            binaryExpr.getLeftExpr().visit(this, arg);  // Visit left
            generatedCode.append(" == "); //
            binaryExpr.getRightExpr().visit(this, arg); // Visit right
        }
        else {
            // Handle others .. To be continued
            binaryExpr.getLeftExpr().visit(this, arg);
            String operatorSymbol = getOperatorSymbol(operator);
            generatedCode.append(" ").append(operatorSymbol).append(" ");
            binaryExpr.getRightExpr().visit(this, arg);
        }

        generatedCode.append(")");  // Close parentheses
        return null;
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

        String varType = translateType(declaration.getNameDef().getType());
        String varName = declaration.getNameDef().getName();
        String scopedName = varName;

        //MODIFY VARIABLES THAT ARE USED IN OTHER SCOPES TO ADD A _ then the CURRSCOPE
        if (scope.containsKey(varName)) {
            scopedName = varName + "_" + currentScopeLevel;
        }
        scope.put(scopedName, currentScopeLevel);

        generatedCode.append(varType).append(" ").append(scopedName);

        if (declaration.getInitializer() != null) {
            generatedCode.append(" = ");
            declaration.getInitializer().visit(this, arg);
        }
        //PROCESS DIMENSION
        Dimension dimension = declaration.getNameDef().getDimension();
        if (dimension != null) {
            dimension.visit(this, arg);
        }
        //END
        generatedCode.append(";");
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        String varName = identExpr.getName();
        System.out.println("Visit ident");
        if (isJavaReservedKeyword(varName)) {
            varName += "_";
        }
        String resolvedVarName = varName;

        for (int currScope = currentScopeLevel; currScope >= 0; currScope--) {
            String scopedVarName = varName + "_" + currScope;
            if (scope.containsKey(scopedVarName)) {
                resolvedVarName = scopedVarName;
                break;
            }
        }
        System.out.println(varName);
        generatedCode.append(resolvedVarName);
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        String varName = lValue.getName();
        if (isJavaReservedKeyword(varName)) {
            varName += "_";
        }
        String scopedVarName = getScopedVariableName(varName);
        generatedCode.append(scopedVarName);
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        generatedCode.append(numLitExpr.getText());
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {

        //PACKAGE NAME
        String packageName = (String) arg;
        String className = program.getNameToken().text();

        // Append the package
        if (packageName != null && !packageName.isEmpty()) {
            generatedCode.append("package ").append(packageName).append(";\n\n");
        }

        generatedCode.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n\n");

        //Begin class def
        generatedCode.append("public class ").append(className).append(" {\n");
        Type returnType = convertTokenTypeToType(program.getTypeToken());
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
            default:
                throw new IllegalArgumentException("Unknown type token: " + typeToken.text());
        }
    }


    private String translateType(Type type) {
        //TRANSLATES TO STRING
        if (type == Type.INT) {
            return "int";
        } else if (type == Type.BOOLEAN) {
            return "boolean";
        }
        else if (type == Type.STRING){
            return "String";
        }

        return "void"; // void = default
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        generatedCode.append("return ");
        if (returnStatement.getE() != null) {
            returnStatement.getE().visit(this, arg);
        } else {

            throw new PLCCompilerException("Return statement must have an expression in a non-void function.");
        }
        //generatedCode.append(";");
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        generatedCode.append(stringLitExpr.getText());
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
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
        else {
            //continue later
        }
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        generatedCode.append("ConsoleIO.write(");
        writeStatement.getExpr().visit(this, arg);
        generatedCode.append(");");
        return null;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        String literalValue = booleanLitExpr.getText().equalsIgnoreCase("TRUE") ? "true" : "false";
        generatedCode.append(literalValue);
        return null;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
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
