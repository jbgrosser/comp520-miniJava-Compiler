package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.AbstractSyntaxTrees.*;

public class Identification implements Visitor<Object,Object> {
	private ErrorReporter _errors;
	private Stack<HashMap<String,Declaration>> idTable = new Stack<>();
	private MethodDecl currMethodDecl = null;
	private String currVar = null;
	private ClassDecl helperClass = null;
	private ClassDecl currentClass = null;
	private boolean qref = false;
	
	public Identification(ErrorReporter errors) {
		this._errors = errors;
	}

	public void parse( Package prog ) {
		try {
			visitPackage(prog,null);
		} catch( IdentificationError e ) {
			_errors.reportError(e.toString());
		}
	}

	class IdentificationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;
		
		public IdentificationError(AST ast, String errMsg) {
			super();
			this._errMsg = ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg;
		}
		
		@Override
		public String toString() {
			return _errMsg;
		}
	}

	public void addDeclaration(String s, Declaration d, int index) {
		if (findDeclaration(s, index) == null) {
			idTable.peek().put(s, d);
		}
		else {
			throw new IdentificationError(d, s +  " already exists in this scope.");
		}
	}

	public Declaration findDeclaration(String s, int index) {
		if (idTable.get(index).containsKey(s)) {
			return idTable.get(index).get(s);
		}
		return null;
	}

	public Declaration findDeclarationLoop(String s) {
		for (int i = idTable.size() - 1; i >= 1; i--) {
			if (idTable.get(i).containsKey(s)) {
				return idTable.get(i).get(s);
			}
		}
		return null;
	}

	@Override
	public Object visitPackage(Package prog, Object arg) throws IdentificationError {
		HashMap<String,Declaration> l0 = new HashMap<>();
		idTable.add(l0);
		String prefix = arg + "  . ";
		FieldDecl fd = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenType.ID, "_PrintStream")), null),"out" , null);
		FieldDeclList fdl = new FieldDeclList();
		fdl.add(fd);
		l0.put("System", new ClassDecl("System", fdl, new MethodDeclList(), null));
		ParameterDecl pd = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
		ParameterDeclList pdl = new ParameterDeclList();
		pdl.add(pd);
		MethodDecl md = new MethodDecl(fd, pdl, new StatementList(), null);
		MethodDeclList mdl = new MethodDeclList();
		mdl.add(md);
		l0.put("_PrintStream", new ClassDecl("_PrintStream", new FieldDeclList(), mdl, null));
		l0.put("String", new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null));

		for(ClassDecl c : prog.classDeclList) {
			if (l0.containsKey(c.name)){
				throw new IdentificationError(c, "Duplicate class name: " + c.name);
			}
			else{
				l0.put(c.name, c);
			}
		}
		for(ClassDecl c : prog.classDeclList) {
			this.currentClass = c;
			c.visit(this, prefix);
		}
		idTable.pop();
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		HashMap<String,Declaration> l1 = new HashMap<>();
		idTable.push(l1);
		FieldDecl fd = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenType.ID, "_PrintStream")), null),"out" , null);
		l1.put("out", fd);
		ParameterDecl pd = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
		ParameterDeclList pdl = new ParameterDeclList();
		pdl.add(pd);
		MethodDecl md = new MethodDecl(fd, pdl, new StatementList(), null);
		l1.put("println", md);
		String prefix = arg + "  . ";
		for(FieldDecl f : cd.fieldDeclList) {
			addDeclaration(f.name, f, idTable.size() - 1);
		}
		for(MethodDecl m : cd.methodDeclList) {
			addDeclaration(m.name, m, idTable.size() - 1);
		}
		for(FieldDecl f : cd.fieldDeclList) {
			f.visit(this, prefix);
		}
		for(MethodDecl m : cd.methodDeclList) {
			m.visit(this, prefix);
		}
		idTable.pop();
		return null;
	}
    
	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, fd);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		this.currMethodDecl = md;
		md.type.visit(this, md);
		HashMap<String,Declaration> l2 = new HashMap<>();
		idTable.push(l2);
		String prefix = arg + "  . ";
		for (ParameterDecl p : md.parameterDeclList) {
			p.visit(this, prefix);
		}
		for (Statement s : md.statementList) {
			s.visit(this, prefix);
		}
		idTable.pop();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		pd.type.visit(this, arg);
		addDeclaration(pd.name, pd, idTable.size() - 1);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, arg);
		addDeclaration(decl.name, decl, idTable.size() - 1);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		Declaration temp = findDeclaration(type.className.spelling, 0);
		if (temp == null) {
			throw new IdentificationError(type, "Undeclared class: " + type.className.spelling);
		}
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		HashMap<String,Declaration> l3 = new HashMap<>();
		idTable.push(l3);
		String prefix = arg + "  . ";
		Object temp = null;

		for (Statement s : stmt.sl) {
			if (s.visit(this, prefix) != null) {
				temp = true;
			}
		}
		idTable.pop();
		return temp;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, arg);
		this.currVar = stmt.varDecl.name;
		stmt.initExp.visit(this, arg);
		this.currVar = null;
		return true;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, arg);
		stmt.val.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, arg);
		stmt.ix.visit(this, arg);
		stmt.exp.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		if (stmt.methodRef.toString().equals("ThisRef")) {
			throw new IdentificationError(stmt.methodRef, "this is not allowed as a method call");
		}
		for (Expression e : stmt.argList) {
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		stmt.returnExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, arg);
		if (stmt.thenStmt.visit(this, arg) != null) {
			throw new IdentificationError(stmt.thenStmt, "solitary variable declaration statement not permitted here");
		}
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt.visit(this, arg) != null) {
				throw new IdentificationError(stmt.elseStmt, "solitary variable declaration statement not permitted here");
			}
		}
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, arg);
		if (stmt.body.visit(this, arg) != null) {
			throw new IdentificationError(stmt.body, "solitary variable declaration statement not permitted here");
		}
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, arg);
		expr.right.visit(this, arg);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, arg);
		expr.ixExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		expr.functionRef.visit(this, arg);
		for (Expression e : expr.argList) {
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, arg);
		expr.sizeExpr.visit(this, arg);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		if (this.currMethodDecl.isStatic) {
			throw new IdentificationError(ref, "this not allowed in static method");
		}
		this.helperClass = this.currentClass;
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (this.currVar != null) {
			if (this.currVar.equals(ref.id.spelling)) {
				throw new IdentificationError(ref.id, "cannot reference variable declared within initialization expression");
			}
		}
		Declaration dec = null;
		if(this.qref) {
			for (int i = idTable.size() - 1; i >= 0; i--) {
				if (idTable.get(i).containsKey(ref.id.spelling)) {
					dec = idTable.get(i).get(ref.id.spelling);
				}
			}
		}
		else {
			dec = findDeclarationLoop(ref.id.spelling);
		}
		if (dec == null) {
			throw new IdentificationError(ref, "Declaration not found: " + ref.id.spelling);
		}
		
		if (qref) {
			if (dec.toString().equals("VarDecl")) {
				this.helperClass = (ClassDecl) this.idTable.get(0).get(((VarDecl) dec).classn);
			}
			else if (dec.toString().equals("FieldDecl")) {
				this.helperClass = (ClassDecl) this.idTable.get(0).get(((FieldDecl) dec).classn);
			}	
			else if (dec.toString().equals("ClassDecl")) {
				this.helperClass = (ClassDecl) this.idTable.get(0).get(dec.name);
			}
			else if (dec.toString().equals("MethodDecl")) {
				this.helperClass = (ClassDecl) this.idTable.get(0).get(((MethodDecl) dec).classn);
			}
			else {
				System.out.println(dec.toString());
			}
		}
		return "VarDecl";
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		this.qref = true;
		ref.ref.visit(this, arg);
		if (this.helperClass == null) {
			throw new IdentificationError(ref, "Qual Ref Error");
		}
		else {
			if (this.helperClass != null && this.helperClass.fieldDeclList != null) {
				for (FieldDecl fd : this.helperClass.fieldDeclList) {
					if (ref.id.spelling.equals(fd.name)) {
						if (!this.currentClass.name.equals(this.helperClass.name) && fd.isPrivate) {
							throw new IdentificationError(ref, "private field error: " + fd.name);
						}
						if (idTable.get(1).containsKey(fd.name)) {
							for (Declaration value : idTable.get(0).values()) {
								ClassDecl cl = (ClassDecl) value;
								for (MethodDecl m : cl.methodDeclList) {
									if (m.name.equals(fd.name)) {
										this.helperClass = (ClassDecl) cl;
										break;
									}
								}
							}
						}
						else {
							throw new IdentificationError(ref, "Mulit Qual Fail");
						}
						break;
					}
				}
			}
			if (this.helperClass != null && this.helperClass.methodDeclList != null) {
				for (MethodDecl md : this.helperClass.methodDeclList) {
					if (ref.id.spelling.equals(md.name)) {
						if (!this.currentClass.name.equals(this.helperClass.name) && md.isPrivate) {
							throw new IdentificationError(ref, "private method error: " + md.name);
						}
						if (idTable.get(1).containsKey(md.name)) {
							for (Declaration value : idTable.get(0).values()) {
								ClassDecl cl = (ClassDecl) value;
								for (MethodDecl m : cl.methodDeclList) {
									if (m.name.equals(md.name)) {
										this.helperClass = (ClassDecl) cl;
										break;
									}
								}
							}
						}
						else {
							throw new IdentificationError(ref, "Mulit Qual Fail");
						}
						break;
					}
				}
			}
		}
		this.qref = false;
		return "MethodDecl";
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nuLL, Object arg) {
		return null;
	}
}