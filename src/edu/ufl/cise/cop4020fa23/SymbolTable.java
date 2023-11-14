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

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.currentScopeID = 0;
        this.scopeStack = new Stack<>();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
        currentScopeID++;
        scopeStack.push(currentScopeID); //
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
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
        if (currentScopeID > 0) {
            currentScopeID--;
        }
    }

    public void insert(String name, NameDef nameDef) {
        SymbolTableEntry previousEntry = getEntry(name); // Get previous entry if it exists
        SymbolTableEntry newEntry = new SymbolTableEntry(nameDef, currentScopeID, previousEntry);
        scopes.peek().put(name, newEntry);
    }
    public SymbolTableEntry getEntry(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolTableEntry entry = scopes.get(i).get(name);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }
    public boolean isVariableDeclared(String name) {
        for (Map<String, SymbolTableEntry> scope : scopes) {
            if (scope.containsKey(name)) {
                System.out.println("variable declared");
                return true;
            }
        }
        System.out.println("variable not declared");

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


