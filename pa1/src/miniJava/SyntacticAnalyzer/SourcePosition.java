package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    private int line = 1;
    private int col = 1;
    
    public SourcePosition(int line, int col) {
        this.line = line;
        this.col = col;
    }

    public String toString() {
        return "Error occurred at line: " + line + " col: " + col;
    }
}
