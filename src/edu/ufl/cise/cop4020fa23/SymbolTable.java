package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.*;

public class SymbolTable {
    private Stack<Map<String, SymbolTableEntry>> scopes;
    private Stack<Integer> scopeStack; // Stack to keep track of active scopes
    private Set<String> allDeclaredVariables = new HashSet<>();

    private int currentScopeID;

    public void setCurrentScope(int passedScope){
        this.currentScopeID = passedScope;
    }

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.currentScopeID = 0;
        this.scopeStack = new Stack<>();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
        currentScopeID++;
        //System.out.println("Entering and pushing scope CurrentSCopeID: " + currentScopeID);
        scopeStack.push(currentScopeID);
    }

    public boolean isDeclaredInLowerScope(String variableName) {
        for (int i = scopes.size() - 2; i >= 0; i--) {
            if (scopes.get(i).containsKey(variableName)) {
                return true; // Found the variable in an outer scope
            }
        }
        return false; // Variable not found in any outer scope
    }



    public void addDeclaredVariable(String name) {
        allDeclaredVariables.add(name);
    }

    public boolean isVariableEverDeclared(String name) {
        return allDeclaredVariables.contains(name);
    }
    //Remove variable on left hand side if declared there
    public void removeVar(String name){
        allDeclaredVariables.remove(name);
    }
    public void leaveScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
        if (currentScopeID > 0) {
            currentScopeID--;
        }
    }

    public void insert(String name, NameDef nameDef) {
        SymbolTableEntry previousEntry = getEntry(name); // Get previous entry if it exists
        String javaName = name;
        //System.out.println("In insert..Current scope Id " + currentScopeID);
        if (previousEntry != null && previousEntry.scopeID < currentScopeID) {
            javaName = name + "_" + currentScopeID;
            System.out.println("Adding variable " + name + " currScope " + currentScopeID);
        }
        nameDef.setJavaName(javaName); // Assuming NameDef has a setJavaName method

        SymbolTableEntry newEntry = new SymbolTableEntry(nameDef, currentScopeID, previousEntry);
        scopes.peek().put(name, newEntry);
    }
    public SymbolTableEntry getEntry(String name) {

        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolTableEntry entry = scopes.get(i).get(name);
            if (entry != null) {
                //System.out.println("Entry isnt null");
                return entry;
            }
        }
        //System.out.println("returning null");
        return null;
    }
    public boolean isVariableDeclaredInScope(String name, int scopeID) {
        if (scopeID < scopes.size()) {
            return scopes.get(scopeID).containsKey(name);
        }
        return false;
    }

    public NameDef lookup(String name) {
        SymbolTableEntry entry = getEntry(name);
        if (entry != null && scopeStack.contains(entry.scopeID)) {
            return entry.nameDef;
        }
        return null;
    }
    public boolean isInCurrentScope(String name) {
        if (scopes.isEmpty()) {
            return false;
        }
        Map<String, SymbolTableEntry> currentScope = scopes.peek();
        return currentScope.containsKey(name);
    }



    public int getCurrentScopeID() {
        return currentScopeID;
    }
}

class SymbolTableEntry {
    NameDef nameDef;
    int scopeID;
    SymbolTableEntry previous; // Link to previous entry with the same name

    public SymbolTableEntry(NameDef nameDef, int scopeID, SymbolTableEntry previous) {
        this.nameDef = nameDef;
        this.scopeID = scopeID;
        this.previous = previous;
    }
}


