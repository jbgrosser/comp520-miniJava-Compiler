/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    public String classn;
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
        this.classn = null;
    }

    public TypeDenoter(TypeKind type, SourcePosition posn, String classn){
        super(posn);
        typeKind = type;
        this.classn = classn;
    }
    
    public TypeKind typeKind;
    
}

        