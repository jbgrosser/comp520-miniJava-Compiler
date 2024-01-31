package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private boolean eot = false;
	
	private final static char eolUnix = '\n';
	private final static char eolWindows = '\r';
	private final static char tab = '\t';
	
	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		
		if (!eot) {
			nextChar();
		}
	}
	
	public Token scan() {
		// TODO: This function should check the current char to determine what the token could be.
		if (eot) {
			return null;
		}
			
		// TODO: Consider what happens if the current char is whitespace
		skipWhitespace();
		// TODO: Consider what happens if there is a comment (// or /* */)
		
		// TODO: What happens if there are no more tokens?
		
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
		_currentText = new StringBuilder();
		TokenType token = scanToken();
		return makeToken(token);
	}
	
	public void skipWhitespace() {
		while ((_currentChar == ' ' || _currentChar == tab || _currentChar == eolUnix || _currentChar == eolWindows) && !eot) {
			skipIt();
		}
	}
	
	public void skipMultiComment() {
		skipIt();
		while((_currentChar != '/' || _currentText.charAt(_currentText.length() - 1) != '*' ) && !eot) {
			takeIt();
		}
		takeIt();
		skipWhitespace();

		_currentText = new StringBuilder();
	}
	
	public void skipSingleComment() {
		_currentText.deleteCharAt(_currentText.length() - 1);
		while ((_currentChar != eolUnix && _currentChar != eolWindows) && !eot) {
			skipIt();
		}
		skipWhitespace();
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}

	private void skipIt() {
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char)c;
			
			// TODO: What happens if c == -1?
			// TODO: What happens if c is not a regular ASCII character?
			if (c == -1) {
				eot = true;
			}
			else if (c < 0 || c >= 128) {
				throw new IOException("Not an ASCII char!");
			}
		} catch( IOException e ) {
			// TODO: Report an error here
			_errors.reportError("Scan Error: I/O Exception!");
		}
	}
	
	private Token makeToken( TokenType toktype) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		return new Token(toktype, _currentText.toString());
	}
	
	public TokenType scanToken() {
		
		if (eot) 
			return(TokenType.EOT); 

		// scan Token
		switch (_currentChar) {
		case '[':
			takeIt();
			return(TokenType.LBRACKET);

		case ']':
			takeIt();
			return(TokenType.RBRACKET);

		case '(': 
			takeIt();
			return(TokenType.LPAREN);

		case ')':
			takeIt();
			return(TokenType.RPAREN);
			
		case '{':
			takeIt();
			return(TokenType.LCURLY);

		case '}':
			takeIt();
			return(TokenType.RCURLY);
			
		case ';':
			takeIt();
			return(TokenType.SEMICOLON);
			
		case '=':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
				return(TokenType.BINOP);
			}
			return(TokenType.EQUALS);

		case '!':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
				return(TokenType.BINOP);
			}
			return(TokenType.UNOP);
			
		case '>':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
			}
			return(TokenType.BINOP);
			
		case '<':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
			}
			return(TokenType.BINOP);
			
		case '-':
			takeIt();
			return(TokenType.SUB);
			
		case '+':
			takeIt();
			return(TokenType.BINOP);
			
		case '/':
			takeIt();
			if (_currentChar == '/') {
				skipSingleComment();
				return scanToken();
			}
			else if (_currentChar == '*') {
				skipMultiComment();
				return scanToken();
			}
			else {
				return(TokenType.BINOP);
			}
			
		case '*':
			takeIt();
			return(TokenType.BINOP);
			
		case '.':
			takeIt();
			return(TokenType.DOT);

		case ',':
			takeIt();
			return(TokenType.COMMA);
			
		case '|':
			takeIt();
			if (_currentChar == '|') {
				takeIt();
				return(TokenType.BINOP);
			}
			_errors.reportError("Scan Error: Single | not allowed");
			return(TokenType.ERROR);
			
		case '&':
			takeIt();
			if (_currentChar == '&') {
				takeIt();
				return(TokenType.BINOP);
			}
			_errors.reportError("Scan Error: Single & not allowed");
			return(TokenType.ERROR);
		
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			while (isDigit(_currentChar))
				takeIt();
			return(TokenType.NUM);
			
		default:
			while (!eot && (Character.isLetter(_currentChar) || Character.isDigit(_currentChar) || _currentChar == '_')) {
				takeIt();
			}
			
			switch (_currentText.toString()) {
			case "class":
				return(TokenType.CLASS);
			case "public":
				return(TokenType.PUBLIC);
				
			case "private": 
				return(TokenType.PRIVATE);
				
			case "static":
				return(TokenType.STATIC);
				
			case "void":
				return(TokenType.VOID);

			case "return":
				return(TokenType.RETURN);
				
			case "int":
				return(TokenType.INT);
				
			case "boolean":
				return(TokenType.BOOL);
				
			case "this":
				return(TokenType.THIS);
				
			case "while":
				return(TokenType.WHILE);
			
			case "if":
				return(TokenType.IF);
		
			case "else":
				return(TokenType.ELSE);
				
			case "new":
				return(TokenType.NEW);
				
			case "true": case "false":
				return(TokenType.BOOLVALUE);
				
			default:				
				if (_currentText.length() == 0 || !Character.isLetter(_currentText.charAt(0))) {
					_errors.reportError("Scan Error: ID is not valid");
					return(TokenType.ERROR);
				}
				return(TokenType.ID);
			}
		}		
	}
	
	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}
}
