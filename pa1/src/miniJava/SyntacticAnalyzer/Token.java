package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	private SourcePosition source;
	
	public Token(TokenType type, String text) {
		// TODO: Store the token's type and text
		this._type = type;
		this._text = text;
		SourcePosition s = new SourcePosition(1, 1);
		this.source = s;
	}
	
	public TokenType getTokenType() {
		// TODO: Return the token type
		return this._type;
	}
	
	public String getTokenText() {
		// TODO: Return the token text
		return this._text;
	}

	public SourcePosition getTokenPosition() {
		return null;
	}
}
