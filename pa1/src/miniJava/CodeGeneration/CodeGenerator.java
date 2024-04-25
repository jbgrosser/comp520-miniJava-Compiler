package miniJava.CodeGeneration;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGenerator implements Visitor<Object, Object> {
	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section
	private HashMap<String, Integer> staticVariables = new HashMap<>();
	private Stack<Integer> methodOffsets = new Stack<>();
    private Stack<HashMap<String, Integer>> localOffsets = new Stack<>();
	private ExprList arguments = null;
	private boolean isAddressValue = false;
	
	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog) {
		_asm = new InstructionList();
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		_asm.markOutputStart();
		prog.visit(this,null);
		
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}

	class CodeGenerationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;
		
		public CodeGenerationError(AST ast, String errMsg) {
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

	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO: visit relevant parts of our AST
		boolean flag = false;
		int staticOffset = 0;
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.R8, Reg64.RBP)));
		for (ClassDecl cl : prog.classDeclList) {
			for (MethodDecl md : cl.methodDeclList) {
				if (flag) {
					throw new CodeGenerationError(prog, "Duplicate main methods found");
				}
				if (md.isPrivate == false && md.isStatic == true && md.type.typeKind == TypeKind.VOID
				 && md.name.equals("main") && md.parameterDeclList.size() == 1 && md.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY
				 && ((ArrayType) md.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS
				 && ((ClassType) ((ArrayType) md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) {
					flag = true;
				}
			}
		}
		if (!flag) {
			throw new CodeGenerationError(prog, "No valid main method found");
		}
		for (ClassDecl cl : prog.classDeclList) {
			for (FieldDecl fd : cl.fieldDeclList) {
				if (fd.isStatic) {
					this.staticVariables.put(fd.name, staticOffset);
					_asm.add(new Push(0));
					staticOffset -= 8;
				}
			}
		}

		for (ClassDecl cl : prog.classDeclList) {
			for (MethodDecl md : cl.methodDeclList) {
				if (md.isPrivate == false && md.isStatic == true && md.type.typeKind == TypeKind.VOID
				 && md.name.equals("main") && md.parameterDeclList.size() == 1 && md.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY
				 && ((ArrayType) md.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS
				 && ((ClassType) ((ArrayType) md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) {
					md.visit(this, null);
				}
			}
		}
		_asm.outputFromMark();
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 60));
		_asm.add(new Xor(new ModRMSIB(Reg64.RDI, Reg64.RDI)));
		_asm.add(new Syscall());
		return null;
	}
	
	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), 0); // TODO: set the location of the main method
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new ModRMSIB(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new ModRMSIB(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
	private int makePrintln() {
		// TODO: how can we generate the assembly to println?
		_asm.add(new Lea(new ModRMSIB(Reg64.RSP, 0, Reg64.RSI)));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDI, true), 1));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX, true), 1));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 1));
		_asm.add(new Syscall());
		_asm.add(new Pop(Reg64.RAX));
		return -1;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		this.localOffsets.push(new HashMap<>());
		this.methodOffsets.push(8);

		for (Statement s : md.statementList) {
			s.visit(this, null);
		}

		this.methodOffsets.pop();
		this.localOffsets.pop();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		for (Statement s : stmt.sl) {
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		this.localOffsets.peek().put(stmt.varDecl.name, this.methodOffsets.peek());
		int methodOffset = this.methodOffsets.pop();
		this.methodOffsets.push(methodOffset + 8);
		stmt.initExp.visit(this, null);
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		this.isAddressValue = false;
		stmt.ref.visit(this, null);
		this.isAddressValue = false;
		stmt.val.visit(this, null);
		_asm.add(new Pop(Reg64.RDX));

		if (this.isAddressValue) {
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RDX, 0, Reg64.RDX)));
		}

		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, 0, Reg64.RDX)));
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		this.arguments = stmt.argList;
		stmt.methodRef.visit(this, null);
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RDX));

		if (this.isAddressValue) {
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RDX, 0, Reg64.RDX)));
		}

		this.isAddressValue = false;
		_asm.add(new Cmp(new ModRMSIB(Reg64.RDX, true), 0));
		int beginningASMSize = _asm.getSize();
		int condJump = _asm.add(new CondJmp(Condition.E, 0, 0, false));
		stmt.thenStmt.visit(this, null);
		int endingASMSize = _asm.getSize();

		if (stmt.elseStmt != null) {
			int beginningASMSizeElse = _asm.getSize();
			int jump = _asm.add(new Jmp(0));
			endingASMSize = _asm.getSize();
			stmt.elseStmt.visit(this, null);
			int asmSizeElse = _asm.getSize();
			_asm.patch(jump, new Jmp(beginningASMSizeElse, asmSizeElse,false));
		}

		_asm.patch(condJump, new CondJmp(Condition.E, beginningASMSize, endingASMSize, false));
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		boolean leftAddressValue = false;
		expr.left.visit(this, null);
		leftAddressValue = this.isAddressValue;
		this.isAddressValue = false;
		expr.right.visit(this, null);
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RAX));

		if (leftAddressValue) {
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
		}
		if (this.isAddressValue) {
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RCX, 0, Reg64.RCX)));
		}
		this.isAddressValue = false;

		switch (expr.operator.spelling) {
			case "&&":
				_asm.add(new And(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				break;
			case "||":
				_asm.add(new Or(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				break;
			case "==":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.E, Reg8.AL));
				break;
			case "!=":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.NE, Reg8.AL));
				break;
			case ">":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.GT, Reg8.AL));
				break;
			case ">=":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.GTE, Reg8.AL));
				break;
			case "<":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.LT, Reg8.AL));
				break;
			case "<=":
				_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				_asm.add(new SetCond(Condition.LTE, Reg8.AL));
				break;
			case "*":
				_asm.add(new Imul(Reg64.RAX, new ModRMSIB(Reg64.RCX, true)));
				break;
			case "/":
				_asm.add(new Idiv(new ModRMSIB(Reg64.RCX, true)));
				break;
			case "+":
				_asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				break;
			case "-":
				_asm.add(new Sub(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
				break;
		}
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ixExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (this.localOffsets.peek().containsKey(ref.id.spelling)) {
			int offset = -this.localOffsets.peek().get(ref.id.spelling);
			_asm.add(new Lea(new ModRMSIB(Reg64.RBP, offset, Reg64.RAX)));
			_asm.add(new Push(Reg64.RAX));
		}
		this.isAddressValue = true;
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (ref.id.spelling.equals("println")) {
			this.arguments.get(0).visit(this, null);

			if (this.isAddressValue) {
				_asm.add(new Pop(Reg64.RAX));
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
				_asm.add(new Push(Reg64.RAX));
			}

			this.isAddressValue = false;

			this.makePrintln();
		} else {
			ref.ref.visit(this, null);
			ref.id.visit(this, null);
		}
		return null;
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
		_asm.add(new Push(Integer.parseInt(num.spelling)));
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if (bool.spelling.equals("false")) {
			_asm.add(new Push(0));
		}
		else {
			_asm.add(new Push(1));
		}
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nuLL, Object arg) {
		_asm.add(new Push(-1));
		return null;
	}
}
