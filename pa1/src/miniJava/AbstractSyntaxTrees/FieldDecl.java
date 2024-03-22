/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FieldDecl extends MemberDecl {
	public String classn;
	public FieldDecl(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, SourcePosition posn){
    	super(isPrivate, isStatic, t, name, posn);
		this.classn = null;
	}

	public FieldDecl(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, String classn, SourcePosition posn){
    	super(isPrivate, isStatic, t, name, posn);
		this.classn = classn;
	}
	
	public FieldDecl(MemberDecl md, SourcePosition posn) {
		super(md,posn);
		this.classn = null;
	}

	public FieldDecl(MemberDecl md, String classn, SourcePosition posn) {
		super(md,posn);
		this.classn = classn;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitFieldDecl(this, o);
    }
}

