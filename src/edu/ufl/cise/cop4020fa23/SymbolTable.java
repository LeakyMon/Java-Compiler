package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.*;

public class SymbolTable {
    private Stack<Map<String, NameDef>> scopes;
    private int currentScopeID;
    private Set<String> allDeclaredVariables = new HashSet<>();

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.currentScopeID = 0;
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void leaveScope() {
        scopes.pop();
    }

    public boolean insert(String name, NameDef def) throws TypeCheckException {
        if (isInCurrentScope(name)) {
            throw new TypeCheckException("Variable already exists");
             // Variable already declared in the current scope
        }
        //String uniqueName = generateUniqueName(name);
        //def.setJavaName(uniqueName);
        scopes.peek().put(name, def);
        return true;
    }
    public void removeVar(String name){
        allDeclaredVariables.remove(name);
    }
    public void addDeclaredVariable(String name) {
        allDeclaredVariables.add(name);
    }
    public NameDef lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, NameDef> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // Variable not found in any scope
    }

    public int lookupScope(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return i; // Return the scope level where the variable is found
            }
        }
        return -1; // Variable not found in any scope
    }

    public boolean isInCurrentScope(String name) {
        if (!scopes.isEmpty()) {
            Map<String, NameDef> currentScope = scopes.peek();
            return currentScope.containsKey(name);
        }
        return false;
    }
    public NameDef lookupInCurrScope(String name, int importScope){

        Map<String, NameDef> scope = scopes.get(importScope);
        if (scope.containsKey(name)) {
            return scope.get(name);
        }

        return null;
    }

    public int getCurrentScopeID() {
        return currentScopeID;
    }

    private String generateUniqueName(String baseName) {
        return baseName + "_" + (++currentScopeID);
    }

    public boolean isVariableEverDeclared(String name) {
        return allDeclaredVariables.contains(name);
    }
    public boolean isDeclaredInLowerScope(String variableName) {
        for (int i = scopes.size() - 2; i >= 0; i--) {
            if (scopes.get(i).containsKey(variableName)) {
                return true; // Found the variable in an outer scope
            }
        }
        return false; // Variable not found in any outer scope
    }
    public void setCurrentScope(int s) {
        this.currentScopeID = s;

    }

}
class SymbolTableEntry {
    NameDef nameDef;
    int scopeID;
    int value;
    ; // Link to previous entry with the same name

    public SymbolTableEntry(NameDef nameDef, int scopeID, int value) {
        this.nameDef = nameDef;
        this.scopeID = scopeID;
        this.value = value;
    }
}

