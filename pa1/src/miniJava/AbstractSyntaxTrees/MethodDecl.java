/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodDecl extends MemberDecl {
	public String classn;
	public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, SourcePosition posn){
        super(md,posn);
        parameterDeclList = pl;
        statementList = sl;
        this.classn = null;
	}

    public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, String classn, SourcePosition posn){
        super(md,posn);
        parameterDeclList = pl;
        statementList = sl;
        this.classn = classn;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }
	
	public ParameterDeclList parameterDeclList;
	public StatementList statementList;
}
