package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
	private ErrorReporter _errors;
	private Stack<HashMap<String,Declaration>> idTable = new Stack<>();
	private MethodDecl currMethodDecl = null;
	private String currentClass = null;
	private String helperClass = this.currentClass;
	private boolean returnExists = false;
	private boolean qref = false;
	private boolean isCallExpr = false;
	private HashMap<String, Declaration> local = new HashMap<>();
	
	public TypeChecking(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog) {
		prog.visit(this, null);
	}

	private void reportTypeError(AST ast, String errMsg) {
		_errors.reportError( ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg );
	}

	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {
		HashMap<String, Declaration> l0 = new HashMap<>();
		idTable.add(l0);
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
			l0.put(c.name, c);
		}
		for(ClassDecl c : prog.classDeclList) {
			this.currentClass = c.name;
			c.visit(this, arg);
		}
		idTable.pop();
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		HashMap<String,Declaration> l1 = new HashMap<>();
		idTable.push(l1);
		FieldDecl fd = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenType.ID, "_PrintStream")), null),"out" , null);
		l1.put("out", fd);
		ParameterDecl pd = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
		ParameterDeclList pdl = new ParameterDeclList();
		pdl.add(pd);
		MethodDecl md = new MethodDecl(fd, pdl, new StatementList(), null);
		l1.put("println", md);
		for (FieldDecl f : cd.fieldDeclList) {
			this.idTable.peek().put(f.name, f);
		}
		for (MethodDecl m : cd.methodDeclList) {
			this.idTable.peek().put(m.name, m);
		}
		for(MethodDecl m : cd.methodDeclList) {
			m.visit(this, arg);
			this.local.clear();
		}
		idTable.pop();
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		TypeDenoter type = null;
		TypeDenoter returnT = md.type;
		this.returnExists = false;
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, arg);
		}
		for (Statement s : md.statementList) {
			type = s.visit(this, arg);
		}
		if (type != null) {
			if (returnT.typeKind == TypeKind.VOID) {
				if (this.returnExists) {
					reportTypeError(type, "Return expression not allowed with void return");
				}
			}
			else {
				if (!this.returnExists) {
					reportTypeError(type, "Return expression not found");
				}
				else if (returnT.typeKind != type.typeKind) {
					reportTypeError(type, "Return type issue");
				}
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		this.local.put(pd.name, pd);
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		this.local.put(decl.name, decl);
		return decl.type;
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		for (Statement s : stmt.sl) {
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		local.put(stmt.varDecl.name, stmt.varDecl);
		TypeDenoter leftType = stmt.varDecl.visit(this, arg);
		TypeDenoter rightType = stmt.initExp.visit(this, arg);
		if (leftType.typeKind == TypeKind.NULL) {
			return null;
		}
		if (rightType.typeKind == TypeKind.NULL) {
			return null;
		}
		if (leftType.typeKind != rightType.typeKind) {
			reportTypeError(stmt, "Types do not match for VarDecl: " + stmt.varDecl.name);
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		if (leftType.typeKind == TypeKind.ARRAY) {
			if (((ArrayType)leftType).eltType.typeKind != ((ArrayType)rightType).eltType.typeKind) {
				reportTypeError(stmt, "Types do not match for VarDecl Array: " + stmt.varDecl.name);
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			else if (((ArrayType)leftType).eltType.typeKind == TypeKind.CLASS && !(((ClassType) ((ArrayType) leftType).eltType).className.spelling).equals((((ClassType) ((ArrayType) rightType).eltType).className.spelling))) {
				reportTypeError(stmt, "Types do not match for VarDecl Array of class type: " + stmt.varDecl.name);
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		if (leftType.typeKind == TypeKind.CLASS && !stmt.varDecl.classn.equals(((ClassType) rightType).className.spelling)) {
			reportTypeError(stmt, "Types do not match for VarDecl Array: " + stmt.varDecl.name);
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		this.qref = false;
		TypeDenoter leftType = stmt.ref.visit(this, arg);
		this.helperClass = null;
		TypeDenoter rightType = stmt.val.visit(this, arg);
		this.helperClass = null;
		if (leftType.typeKind == TypeKind.NULL) {
			return null;
		}
		if (rightType.typeKind == TypeKind.NULL) {
			return null;
		}

		if (leftType.typeKind != rightType.typeKind) {
			reportTypeError(stmt, "Types do not match for AssignStmt: " + stmt.ref.toString());
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		if (leftType.typeKind == TypeKind.ARRAY) {
			if (((ArrayType)leftType).eltType.typeKind != ((ArrayType)rightType).eltType.typeKind) {
				reportTypeError(stmt, "Types do not match for AssignStmt Array: " + stmt.ref.toString());
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			else if (((ArrayType)leftType).eltType.typeKind == TypeKind.CLASS && !(((ClassType) ((ArrayType) leftType).eltType).className.spelling).equals((((ClassType) ((ArrayType) rightType).eltType).className.spelling))) {
				reportTypeError(stmt, "Types do not match for AssignStmt Arra of class type: " + stmt.ref.toString());
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		if (leftType.typeKind == TypeKind.CLASS && !((ClassType) leftType).className.spelling.equals(((ClassType) rightType).className.spelling)) {
			reportTypeError(stmt, "Types do not match for AssignStmt Array: " + stmt.ref.toString());
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		this.qref = false;
		TypeDenoter type = stmt.ref.visit(this, arg);
		this.helperClass = null;
		TypeDenoter ixexp = stmt.ix.visit(this, arg);
		this.helperClass = null;
		TypeDenoter exp = stmt.exp.visit(this, arg);
		this.helperClass = null;
		if (ixexp.typeKind != TypeKind.INT) {
			reportTypeError(stmt, "Expression inside of brackets must be integer");
		}
		if (exp.typeKind == TypeKind.ARRAY) {
			exp = ((ArrayType) exp).eltType;
		}
		if (type.typeKind != TypeKind.ARRAY) {
			reportTypeError(stmt, "Incorrect type (array not seen)");
		}
		if (exp.typeKind != ((ArrayType) type).eltType.typeKind) {
			reportTypeError(stmt, "ArrayType Assignment issue");
		}
		if (type.typeKind == TypeKind.CLASS && !(((ClassType) type).className.spelling.equals(((ClassType) exp).className.spelling))) {
			reportTypeError(stmt, "ArrayType Assignment issue");
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		this.qref = false;
		TypeDenoter type = stmt.methodRef.visit(this, arg);
		this.helperClass = null;
		MethodDecl methodDecl = this.currMethodDecl;
		if (methodDecl != null) {
			ParameterDeclList pdl = methodDecl.parameterDeclList;
			if (methodDecl != null) {
				if (pdl.size() == stmt.argList.size()) {
					for (int i = 0; i < stmt.argList.size(); i++) {
						TypeDenoter leftType = pdl.get(i).visit(this, arg);
						TypeDenoter rightType = stmt.argList.get(i).visit(this, arg);
						if (leftType.typeKind != rightType.typeKind) {
							reportTypeError(stmt, "Types do not match in call statement");
						}
						if (leftType.typeKind == TypeKind.ARRAY) {
							if (((ArrayType)leftType).eltType.typeKind != ((ArrayType)rightType).eltType.typeKind) {
								reportTypeError(stmt, "Types do not match for Call Statement Array");
							}
							else if (((ArrayType)leftType).eltType.typeKind == TypeKind.CLASS && !(((ClassType) ((ArrayType) leftType).eltType).className.spelling).equals((((ClassType) ((ArrayType) rightType).eltType).className.spelling))) {
								reportTypeError(stmt, "Types do not match for Call Statement Array of class type");
							}
						}
						if (leftType.typeKind == TypeKind.CLASS && !((ClassType) leftType).className.spelling.equals(((ClassType) rightType).className.spelling)) {
							reportTypeError(stmt, "Class Types do not match for Call Statement");
						}
					}
				}
				else {
					reportTypeError(stmt, "Number of arguments does not match number of params");
				}
			}
		}
		return type;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			returnExists = true;
			return stmt.returnExpr.visit(this, arg);
		}
		else {
			return new BaseType(TypeKind.NULL, null);
		}
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		TypeDenoter type = stmt.cond.visit(this, arg);
		if (type.typeKind != TypeKind.BOOLEAN) {
			reportTypeError(stmt, "if condition must be of type boolean");
		}


		TypeDenoter elseType = stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, arg);
		}
		return elseType;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeDenoter type = stmt.cond.visit(this, arg);
		if (type.typeKind != TypeKind.BOOLEAN) {
			reportTypeError(stmt, "while condition must be of type boolean");
		}
		return stmt.body.visit(this, arg);
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		TypeDenoter type = expr.expr.visit(this, arg);
		if (expr.operator.kind == TokenType.UNOP) {
			if (type.typeKind != TypeKind.BOOLEAN) {
				reportTypeError(expr, "Must use type boolean with !");
			}
			return new BaseType(TypeKind.BOOLEAN, null);
		}
		else {
			if (type.typeKind != TypeKind.INT) {
				reportTypeError(expr, "Must use type int with -");
			}
			return new BaseType(TypeKind.INT, null);
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeDenoter leftType = expr.left.visit(this, arg);
		TypeDenoter rightType = expr.right.visit(this, arg);
		if (expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||")) {
			if (leftType.typeKind == TypeKind.BOOLEAN && rightType.typeKind == TypeKind.BOOLEAN) {
				return new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				reportTypeError(expr, "For &&,|| both sides must be type boolean");
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		else if (expr.operator.spelling.equals(">") || expr.operator.spelling.equals(">=") || expr.operator.spelling.equals("<") || expr.operator.spelling.equals("<=")) {
			if (leftType.typeKind == TypeKind.INT && rightType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				reportTypeError(expr, "For >,>=,<,<= both sides must be type int");
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		else if (expr.operator.spelling.equals("+") || expr.operator.spelling.equals("-") || expr.operator.spelling.equals("/") || expr.operator.spelling.equals("*")) {
			if (leftType.typeKind == TypeKind.INT && rightType.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, null);
			}
			else {
				reportTypeError(expr, "For +,-,/,* both sides must be type int");
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		else if (expr.operator.spelling.equals("==") || expr.operator.spelling.equals("!=")) {
			if (leftType.typeKind != rightType.typeKind) {
				reportTypeError(expr, "For ==,!= both sides types must match");
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
			else {
				if (leftType.typeKind != TypeKind.ARRAY && leftType.typeKind != TypeKind.CLASS) {
					return new BaseType(TypeKind.BOOLEAN, null);
				}
				if (leftType.typeKind == TypeKind.ARRAY) {
					if (((ArrayType)leftType).eltType.typeKind != ((ArrayType)rightType).eltType.typeKind) {
						reportTypeError(expr, "Types do not match for Call Statement Array");
						return new BaseType(TypeKind.UNSUPPORTED, null);
					}
					else if (((ArrayType)leftType).eltType.typeKind == TypeKind.CLASS && !(((ClassType) ((ArrayType) leftType).eltType).className.spelling).equals((((ClassType) ((ArrayType) rightType).eltType).className.spelling))) {
						reportTypeError(expr, "Types do not match for Call Statement Array of class type");
						return new BaseType(TypeKind.UNSUPPORTED, null);
					}
					return new BaseType(TypeKind.UNSUPPORTED, null);
				}
				if (leftType.typeKind == TypeKind.CLASS && !((ClassType) leftType).className.spelling.equals(((ClassType) rightType).className.spelling)) {
					reportTypeError(expr, "Class Types do not match for Call Statement");
					return new BaseType(TypeKind.UNSUPPORTED, null);
				}
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		else {
			return new BaseType(TypeKind.BOOLEAN, null);
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		this.qref = false;
		TypeDenoter temp = expr.ref.visit(this, arg);
		this.helperClass = null;
		return temp;
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		this.qref = false;
		TypeDenoter type = expr.ref.visit(this, arg);
		this.helperClass = null;
		TypeDenoter ixtype = expr.ixExpr.visit(this, arg);
		this.helperClass = null;

		if (ixtype.typeKind != TypeKind.INT) {
			reportTypeError(ixtype, "Index Expression must be of type int");
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		if (type.typeKind != TypeKind.ARRAY) {
			reportTypeError(type, "Must be of type array for ix");
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return ((ArrayType) type).eltType;
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		this.qref = false;
		this.isCallExpr = true;
		TypeDenoter type = expr.functionRef.visit(this, arg);
		this.isCallExpr = false;
		this.helperClass = null;
		MethodDecl methodDecl = this.currMethodDecl;
		if (methodDecl != null) {
			if (methodDecl.parameterDeclList.size() != expr.argList.size()) {
				reportTypeError(methodDecl, "Number of params does not match argument list length");
			}
			else {
				for (int i = 0; i < expr.argList.size(); i++) {
					TypeDenoter leftType = methodDecl.parameterDeclList.get(i).visit(this, arg);
					TypeDenoter rightType = expr.argList.get(i).visit(this, arg);
					if (leftType.typeKind != rightType.typeKind) {
						reportTypeError(expr, "Types do not match in call statement");
					}
					if (leftType.typeKind == TypeKind.ARRAY) {
						if (((ArrayType)leftType).eltType.typeKind != ((ArrayType)rightType).eltType.typeKind) {
							reportTypeError(expr, "Types do not match for Call Statement Array");
						}
						else if (((ArrayType)leftType).eltType.typeKind == TypeKind.CLASS && !(((ClassType) ((ArrayType) leftType).eltType).className.spelling).equals((((ClassType) ((ArrayType) rightType).eltType).className.spelling))) {
							reportTypeError(expr, "Types do not match for Call Statement Array of class type");
						}
					}
					if (leftType.typeKind == TypeKind.CLASS && !((ClassType) leftType).className.spelling.equals(((ClassType) rightType).className.spelling)) {
						reportTypeError(expr, "Class Types do not match for Call Statement");
					}
				}
			}
		}
		return type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, arg);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return expr.classtype;
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		TypeDenoter type = expr.eltType;
		TypeDenoter sizeType = expr.sizeExpr.visit(this, arg);
		if (type.typeKind != TypeKind.INT && type.typeKind != TypeKind.CLASS) {
			reportTypeError(type, "Array Type must be int or class");
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		if (sizeType.typeKind != TypeKind.INT) {
			reportTypeError(sizeType, "Expression inside [] must be int");
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return new ArrayType(type, null);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		return new ClassType(new Identifier(new Token(TokenType.ID, this.currentClass)), null);
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		//System.out.println(this.helperClass);
		if (this.helperClass == null) {
			for (Declaration d : this.local.values()) {
				if (d.name.equals(ref.id.spelling)) {
					this.helperClass = this.local.get(d.name).type.classn;
					return this.local.get(d.name).type;
				}
			}

			for (Declaration d : this.idTable.get(0).values()) {
				if (((ClassDecl) d).name.equals(this.currentClass)) {
					for (FieldDecl fd : ((ClassDecl) d).fieldDeclList) {
						if (fd.name.equals(ref.id.spelling)) {
							this.helperClass = fd.classn;
							return fd.type;
						}
					}
					for (MethodDecl md : ((ClassDecl) d).methodDeclList) {
						if (md.name.equals(ref.id.spelling)) {
							this.helperClass = md.classn;
							return md.type;
						}
					}
				}
			}
	
			if (this.idTable.get(0).containsKey(ref.id.spelling)) {
				this.helperClass = ref.id.spelling;
				return new ClassType(ref.id, null);
			}
			//System.out.println(ref.id.spelling);

			return new BaseType(TypeKind.UNSUPPORTED, null);
		} else {
			for (Declaration d : this.idTable.get(0).values()) {
				if (((ClassDecl) d).name.equals(this.helperClass)) {
					for (FieldDecl fd : ((ClassDecl) d).fieldDeclList) {
						if (fd.name.equals(ref.id.spelling)) {
							this.helperClass = fd.classn;
							return fd.type;
						}
					}
					for (MethodDecl md : ((ClassDecl) d).methodDeclList) {
						if (md.name.equals(ref.id.spelling)) {
							this.helperClass = md.classn;
							return md.type;
						}
					}
					break;
				}
			}
	
			// if (this.idTable.get(0).containsKey(ref.id.spelling)) {
			// 	this.helperClass = ref.id.spelling;
			// 	return new ClassType(ref.id, null);
			// }
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		this.qref = true;
		TypeDenoter refType = ref.ref.visit(this, arg);
		//System.out.println(this.helperClass);
		TypeDenoter idType = ref.id.visit(this, arg);
		if (refType.typeKind != TypeKind.CLASS) {
			reportTypeError(ref, "Reference must be of class type");
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return idType;
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		if (this.helperClass == null) {
			for (Declaration d : this.local.values()) {
				if (d.name.equals(id.spelling)) {
					this.helperClass = this.local.get(d.name).type.classn;
					return this.local.get(d.name).type;
				}
			}

			for (Declaration d : this.idTable.get(0).values()) {
				if (((ClassDecl) d).name.equals(this.currentClass)) {
					for (FieldDecl fd : ((ClassDecl) d).fieldDeclList) {
						if (fd.name.equals(id.spelling)) {
							this.helperClass = fd.classn;
							return fd.type;
						}
					}
					for (MethodDecl md : ((ClassDecl) d).methodDeclList) {
						if (md.name.equals(id.spelling)) {
							this.helperClass = md.classn;
							return md.type;
						}
					}
				}
			}
	
			if (this.idTable.get(0).containsKey(id.spelling)) {
				this.helperClass = id.spelling;
				return new ClassType(id, null);
			}
			//System.out.println(id.spelling);

			return new BaseType(TypeKind.UNSUPPORTED, null);
		} else {
			for (Declaration d : this.idTable.get(0).values()) {
				if (((ClassDecl) d).name.equals(this.helperClass)) {
					for (FieldDecl fd : ((ClassDecl) d).fieldDeclList) {
						if (fd.name.equals(id.spelling)) {
							this.helperClass = fd.classn;
							return fd.type;
						}
					}
					for (MethodDecl md : ((ClassDecl) d).methodDeclList) {
						if (md.name.equals(id.spelling)) {
							this.helperClass = md.classn;
							return md.type;
						}
					}
					break;
				}
			}
	
			if (this.idTable.get(0).containsKey(id.spelling)) {
				this.helperClass = id.spelling;
				return new ClassType(id, null);
			}

			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nuLL, Object arg) {
		return new BaseType(TypeKind.NULL, null);
	}
}
