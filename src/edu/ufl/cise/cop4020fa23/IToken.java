
package edu.ufl.cise.cop4020fa23;

/**
 * IMPLEMENTATIONS of IToken should override equals and hashcode
 */
public interface IToken {
	
	public SourceLocation sourceLocation();
	public Kind kind();
	public String text();
}
