package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

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
	
	public void parse() {
		try {
			// The first thing we need to parse is the Program symbol
			parseProgram();
		} catch( SyntaxError e ) { 
			
		}
	}
	
	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		// TODO: Keep parsing class declarations until eot
		while (_currentToken.getTokenType() != TokenType.EOT && _currentToken != null) {
			parseClassDeclaration();
		}
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private void parseClassDeclaration() throws SyntaxError {
		// TODO: Take in a "class" token (check by the TokenType)
		//  What should be done if the first token isn't "class"?
		accept(TokenType.CLASS);
		// TODO: Take in an identifier token
		accept(TokenType.ID);
		// TODO: Take in a {
		accept(TokenType.LCURLY);
		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		parseFieldMethodDeclaration();
		// TODO: Take in a }
		accept(TokenType.RCURLY);
	}
	
	private void parseFieldMethodDeclaration() throws SyntaxError {
		if (TokenType.PUBLIC == _currentToken.getTokenType()) {
			accept(TokenType.PUBLIC);
		}
		else if (TokenType.PRIVATE == _currentToken.getTokenType()) {
			accept(TokenType.PRIVATE);
		}
		
		if (TokenType.STATIC == _currentToken.getTokenType()) {
			accept(TokenType.STATIC);
		}
		
		if (TokenType.VOID == _currentToken.getTokenType()) {
			accept(TokenType.VOID);
		}
		else if (TokenType.BOOL == _currentToken.getTokenType()) {
			accept(TokenType.BOOL);
		}
		else if (TokenType.INT == _currentToken.getTokenType()) {
			accept(TokenType.INT);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}
		else if (TokenType.ID == _currentToken.getTokenType()) {
			accept(TokenType.ID);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}
		else {
			accept(TokenType.ERROR);
		}
		
		accept(TokenType.ID);
		
		if (TokenType.SEMICOLON == _currentToken.getTokenType()) {
			accept(TokenType.SEMICOLON);
		}
		else {
			accept(TokenType.LPAREN);
			if (TokenType.RPAREN != _currentToken.getTokenType()) {
				parseParams();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.LCURLY);
			if (TokenType.RCURLY != _currentToken.getTokenType()) {
				parseStatement();
			}
			accept(TokenType.RCURLY);
		}	
	}
	
	private void parseParams() throws SyntaxError {
		if (TokenType.BOOL == _currentToken.getTokenType()) {
			accept(TokenType.BOOL);
		}
		else if (TokenType.INT == _currentToken.getTokenType()) {
			accept(TokenType.INT);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}
		else if (TokenType.ID == _currentToken.getTokenType()) {
			accept(TokenType.ID);
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
			}
		}
		else {
			accept(TokenType.ERROR);
		}
		
		accept(TokenType.ID);
		
		while (TokenType.RPAREN != _currentToken.getTokenType()) {
			accept(TokenType.COMMA);
			
			if (TokenType.BOOL == _currentToken.getTokenType()) {
				accept(TokenType.BOOL);
			}
			else if (TokenType.INT == _currentToken.getTokenType()) {
				accept(TokenType.INT);
				if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					accept(TokenType.LBRACKET);
					accept(TokenType.RBRACKET);
				}
			}
			else if (TokenType.ID == _currentToken.getTokenType()) {
				accept(TokenType.ID);
				if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					accept(TokenType.LBRACKET);
					accept(TokenType.RBRACKET);
				}
			}
			else {
				accept(TokenType.ERROR);
			}
			
			accept(TokenType.ID);
		}
	}
	
	private void parseStatement() throws SyntaxError {
		while (TokenType.RCURLY != _currentToken.getTokenType()) {
			if (TokenType.WHILE == _currentToken.getTokenType()) {
				accept(TokenType.WHILE);
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				if (TokenType.LCURLY == _currentToken.getTokenType()) {
					accept(TokenType.LCURLY);
					while (TokenType.RCURLY != _currentToken.getTokenType()) {
						parseStatement();
					}
					accept(TokenType.RCURLY);
				}
				else {
					parseStatement();
				}
			}
			else if (TokenType.IF == _currentToken.getTokenType()) {
				accept(TokenType.IF);
				accept(TokenType.LPAREN);
				parseExpression();
				accept(TokenType.RPAREN);
				if (TokenType.LCURLY == _currentToken.getTokenType()) {
					accept(TokenType.LCURLY);
					while (TokenType.RCURLY != _currentToken.getTokenType()) {
						parseStatement();
					}
					accept(TokenType.RCURLY);
				}
				else {
					parseStatement();
				}
				
				if (TokenType.ELSE == _currentToken.getTokenType()) {
					accept(TokenType.ELSE);
					
					if (TokenType.LCURLY == _currentToken.getTokenType()) {
						accept(TokenType.LCURLY);
						while (TokenType.RCURLY != _currentToken.getTokenType()) {
							parseStatement();
						}
						accept(TokenType.RCURLY);
					}
					else {
						parseStatement();
					}
				}
			}
			else if (TokenType.RETURN == _currentToken.getTokenType()) {
				accept(TokenType.RETURN);
				if (TokenType.SEMICOLON != _currentToken.getTokenType()) {
					parseExpression();
				}
				accept(TokenType.SEMICOLON);
			}
			else if (TokenType.ID == _currentToken.getTokenType()) {
				accept(TokenType.ID);
				if (TokenType.ID == _currentToken.getTokenType()) {
					if (TokenType.LBRACKET == _currentToken.getTokenType()) {
						accept(TokenType.LBRACKET);
						accept(TokenType.RBRACKET);
					}
					accept(TokenType.ID);
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				}
				else if (TokenType.EQUALS == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				}
				else if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.LBRACKET);
					parseExpression();
					accept(TokenType.RBRACKET);
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				}
				else if (TokenType.LPAREN == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.LPAREN);
					if (TokenType.RPAREN != _currentToken.getTokenType()) {
						parseArgList();
					}
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);
				}
				else {
					accept(TokenType.ERROR);
				}
			}
			else if (TokenType.THIS == _currentToken.getTokenType()) {
				accept(TokenType.THIS);
				if (TokenType.EQUALS == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				}
				else if (TokenType.LBRACKET == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.LBRACKET);
					parseExpression();
					accept(TokenType.RBRACKET);
					accept(TokenType.EQUALS);
					parseExpression();
					accept(TokenType.SEMICOLON);
				}
				else if (TokenType.LPAREN == _currentToken.getTokenType()) {
					if (TokenType.DOT == _currentToken.getTokenType()) {
						accept(TokenType.DOT);
						accept(TokenType.ID);
					}
					accept(TokenType.LPAREN);
					if (TokenType.RPAREN != _currentToken.getTokenType()) {
						parseArgList();
					}
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);
				}
				else {
					accept(TokenType.ERROR);
				}
			}
			else if (TokenType.INT == _currentToken.getTokenType() || TokenType.BOOL == _currentToken.getTokenType()) {
				if (TokenType.INT == _currentToken.getTokenType()) {
					accept(TokenType.INT);
					if (TokenType.LBRACKET == _currentToken.getTokenType()) {
						accept(TokenType.LBRACKET);
						accept(TokenType.RBRACKET);
					}
				}
				else {
					accept(TokenType.BOOL);
				}
				accept(TokenType.ID);
				accept(TokenType.EQUALS);
				parseExpression();
				accept(TokenType.SEMICOLON);
			}
			else {
				accept(TokenType.ERROR);
			}
		}
	}
	
	private void parseExpression() throws SyntaxError {
		if (TokenType.BOOLVALUE == _currentToken.getTokenType()) {
			accept(TokenType.BOOLVALUE);
		}
		else if (TokenType.NUM == _currentToken.getTokenType()) {
			accept(TokenType.NUM);
		}
		else if (TokenType.NEW == _currentToken.getTokenType()) {
			accept(TokenType.NEW);
			if (TokenType.ID == _currentToken.getTokenType()) {
				if (TokenType.LPAREN == _currentToken.getTokenType()) {
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
				}
				else {
					accept(TokenType.LBRACKET);
					parseExpression();
					accept(TokenType.RBRACKET);
				}
			}
			else if (TokenType.INT == _currentToken.getTokenType()) {
				accept(TokenType.INT);
				accept(TokenType.LBRACKET);
				parseExpression();
				accept(TokenType.RBRACKET);
			}
			else {
				accept(TokenType.ERROR);
			}
		}
		else if (TokenType.LPAREN == _currentToken.getTokenType()) {
			accept(TokenType.LPAREN);
			parseExpression();
			accept(TokenType.RPAREN);
		}
		else if (TokenType.UNOP == _currentToken.getTokenType() || TokenType.SUB == _currentToken.getTokenType()) {
			if (TokenType.UNOP == _currentToken.getTokenType()) {
				accept(TokenType.UNOP);
			}
			else {
				accept(TokenType.SUB);
			}
			parseExpression();
		}
		else if (TokenType.THIS == _currentToken.getTokenType() || TokenType.ID == _currentToken.getTokenType()) {
			if (TokenType.THIS == _currentToken.getTokenType()) {
				accept(TokenType.THIS);
			}
			else {
				accept(TokenType.ID);
			}
			if (TokenType.DOT == _currentToken.getTokenType()) {
				accept(TokenType.DOT);
				accept(TokenType.ID);
			}
			
			if (TokenType.LBRACKET == _currentToken.getTokenType()) {
				accept(TokenType.LBRACKET);
				parseExpression();
				accept(TokenType.RBRACKET);
			}
			else if (TokenType.LPAREN == _currentToken.getTokenType()) {
				accept(TokenType.LPAREN);
				if (TokenType.RPAREN != _currentToken.getTokenType()) {
					parseArgList();
				}
				accept(TokenType.RPAREN);
			}
		}
		
		if (TokenType.BINOP == _currentToken.getTokenType() || TokenType.SUB == _currentToken.getTokenType()) {
			if (TokenType.BINOP == _currentToken.getTokenType()) {
				accept(TokenType.BINOP);
			}
			else {
				accept(TokenType.SUB);
			}
			parseExpression();
		}
	}
	
	private void parseArgList() throws SyntaxError {
		parseExpression();
		
		while (TokenType.RPAREN != _currentToken.getTokenType()) {
			accept(TokenType.COMMA);
			parseExpression();
		}
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
		_errors.reportError("Got token " + _currentToken.getTokenText());
		throw new SyntaxError();
	}
}
