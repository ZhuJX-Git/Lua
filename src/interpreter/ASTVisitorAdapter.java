package interpreter;

import java.io.Reader;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.text.Collator;

import cop5556fa19.Token.Kind;
import cop5556fa19.Token;
import cop5556fa19.AST.ASTVisitor;
import cop5556fa19.AST.Block;
import cop5556fa19.AST.Chunk;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpFunctionCall;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpList;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTableLookup;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpUnary;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.FieldList;
import cop5556fa19.AST.FieldNameKey;
import cop5556fa19.AST.FuncBody;
import cop5556fa19.AST.FuncName;
import cop5556fa19.AST.Name;
import cop5556fa19.AST.ParList;
import cop5556fa19.AST.RetStat;
import cop5556fa19.AST.Stat;
import cop5556fa19.AST.StatAssign;
import cop5556fa19.AST.StatBreak;
import cop5556fa19.AST.StatDo;
import cop5556fa19.AST.StatFor;
import cop5556fa19.AST.StatForEach;
import cop5556fa19.AST.StatFunction;
import cop5556fa19.AST.StatGoto;
import cop5556fa19.AST.StatIf;
import cop5556fa19.AST.StatLabel;
import cop5556fa19.AST.StatLocalAssign;
import cop5556fa19.AST.StatLocalFunc;
import cop5556fa19.AST.StatRepeat;
import cop5556fa19.AST.StatWhile;
import interpreter.built_ins.*;

public abstract class ASTVisitorAdapter implements ASTVisitor {
	
	@SuppressWarnings("serial")
	public static class StaticSemanticException extends Exception{
		
		public StaticSemanticException(Token first, String msg) {
			super(first.line + ":" + first.pos + " " + msg);
		}
	}
	
	
	@SuppressWarnings("serial")
	public static class TypeException extends Exception{

		public TypeException(String msg) {
			super(msg);
		}
		
		public TypeException(Token first, String msg) {
			super(first.line + ":" + first.pos + " " + msg);
		}
		
	}
	
	public abstract List<LuaValue> load(Reader r) throws Exception;
	List<LuaValue> valueList = null;
	boolean exitFlag = false, exitLoop = false;
	
	void error(Token firstToken, String message) throws StaticSemanticException{
		throw new StaticSemanticException(firstToken, message);
	}
	
	int stringToInt(String s) throws Exception{
		try {
			return Integer.parseInt(s);
		} catch(NumberFormatException excep) {
			throw new StaticSemanticException(null, "Value out of range");
		}
	}
	
	LuaValue handleExp(Exp e, Object arg, int d) throws Exception { // Handle different kinds of exp, also the right hand side of statements
		LuaValue val = null;
		if (e instanceof ExpBinary) val = (LuaValue)visitExpBin((ExpBinary)e, arg);
		else if (e instanceof ExpFalse) val = new LuaBoolean(false);
		else if (e instanceof ExpFunctionCall) val = (LuaValue)e.visit(this, arg);
		else if (e instanceof ExpInt) val = new LuaInt(((ExpInt)e).v);
		else if (e instanceof ExpName) {
			LuaString key = new LuaString(((ExpName)e).name);
			val = ((LuaTable)arg).get(key);
			
			if (d == 0) { // e is left hand of an assignment
				val = key; // If key doesn't exist in table, create a new one 
			}
			else if (d == 1) { // e is right hand of an assignment
				if (val == LuaNil.nil) error(e.firstToken, "ExpName doesn't exist, cannot be assigned");
			}
			else if (d == 2) { // e is the condition in if statement, 
				if (val == LuaNil.nil) val = LuaNil.nil;
			}
			else if (d == 3) {
				if (val == LuaNil.nil) val = key;
			}
			
//			if (val == LuaNil.nil) {
//				if (d == 1) error(e.firstToken, "ExpName doesn't exist"); // If right side, key must exist in the LuaTable
//				else if (d == 0) val = key; // If left side, no matters
//				else val = LuaNil.nil;
//			}
//			else if (d == 2 || d == 0) val = key; // d = 2 is used when e is the condition in if statement
		}
		else if (e instanceof ExpNil) val = LuaNil.nil;
		else if (e instanceof ExpString) {
			LuaString key = new LuaString(((ExpString)e).v);
			val = ((LuaTable)arg).get(key);
			if (val == LuaNil.nil) val = key;
		}
		else if (e instanceof ExpTable) val = (LuaValue)visitExpTable((ExpTable)e, arg);
		else if (e instanceof ExpTableLookup) val = (LuaValue)visitExpTableLookup((ExpTableLookup)e, arg);
		else if (e instanceof ExpTrue) val = new LuaBoolean(true);
		else if (e instanceof ExpUnary) val = (LuaValue)visitUnExp((ExpUnary)e, arg);
		else val = null;
		return val;
	}
	
	int getInt(LuaValue val) throws Exception {
		if (val instanceof LuaInt) return ((LuaInt)val).v;
		else if (val instanceof LuaString) return stringToInt(((LuaString)val).value);
		else error(null, "Cannot get int from LuaValue");
		return -1;
	}
	
	String getString(LuaValue val) throws Exception {
		if (val instanceof LuaInt) return Integer.toString(((LuaInt)val).v);
		else if (val instanceof LuaString) return ((LuaString)val).value;
		else error(null, "Cannot get string from LuaValue");
		return null;
	}
	
	boolean getBoolean(LuaValue val) throws Exception {
		if (val instanceof LuaBoolean) return ((LuaBoolean)val).value;
		else error(null, "Cannot get boolean from LuaValue");
		return true;
	}
	
	boolean transferToBool(LuaValue val) throws Exception {
		if (val == null) return true;
		else if (val == LuaNil.nil) return false;
		else if (val instanceof LuaBoolean) return ((LuaBoolean)val).value;
		else return true;
	}
	
	boolean compareInt(LuaInt e0, LuaInt e1, int relation) {
		int flag = e0.v - e1.v;
		if (relation == 0) return flag < 0 ? true : false; // <
		if (relation == 1) return flag <= 0 ? true : false; // <=
		if (relation == 2) return flag > 0 ? true : false; // >
		else return flag >= 0 ? true : false; // >=
	}
	
	boolean compareString(LuaString e0, LuaString e1, int relation) {
		Collator c = Collator.getInstance(Locale.US);
		int flag = c.compare(e0.value, e1.value);
		if (relation == 0) return flag < 0 ? true : false; // <
		if (relation == 1) return flag <= 0 ? true : false; // <=
		if (relation == 2) return flag > 0 ? true : false; // >
		else return flag >= 0 ? true : false; // >=
	}
	
	boolean compare(Exp e0, Exp e1, int type, Object arg) throws Exception {
		LuaValue v1 = handleExp(e0, arg, 1), v2 = handleExp(e1, arg, 1);
		if (v1 instanceof LuaInt && v2 instanceof LuaInt) return compareInt((LuaInt)v1, (LuaInt)v2, type);
		if (v1 instanceof LuaString && v2 instanceof LuaString) return compareString((LuaString)v1, (LuaString)v2, type);
		else error(e0.firstToken, "Cannot compare, type incompitable");
		return false;
	}

	@Override
	public Object visitExpNil(ExpNil expNil, Object arg) {
		return LuaNil.nil;
	}

	@Override
	public Object visitExpBin(ExpBinary expBin, Object arg) throws Exception {
		LuaValue res = null;
		Kind op = expBin.op;
		Exp e0 = expBin.e0, e1 = expBin.e1;
		
		if (op == Kind.OP_PLUS) res = new LuaInt(getInt(handleExp(e0, arg, 1)) + getInt(handleExp(e1, arg, 1))); // +
		else if (op == Kind.OP_MINUS) res = new LuaInt(getInt(handleExp(e0, arg, 1)) - getInt(handleExp(e1, arg, 1))); // -
		else if (op == Kind.OP_TIMES) res = new LuaInt(getInt(handleExp(e0, arg, 1)) * getInt(handleExp(e1, arg, 1))); // *
		else if (op == Kind.OP_DIV) res = new LuaInt(getInt(handleExp(e0, arg, 1)) / getInt(handleExp(e1, arg, 1))); // /
		else if (op == Kind.OP_DIVDIV) res = new LuaInt(Math.floorDiv(getInt(handleExp(e0, arg, 1)), getInt(handleExp(e1, arg, 1))));// //
		else if (op == Kind.OP_MOD) res = new LuaInt(getInt(handleExp(e0, arg, 1)) % getInt(handleExp(e1, arg, 1))); // %
		else if (op == Kind.OP_POW) res = new LuaInt((int)Math.pow(getInt(handleExp(e0, arg, 1)), getInt(handleExp(e1, arg, 1)))); // ^
		else if (op == Kind.BIT_AMP) res = new LuaInt(getInt(handleExp(e0, arg, 1)) & getInt(handleExp(e1, arg, 1))); // &
		else if (op == Kind.BIT_OR) res = new LuaInt(getInt(handleExp(e0, arg, 1)) | getInt(handleExp(e1, arg, 1))); // |
		else if (op == Kind.BIT_XOR) res = new LuaInt(getInt(handleExp(e0, arg, 1)) ^ getInt(handleExp(e1, arg, 1))); // ~
		else if (op == Kind.BIT_SHIFTR) res = new LuaInt(getInt(handleExp(e0, arg, 1)) >> getInt(handleExp(e1, arg, 1))); // >>
		else if (op == Kind.BIT_SHIFTR) res = new LuaInt(getInt(handleExp(e0, arg, 1)) << getInt(handleExp(e1, arg, 1))); // <<
		else if (op == Kind.REL_EQEQ) res = e0.equals(e1) ? new LuaBoolean(true) : new LuaBoolean(false); // ==
		else if (op == Kind.REL_NOTEQ) res = e0.equals(e1) ? new LuaBoolean(false) : new LuaBoolean(true); // ~=
		else if (op == Kind.REL_LT) res = new LuaBoolean(compare(e0, e1, 0, arg)); // <
		else if (op == Kind.REL_LE) res = new LuaBoolean(compare(e0, e1, 1, arg)); // <=
		else if (op == Kind.REL_GT) res = new LuaBoolean(compare(e0, e1, 2, arg)); // >
		else if (op == Kind.REL_GE) res = new LuaBoolean(compare(e0, e1, 3, arg)); // >=
		else if (op == Kind.KW_and) { // and
			if (transferToBool(handleExp(e0, arg, 1)) == false) res = handleExp(e0, arg, 1);
			else res = handleExp(e1, arg, 1);
		}
		else if (op == Kind.KW_or) { // or
			if (transferToBool(handleExp(e0, arg, 1)) != false) res = handleExp(e0, arg, 1);
			else res = handleExp(e1, arg, 1);
		}
		else if (op == Kind.DOTDOT) res = new LuaString(getString(handleExp(e0, arg, 1)) + getString(handleExp(e1, arg, 1))); // ..
		else error(expBin.firstToken, "Error ExpBinary");
		return res;
	}

	@Override
	public Object visitUnExp(ExpUnary unExp, Object arg) throws Exception {
		LuaValue res = null;
		Kind op = unExp.op;
		Exp e = unExp.e;
		
		if (op == Kind.OP_MINUS) res = new LuaInt(-getInt(handleExp(e, arg, 1))); // -
		else if (op == Kind.BIT_XOR) res = new LuaInt(~getInt(handleExp(e, arg, 1))); // ~
		else if (op == Kind.KW_not) res = new LuaBoolean(!transferToBool(handleExp(e, arg, 1))); // not
		else if (op == Kind.OP_HASH) error(unExp.firstToken, "Not yet implemented"); // #
		else error(unExp.firstToken, "Error ExpUnary");
		
		return res;
	}

	@Override
	public Object visitExpInt(ExpInt expInt, Object arg) {
		return new LuaInt(expInt.v);
	}

	@Override
	public Object visitExpString(ExpString expString, Object arg) {
		return new LuaString(expString.v);
	}

	@Override
	public Object visitExpTable(ExpTable expTableConstr, Object arg) throws Exception {
		LuaTable table = new LuaTable();
		List<Field> fieldList = expTableConstr.fields;
		for (Field f : fieldList) {
			f.visit(this, arg);
			
			if (f instanceof FieldExpKey) {
				Exp key = ((FieldExpKey)f).key, value = ((FieldExpKey) f).value;
				table.put(handleExp(key, arg, 1), handleExp(value, arg, 2));
			}
			else if (f instanceof FieldImplicitKey) {
				Exp key = ((FieldImplicitKey)f).exp;
				table.putImplicit(handleExp(key, arg, 3));
			}
			else {
				String key = ((FieldNameKey)f).name.name;
				Exp value = ((FieldNameKey)f).exp;
				table.put(key, handleExp(value, arg, 1));
			}
		}
		return table;
	}

	@Override
	public Object visitExpList(ExpList expList, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitParList(ParList parList, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitFunDef(ExpFunction funcDec, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitName(Name name, Object arg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		List<Stat> statList = block.stats;
		
		for (Stat s : statList) {
			if (exitFlag == true) return null;
			if (s instanceof StatAssign) visitStatAssign((StatAssign)s, arg);
			else if (s instanceof StatBreak) { 
				visitStatBreak((StatBreak)s, arg);
				exitLoop = true;
				return null;
			}
			else if (s instanceof StatDo) visitStatDo((StatDo)s, arg);
			else if (s instanceof StatFor) visitStatFor((StatFor)s, arg);
			else if (s instanceof StatForEach) visitStatForEach((StatForEach)s, arg);
			else if (s instanceof StatFunction) visitStatFunction((StatFunction)s, arg);
			else if (s instanceof StatGoto) {
				visitStatGoto((StatGoto)s, arg);
				break;
			}
			else if (s instanceof StatIf) {
				visitStatIf((StatIf)s, arg);
				if (exitLoop == true) return null;
			}
			else if (s instanceof StatLabel) visitLabel((StatLabel)s, arg);
			else if (s instanceof StatLocalAssign) visitStatLocalAssign((StatLocalAssign)s, arg);
			else if (s instanceof StatLocalFunc) visitStatLocalFunc((StatLocalFunc)s, arg);
			else if (s instanceof StatRepeat) visitStatRepeat((StatRepeat)s, arg);
			else if (s instanceof StatWhile) visitStatWhile((StatWhile)s, arg);
			else if (s instanceof RetStat) {
				exitFlag = true;
				visitRetStat((RetStat)s, arg);
				return null;
			}
			else throw new StaticSemanticException(s.firstToken, "Error statement type");
		}
		
		return null;
	}
	
	Object gotoBlock(Block b, int index, Object arg) throws Exception {
		List<Stat> statList = b.stats;
		for (int i = index; i < statList.size(); i++) {
			Stat s = statList.get(i);
			if (exitFlag == true) return null;
			if (s instanceof StatAssign) visitStatAssign((StatAssign)s, arg);
			else if (s instanceof StatBreak) { 
				visitStatBreak((StatBreak)s, arg);
				exitLoop = true;
				return null;
			}
			else if (s instanceof StatDo) visitStatDo((StatDo)s, arg);
			else if (s instanceof StatFor) visitStatFor((StatFor)s, arg);
			else if (s instanceof StatForEach) visitStatForEach((StatForEach)s, arg);
			else if (s instanceof StatFunction) visitStatFunction((StatFunction)s, arg);
			else if (s instanceof StatGoto) visitStatGoto((StatGoto)s, arg);
			else if (s instanceof StatIf) visitStatIf((StatIf)s, arg);
			else if (s instanceof StatLabel) visitLabel((StatLabel)s, arg);
			else if (s instanceof StatLocalAssign) visitStatLocalAssign((StatLocalAssign)s, arg);
			else if (s instanceof StatLocalFunc) visitStatLocalFunc((StatLocalFunc)s, arg);
			else if (s instanceof StatRepeat) visitStatRepeat((StatRepeat)s, arg);
			else if (s instanceof StatWhile) visitStatWhile((StatWhile)s, arg);
			else if (s instanceof RetStat) {
				exitFlag = true;
				visitRetStat((RetStat)s, arg);
				return null;
			}
			else throw new StaticSemanticException(s.firstToken, "Error statement type");
		}
		return null;
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg, Object arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatBreak(StatBreak statBreak, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitStatGoto(StatGoto statGoto, Object arg) throws Exception {
		StatLabel label = statGoto.label;
		Block b = label.getBlock();
		gotoBlock(b, label.index, arg);
		return null;
	}

	@Override
	public Object visitStatDo(StatDo statDo, Object arg) throws Exception {
		Block b = statDo.b;
		visitBlock(b, arg);
		
		return null;
	}

	@Override
	public Object visitStatWhile(StatWhile statWhile, Object arg) throws Exception {
		Exp e = statWhile.e;
		Block b = statWhile.b;
		boolean flag = transferToBool(handleExp(e, arg, 2));
		while (flag == true) {
			visitBlock(b, arg);
			if (exitLoop == true) return null;
			flag = transferToBool(handleExp(e, arg, 2));
		}
		return null;
	}

	@Override
	public Object visitStatRepeat(StatRepeat statRepeat, Object arg) throws Exception {
		Block b = statRepeat.b;
		Exp e = statRepeat.e;
		boolean flag = transferToBool(handleExp(e, arg, 2));
		while (flag == false) {
			visitBlock(b, arg);
			if (exitLoop == true) return null;
			flag = transferToBool(handleExp(e, arg, 2));
		}
		return null;
	}

	@Override
	public Object visitStatIf(StatIf statIf, Object arg) throws Exception {
		List<Exp> es = statIf.es;
		List<Block> bs = statIf.bs; 
		for (int i = 0; i < es.size(); i++) {
			Exp cond = es.get(i);
			Block b = bs.get(i);
			boolean flag = transferToBool(handleExp(cond, arg, 2));
			if (flag == true) {
				visitBlock(b, arg);
				break;
			}
		}
		return null;
	}

	// Need not to implement
	@Override
	public Object visitStatFor(StatFor statFor1, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	// Need not to implement
	@Override
	public Object visitStatForEach(StatForEach statForEach, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitFuncName(FuncName funcName, Object arg) {
		throw new UnsupportedOperationException();
	}

	// Need not to implement
	@Override
	public Object visitStatFunction(StatFunction statFunction, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	// Need not to implement
	@Override
	public Object visitStatLocalFunc(StatLocalFunc statLocalFunc, Object arg) {
		throw new UnsupportedOperationException();
	}

	// Need not to implement
	@Override
	public Object visitStatLocalAssign(StatLocalAssign statLocalAssign, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitRetStat(RetStat retStat, Object arg) throws Exception {
		List<Exp> expList = retStat.el;
		if (expList.size() > 0) valueList = new ArrayList<>();
		for (Exp e : expList) {
			if (e instanceof ExpInt) valueList.add(new LuaInt(((ExpInt)e).v));
			else if (e instanceof ExpName) {
				LuaValue key = new LuaString(((ExpName)e).name);
				valueList.add(((LuaTable)arg).get(key));
			}
			else if (e instanceof ExpString) {
				LuaValue key = new LuaString(((ExpString)e).v);
				valueList.add(((LuaTable)arg).get(key));
			}
			else error(retStat.firstToken, "Type cannot return");
		}
		return null;
	}

	@Override
	public Object visitChunk(Chunk chunk, Object arg) throws Exception {
		Block b = chunk.block;
		visitBlock(b, arg);	
		
		return valueList;
	}

	@Override
	public Object visitFieldExpKey(FieldExpKey fieldExpKey, Object object) throws Exception {
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
	public Object visitExpTrue(ExpTrue expTrue, Object arg) {
		return new LuaBoolean(true);
	}

	@Override
	public Object visitExpFalse(ExpFalse expFalse, Object arg) {
		return new LuaBoolean(false);
	}

	@Override
	public Object visitFuncBody(FuncBody funcBody, Object arg) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpVarArgs(ExpVarArgs expVarArgs, Object arg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatAssign(StatAssign statAssign, Object arg) throws Exception {
		List<Exp> varList = statAssign.varList, expList = statAssign.expList;
		List<LuaValue> varVals = new ArrayList<>(), expVals = new ArrayList<>();
		
		int diff = varList.size() - expList.size();
		if (diff > 0) for (int i = 0; i < diff; i++) expList.add(null);
		for (int i = 0; i < varList.size(); i++) {
			Exp var = varList.get(i), exp = expList.get(i);
			if (var instanceof ExpTableLookup) {
				LuaValue value = handleExp(exp, arg, 1);
				Exp table = ((ExpTableLookup) var).table, key = ((ExpTableLookup) var).key;
				LuaValue t = handleExp(table, arg, 1);
				if (t instanceof LuaTable) {
					LuaValue k = handleExp(key, arg, 3);
					((LuaTable) t).put(k, value);
				}
				else error(statAssign.firstToken, "Cannot put in table");
			}
			else {
				LuaValue key = handleExp(var, arg, 0), value = null;
				if (exp == null) value = LuaNil.nil;
				else value = handleExp(exp, arg, 1);
				((LuaTable)arg).put(key, value);
			}
		}
		return null;
	}

	@Override
	public Object visitExpTableLookup(ExpTableLookup expTableLookup, Object arg) throws Exception {
		LuaValue res = null;
		Exp table = expTableLookup.table, key = expTableLookup.key;
		LuaValue t = (((LuaTable)arg).get(handleExp(table, arg, 1)));
		if (t instanceof LuaTable) res = ((LuaTable)t).get(handleExp(key, t, 1));
		else res = LuaNil.nil;
		return res;
	}

	@Override
	public Object visitExpFunctionCall(ExpFunctionCall expFunctionCall, Object arg) throws Exception {
		Exp f = expFunctionCall.f;
		List<Exp> args = expFunctionCall.args;
		List<LuaValue> argVals = new ArrayList<>();
		LuaValue fun = handleExp(f, arg, 1);
		for (Exp e : args) argVals.add(handleExp(e, arg, 1));
		if (fun instanceof JavaFunction) {
			List<LuaValue> res = ((JavaFunction) fun).call(argVals);
			if (res.size() > 0) return res.get(0);
		}
		else error(expFunctionCall.firstToken, "Cannot make a function call");
		return fun;
	}

	@Override
	public Object visitLabel(StatLabel statLabel, Object arg) {
		return null;
	}

	@Override
	public Object visitFieldList(FieldList fieldList, Object arg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpName(ExpName expName, Object arg) {
		return new LuaString(expName.name);
	}
}
