package interpreter;
import cop5556fa19.AST.*;
import java.util.*;
import cop5556fa19.Token;

public class StaticAnalysis implements ASTVisitor{
	Stack<Map<Name, StatLabel>> symbolTable;
	
	public StaticAnalysis() {
		symbolTable = new Stack<>();
	}
	
	@Override
	public Object visitExpNil(ExpNil expNil, Object arg) throws Exception {
		return null;
	}
	
	@Override
	public Object visitExpBin(ExpBinary expBin, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitUnExp(ExpUnary unExp, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpInt(ExpInt expInt, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpString(ExpString expString, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpTable(ExpTable expTableConstr, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpList(ExpList expList, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitParList(ParList parList, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitFunDef(ExpFunction funcDec, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitName(Name name, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		List<Stat> stats = block.stats;
		Map<Name, StatLabel> map = new HashMap<>();
		symbolTable.push(map);
		
		for (int i = 0; i < stats.size(); i++) {
			Stat s = stats.get(i);
			if (s instanceof StatLabel) {
				((StatLabel) s).setBlock(block);
				((StatLabel) s).index = i;
				s.visit(this, arg);
			}
		}
		for (Stat s : stats) {
			if (s instanceof StatLabel) continue;
			else s.visit(this, arg);
		}
		symbolTable.pop();
		return null;
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg, Object arg2) {
		return null;
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitStatGoto(StatGoto statGoto, Object arg) throws Exception {
		Map<Name, StatLabel> m = null;
		for (int i = symbolTable.size() - 1; i >= 0; i--) {
			m = symbolTable.get(i);
			if (m.containsKey(statGoto.name)) { 
				statGoto.label = m.get(statGoto.name);
				System.out.println(statGoto.label.getIndex());
				return null;
			}
		}
		throw new StaticSemanticException(statGoto.firstToken, "Label not found");
	}

	@Override
	public Object visitStatDo(StatDo statDo, Object arg) throws Exception {
		return statDo.b.visit(this, arg);
	}

	@Override
	public Object visitStatWhile(StatWhile statWhile, Object arg) throws Exception {
		return statWhile.b.visit(this, arg);
	}

	@Override
	public Object visitStatRepeat(StatRepeat statRepeat, Object arg) throws Exception {
		return statRepeat.b.visit(this, arg);
	}

	@Override
	public Object visitStatIf(StatIf statIf, Object arg) throws Exception {
		List<Block> blockList = statIf.bs;
		for (Block b : blockList) {
			visitBlock(b, arg);
		}
		return null;
	}

	@Override
	public Object visitStatFor(StatFor statFor1, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatForEach(StatForEach statForEach, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitFuncName(FuncName funcName, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatFunction(StatFunction statFunction, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatLocalFunc(StatLocalFunc statLocalFunc, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatLocalAssign(StatLocalAssign statLocalAssign, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitRetStat(RetStat retStat, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitChunk(Chunk chunk, Object arg) throws Exception {
		Block b = chunk.block;
		visitBlock(b, arg);
		return null;
	}

	@Override
	public Object visitFieldExpKey(FieldExpKey fieldExpKey, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitFieldNameKey(FieldNameKey fieldNameKey, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitFieldImplicitKey(FieldImplicitKey fieldImplicitKey, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpTrue(ExpTrue expTrue, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpFalse(ExpFalse expFalse, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitFuncBody(FuncBody funcBody, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpVarArgs(ExpVarArgs expVarArgs, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitStatAssign(StatAssign statAssign, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpTableLookup(ExpTableLookup expTableLookup, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpFunctionCall(ExpFunctionCall expFunctionCall, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitLabel(StatLabel statLabel, Object arg) throws Exception {
		Map<Name, StatLabel> m = symbolTable.peek();
		if (m.containsKey(statLabel.label)) throw new StaticSemanticException(statLabel.firstToken, "Same label exists");
		else symbolTable.peek().put(statLabel.label, statLabel);
		return null;
	}

	@Override
	public Object visitFieldList(FieldList fieldList, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitExpName(ExpName expName, Object arg) throws Exception {
		return null;
	}
}
