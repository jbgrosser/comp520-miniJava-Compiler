package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public AST parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parseProgram();
		} catch( SyntaxError e ) { 
			return null;
		}
	}
	
	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		ClassDeclList classes = new ClassDeclList();
		while (_currentToken != null && _currentToken.getTokenType() != TokenType.EOT) {
			classes.add(parseClassDeclaration());
		}
		return new Package(classes, null);
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?
		accept(TokenType.CLASS);
		ClassDecl classD = new ClassDecl(_currentToken.getTokenText(), new FieldDeclList(), new MethodDeclList(), null);
		// TODO: Take in an identifier token
		accept(TokenType.ID);
		// TODO: Take in a {
		accept(TokenType.LCURLY);
		StatementList statements = new StatementList();

		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		while (TokenType.RCURLY != _currentToken.getTokenType()) {
			FieldDecl field = null;
			MethodDecl method = null;
			boolean isStatic = false;
			boolean isPrivate = false;
			ParameterDeclList params = new ParameterDeclList();

			boolean isMethod = false;
			if (TokenType.PUBLIC == _currentToken.getTokenType()) {
				accept(TokenType.PUBLIC);
			}
			else if (TokenType.PRIVATE == _currentToken.getTokenType()) {
				accept(TokenType.PRIVATE);
				isPrivate = true;
			}
			
			if (TokenType.STATIC == _currentToken.getTokenType()) {
				accept(TokenType.STATIC);
				isStatic = true;
			}
			
			TypeDenoter type = null;
			if (TokenType.VOID == _currentToken.getTokenType()) {
				accept(TokenType.VOID);
				isMethod = true;
				type = new BaseType(TypeKind.VOID, null);
			}
			else if (TokenType.BOOL == _currentToken.getTokenType()) {
				accept(TokenType.BOOL);
				type = new BaseType(TypeKind.BOOLEAN, null);
			}
			else if (TokenType.INT == _currentToken.getTokenType()) {
				accept(TokenType.INT);
				if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					accept(TokenType.LBRACKET);
					accept(TokenType.RBRACKET);
					TypeDenoter intType = new BaseType(TypeKind.INT, null);
					type = new ArrayType(intType, null);
				}
				else {
					type = new BaseType(TypeKind.INT, null);
				}
			}
			else if (TokenType.ID == _currentToken.getTokenType()) {
				accept(TokenType.ID);
				if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					accept(TokenType.LBRACKET);
					accept(TokenType.RBRACKET);
					TypeDenoter classType = new ClassType(new Identifier(_currentToken), null);
					type = new ArrayType(classType, null);
				}
				else {
					type = new ClassType(new Identifier(_currentToken), null);
				}
			}
			else {
				accept(TokenType.ERROR);
				type = new BaseType(TypeKind.UNSUPPORTED, null);
			}
			
			String identifier = _currentToken.getTokenText();
			accept(TokenType.ID);

			if (TokenType.SEMICOLON == _currentToken.getTokenType() && !isMethod) {
				field = new FieldDecl(isPrivate, isStatic, type, identifier, null);
				accept(TokenType.SEMICOLON);
				classD.fieldDeclList.add(field);
			}
			else {
				accept(TokenType.LPAREN);
				if (TokenType.RPAREN != _currentToken.getTokenType()) {
					params = parseParams();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.LCURLY);
				while (TokenType.RCURLY != _currentToken.getTokenType()) {
					statements.add(parseStatement());
				}
				field = new FieldDecl(isPrivate, isStatic, type, identifier, null);
				method = new MethodDecl(field, params, statements, null);
				accept(TokenType.RCURLY);
				classD.methodDeclList.add(method);
			}	
		}
		
		// TODO: Take in a }
		accept(TokenType.RCURLY);
		return classD;
	}
	
	private TypeDenoter parseType() throws SyntaxError {
		if (TokenType.BOOL == _currentToken.getTokenType()) {
			accept(TokenType.BOOL);
			return new BaseType(TypeKind.BOOLEAN, null);
		}
		else if (TokenType.INT == _currentToken.getTokenType()) {
			accept(TokenType.INT);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
				TypeDenoter intType = new BaseType(TypeKind.INT, null);
				return new ArrayType(intType, null);
			}
			else {
				return new BaseType(TypeKind.INT, null);
			}
		}
		else if (TokenType.ID == _currentToken.getTokenType()) {
			accept(TokenType.ID);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
				TypeDenoter classType = new ClassType(new Identifier(_currentToken), null);
				return new ArrayType(classType, null);
			}
			else {
				return new ClassType(new Identifier(_currentToken), null);
			}
		}
		else {
			accept(TokenType.ERROR);
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
	}

	private ParameterDeclList parseParams() throws SyntaxError {
		ParameterDeclList params = new ParameterDeclList();
		
		TypeDenoter type = parseType();
		ParameterDecl param = new ParameterDecl(type, _currentToken.getTokenText(), null);
		accept(TokenType.ID);
		params.add(param);
		
		while (TokenType.RPAREN != _currentToken.getTokenType()) {
			accept(TokenType.COMMA);
			
			type = parseType();
			param = new ParameterDecl(type, _currentToken.getTokenText(), null);
			accept(TokenType.ID);
			params.add(param);
		}
		return params;
	}
	
	private Statement parseStatement() throws SyntaxError {
		Expression expression = null;
		Token savedCurrentToken = _currentToken;
		if (TokenType.WHILE == _currentToken.getTokenType()) {
			Statement whileS = null;
			accept(TokenType.WHILE);
			accept(TokenType.LPAREN);
			expression = parseExpression();
			accept(TokenType.RPAREN);
			if (TokenType.LCURLY == _currentToken.getTokenType()) {
				accept(TokenType.LCURLY);
				StatementList statements = new StatementList();
				while (TokenType.RCURLY != _currentToken.getTokenType()) {
					statements.add(parseStatement());
				}
				whileS = new BlockStmt(null, null);
				accept(TokenType.RCURLY);
			}
			else {
				whileS = parseStatement();
			}
			return new WhileStmt(expression, whileS, null);
		}
		else if (TokenType.IF == _currentToken.getTokenType()) {
			Statement ifS = null;
			Statement elseS = null;
			accept(TokenType.IF);
			accept(TokenType.LPAREN);
			expression = parseExpression();
			accept(TokenType.RPAREN);
			if (TokenType.LCURLY == _currentToken.getTokenType()) {
				accept(TokenType.LCURLY);
				StatementList statements = new StatementList();
				while (TokenType.RCURLY != _currentToken.getTokenType()) {
					statements.add(parseStatement());
				}
				accept(TokenType.RCURLY);
			}
			else {
				ifS = parseStatement();
			}
			
			if (TokenType.ELSE == _currentToken.getTokenType()) {
				accept(TokenType.ELSE);
				
				if (TokenType.LCURLY == _currentToken.getTokenType()) {
					accept(TokenType.LCURLY);
					StatementList statements = new StatementList();
					while (TokenType.RCURLY != _currentToken.getTokenType()) {
						statements.add(parseStatement());
					}
					accept(TokenType.RCURLY);
				}
				else {
					elseS = parseStatement();
				}
			}
			return new IfStmt(expression, ifS, elseS, null);
		}
		else if (TokenType.RETURN == _currentToken.getTokenType()) {
			accept(TokenType.RETURN);
			if (TokenType.SEMICOLON != _currentToken.getTokenType()) {
				expression = parseExpression();
			}
			accept(TokenType.SEMICOLON);
			return new ReturnStmt(expression, null);
		}
		else if (TokenType.ID == _currentToken.getTokenType()) {
			Statement statement = null;
			Reference ref = new IdRef(new Identifier(savedCurrentToken), null);
			boolean reference = false;

			accept(TokenType.ID);

			while (TokenType.DOT == _currentToken.getTokenType()) {
				accept(TokenType.DOT);
				ref = new QualRef(ref, new Identifier(_currentToken), null);
				accept(TokenType.ID);
				reference = true;
			}

			if (!reference && TokenType.ID == _currentToken.getTokenType()) {
				Token temp = _currentToken;
				accept(TokenType.ID);
				accept(TokenType.EQUALS);
				Expression e = parseExpression();
				accept(TokenType.SEMICOLON);
				ClassType c = new ClassType(new Identifier(savedCurrentToken), null);
				statement = new VarDeclStmt(new VarDecl(c, temp.getTokenText(), null), e, null);
			}
			else if (TokenType.EQUALS == _currentToken.getTokenType()) {
				accept(TokenType.EQUALS);
				Expression equalExpression = parseExpression();
				accept(TokenType.SEMICOLON);
				statement = new AssignStmt(ref, equalExpression, null);
			}
			else if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				Expression e = null;
				if (TokenType.RBRACKET != _currentToken.getTokenType()) {
					e = parseExpression();
					accept(TokenType.RBRACKET);
				} 
				else {
					accept(TokenType.RBRACKET);
					accept(TokenType.ID);
				}
				accept(TokenType.EQUALS);
				statement = new IxAssignStmt(ref, e, parseExpression(), null);
				accept(TokenType.SEMICOLON);
			}
			else if (TokenType.LPAREN == _currentToken.getTokenType()) {
				ExprList expressions = new ExprList();
				accept(TokenType.LPAREN);
				if (TokenType.RPAREN != _currentToken.getTokenType()) {
					expressions = parseArgList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				statement = new CallStmt(ref, expressions, null);
			}
			else {
				accept(TokenType.ERROR);
			}
			return statement;
		}
		else if (TokenType.THIS == _currentToken.getTokenType()) {
			Statement statement = null;
			Reference ref = new ThisRef(null);
			accept(TokenType.THIS);
			while (TokenType.DOT == _currentToken.getTokenType()) {
				accept(TokenType.DOT);
				ref = new QualRef(ref, new Identifier(_currentToken), null);
				accept(TokenType.ID);
			}

			if (TokenType.EQUALS == _currentToken.getTokenType()) {
				accept(TokenType.EQUALS);
				Expression equalExpression = parseExpression();
				accept(TokenType.SEMICOLON);
				statement = new AssignStmt(ref, equalExpression, null);
			}
			else if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				Expression e = parseExpression();
				accept(TokenType.RBRACKET);
				accept(TokenType.EQUALS);
				statement = new IxAssignStmt(ref, e, parseExpression(), null);
				accept(TokenType.SEMICOLON);
			}
			else if (TokenType.LPAREN == _currentToken.getTokenType()) {
				ExprList expressions = new ExprList();
				accept(TokenType.LPAREN);
				if (TokenType.RPAREN != _currentToken.getTokenType()) {
					expressions = parseArgList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				statement = new CallStmt(ref, expressions, null);
			}
			else {
				accept(TokenType.ERROR);
			}
			return statement;
		}
		else if (TokenType.INT == _currentToken.getTokenType() || TokenType.BOOL == _currentToken.getTokenType()) {
			TypeDenoter type = null;
			if (TokenType.INT == _currentToken.getTokenType()) {
				accept(TokenType.INT);
				if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					accept(TokenType.LBRACKET);
					accept(TokenType.RBRACKET);
					TypeDenoter intType = new BaseType(TypeKind.INT, null);
					type = new ArrayType(intType, null);
				}
				else {
					type = new BaseType(TypeKind.INT, null);
				}
			}
			else {
				accept(TokenType.BOOL);
				type = new BaseType(TypeKind.BOOLEAN, null);
			}
			Token temp = _currentToken;
			accept(TokenType.ID);
			accept(TokenType.EQUALS);
			Expression e = parseExpression();
			accept(TokenType.SEMICOLON);
			return new VarDeclStmt(new VarDecl(type, temp.getTokenText(), null), e, null);
		}
		else {
			accept(TokenType.ERROR);
			return null;
		}	
	}
	
	private Expression parseExpression() throws SyntaxError {
		Token savedCurrentToken = _currentToken;
		Expression expression = null;

		if (TokenType.BOOLVALUE == _currentToken.getTokenType()) {
			accept(TokenType.BOOLVALUE);
			expression = new LiteralExpr(new BooleanLiteral(savedCurrentToken), null);
		}
		else if (TokenType.NUM == _currentToken.getTokenType()) {
			accept(TokenType.NUM);
			expression = new LiteralExpr(new IntLiteral(savedCurrentToken), null);
		}
		else if (TokenType.NEW == _currentToken.getTokenType()) {
			Expression e = null;
			accept(TokenType.NEW);
			if (TokenType.ID == _currentToken.getTokenType()) {
				accept(TokenType.ID);
				if (TokenType.LPAREN == _currentToken.getTokenType()) {
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
					e = new NewObjectExpr(new ClassType(new Identifier(savedCurrentToken), null), null);
				}
				else {
					accept(TokenType.LBRACKET);
					Expression exp = parseExpression();
					accept(TokenType.RBRACKET);
					e = new NewArrayExpr(new ClassType(new Identifier(savedCurrentToken), null), exp, null);
				}
			}
			else if (TokenType.INT == _currentToken.getTokenType()) {
				accept(TokenType.INT);
				accept(TokenType.LBRACKET);
				Expression exp = parseExpression();
				accept(TokenType.RBRACKET);
				e = new NewArrayExpr(new BaseType(TypeKind.INT, null), exp, null);
			}
			else {
				accept(TokenType.ERROR);
			}
			expression = e;
		}
		else if (TokenType.LPAREN == _currentToken.getTokenType()) {
			accept(TokenType.LPAREN);
			Expression e = parseExpression();
			accept(TokenType.RPAREN);
			expression = e;
		}
		else if (TokenType.UNOP == _currentToken.getTokenType() || TokenType.SUB == _currentToken.getTokenType()) {
			if (TokenType.UNOP == _currentToken.getTokenType()) {
				accept(TokenType.UNOP);
			}
			else {
				accept(TokenType.SUB);
			}
			Expression e = parseExpression();
			expression = new UnaryExpr(new Operator(savedCurrentToken), e, null);
		}
		else if (TokenType.THIS == _currentToken.getTokenType() || TokenType.ID == _currentToken.getTokenType()) {
			Reference reference = null;
			Expression e = null;
			if (TokenType.THIS == _currentToken.getTokenType()) {
				accept(TokenType.THIS);
				reference = new ThisRef(null);
			}
			else {
				accept(TokenType.ID);
				reference = new IdRef(new Identifier(savedCurrentToken), null);
			}

			if (TokenType.DOT == _currentToken.getTokenType()) {
				Token temp = _currentToken;
				accept(TokenType.DOT);
				accept(TokenType.ID);
				reference = new QualRef(reference, new Identifier(temp), null);
			}
			
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				e = parseExpression();
				accept(TokenType.RBRACKET);
				expression = new IxExpr(reference, e, null);
			}
			else if (TokenType.LPAREN == _currentToken.getTokenType()) {
				ExprList expressions = new ExprList();
				accept(TokenType.LPAREN);
				if (TokenType.RPAREN != _currentToken.getTokenType()) {
					expressions = parseArgList();
				}
				accept(TokenType.RPAREN);
				expression = new CallExpr(reference, expressions, null);
			}
			else {
				expression = new RefExpr(reference, null);
			}
		}
		else {
			accept(TokenType.ERROR);
			
		}
		
		if (TokenType.BINOP == _currentToken.getTokenType() || TokenType.SUB == _currentToken.getTokenType()) {
			Token temp = _currentToken;
			if (TokenType.BINOP == _currentToken.getTokenType()) {
				accept(TokenType.BINOP);
			}
			else {
				accept(TokenType.SUB);
			}
			Expression e = parseExpression();
			expression = new BinaryExpr(new Operator(temp), expression, e, null);
		}
		return expression;
	}
	
	private ExprList parseArgList() throws SyntaxError {
		ExprList expressions = new ExprList();
		expressions.add(parseExpression());
		
		while (TokenType.RPAREN != _currentToken.getTokenType()) {
			accept(TokenType.COMMA);
			expressions.add(parseExpression());
		}
		return expressions;
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			_currentToken = _scanner.scan();
			return;
		}
		
		// TODO: Report an error here.
		//  "Expected token X, but got Y"
		_errors.reportError("Got token " + _currentToken.getTokenText() + " expected " + expectedType);
		throw new SyntaxError();
	}
}
