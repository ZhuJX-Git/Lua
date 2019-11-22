/**
 * Developed  for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Fall 2019.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Fall 2019 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2019
 */

package cop5556fa19;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import cop5556fa19.AST.*;
import cop5556fa19.AST.Block;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpUnary;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.FieldList;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.FieldNameKey;
import cop5556fa19.AST.FuncBody;
import cop5556fa19.AST.Name;
import cop5556fa19.AST.ParList;
import cop5556fa19.Token.Kind;
import static cop5556fa19.Token.Kind.*;

public class Parser {
	
	@SuppressWarnings("serial")
	class SyntaxException extends Exception {
		Token t;
		
		public SyntaxException(Token t, String message) {
			super(t.line + ":" + t.pos + " " + message);
		}
	}
	
	final Scanner scanner;
	Token t;  //invariant:  this is the next token

	public Chunk parse() throws Exception {
		Chunk chunk = chunk();
		if (!isKind(EOF)) throw new SyntaxException(t, "Parse ended before end of input");
		return chunk;
	}

	public Parser(Scanner s) throws Exception {
		this.scanner = s;
		t = scanner.getNext(); //establish invariant
	}
	
	public Chunk chunk() throws Exception{
		Token firstToken = t;
		
		return new Chunk(firstToken, block());
	}
	
	Stat stat() throws Exception {
		Token firstToken = t;
		Stat s = null;
		
//		if (isKind(SEMI)) {
//			consume();
//		}
		if (isKind(NAME) || isKind(LPAREN)) {
			List<Exp> varList = varlist();
			match(ASSIGN);
			List<Exp> expList = explist().list;
			s = new StatAssign(firstToken, varList, expList);
		}
		else if (isKind(KW_break)) {
			consume();
			s = new StatBreak(firstToken);
		}
		else if (isKind(COLONCOLON)) {
			consume();
			Name label = Name();
//			if (isKind(COLONCOLON)) {
//				consume();
//				s = new StatLabel(firstToken, label);
//			}
//			else error(firstToken, "Invalid stat");
			match(COLONCOLON);
			s = new StatLabel(firstToken, label);
		}
		else if (isKind(KW_goto)) {
			consume();
			Name name = Name();
			s = new StatGoto(firstToken, name);
		}
		else if (isKind(KW_do)) {
			consume();
			Block b = block();
			match(KW_end);
			System.out.println(firstToken);
			s = new StatDo(firstToken, b);
		}
		else if (isKind(KW_while)) {
			consume();
			Exp e = exp();
			match(KW_do);
			Block b = block();
			match(KW_end);
			s = new StatWhile(firstToken, e, b);
		}
		else if (isKind(KW_repeat)) {
			consume();
			Block b = block();
			match(KW_until);
			Exp e = exp();
			s = new StatRepeat(firstToken, b, e);
		}
		else if (isKind(KW_if)) {
			List<Exp> es = new ArrayList<>();
			List<Block> bs = new ArrayList<>();
			
			consume();
			es.add(exp());
			match(KW_then);
			bs.add(block());
			while (isKind(KW_elseif)) {
				consume();
				es.add(exp());
				match(KW_then);
				bs.add(block());
			}
			if (isKind(KW_else)) {
				consume();
				bs.add(block());
			}
			match(KW_end);
			s = new StatIf(firstToken, es, bs);
		}
		else if (isKind(KW_for)) {
			consume();
			ExpName n = new ExpName(consume());
			if (isKind(ASSIGN)) {
				consume();
				Exp ebeg = exp();
				match(COMMA);
				Exp eend = exp();
				if (isKind(KW_do) || isKind(COMMA)) {
					Exp einc = null;
					if (isKind(COMMA)) {
						consume();
						einc = exp();
					}
					match(KW_do);
					Block b = block();
					match(KW_end);
					s = new StatFor(firstToken, n, ebeg, eend, einc, b);
				}
				else error(firstToken, "Invalid statement");
			}
			else { 
				List<ExpName> names = new ArrayList<>();
				names.add(n);
				while (isKind(COMMA)) {
					consume();
					names.add(new ExpName(consume()));
				}
				match(KW_in);
				List<Exp> exps = explist().list;
				match(KW_do);
				Block b = block();
				match(KW_end);
				s = new StatForEach(firstToken, names, exps, b);
			}
		}
		else if (isKind(KW_function)) {
			consume();
			FuncName name = funcname();
			FuncBody body = funcbody();
			s = new StatFunction(firstToken, name, body);		
		}
		else if (isKind(KW_local)) {
			consume();
			if (isKind(KW_function)) {
				consume();
				FuncName funcName = funcname();
				FuncBody funcBody = funcbody();
				s = new StatLocalFunc(firstToken, funcName, funcBody);
			}
			else {
				List<ExpName> nameList = new ArrayList<>();
				nameList.add(new ExpName(consume()));
				while (isKind(COMMA)) {
					consume();
					nameList.add(new ExpName(consume()));
				}
				List<Exp> expList = null;
				if (isKind(ASSIGN)) {
					consume();
					expList = explist().list;
				}
				s = new StatLocalAssign(firstToken, nameList, expList);
			}
		}
		else error(firstToken, "Invalid statement");
		
		return s;
	}
	
	RetStat retstat() throws Exception {
		Token firstToken = t;
		List<Exp> el = null;
		
		match(KW_return);
		if (isKind(KW_nil) || isKind(KW_false) || isKind(KW_true) || isKind(INTLIT) || isKind(STRINGLIT) || isKind(DOTDOTDOT) || isKind(KW_function) || isKind(NAME) || isKind(LPAREN) || isKind(LCURLY) || isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)) {
			el = explist().list;
		}
		if (isKind(SEMI)) {
			consume();
		}
		return new RetStat(firstToken, el);
	}
	
	FuncName funcname() throws Exception{
		Token firstToken = t;
		
		ExpName n = new ExpName(consume());
		if (isKind(DOT) || isKind(COLON)) {
			List<ExpName> names = new ArrayList<>();
			names.add(n);
			while (isKind(DOT)) {
				consume();
				names.add(new ExpName(consume()));
			}
			ExpName nameAfterColon = null;
			if (isKind(COLON)) {
				consume();
				nameAfterColon = new ExpName(consume());
			}
			return new FuncName(firstToken, names, nameAfterColon);
		}
		else return new FuncName(firstToken, n);
	}
	
	ExpList explist() throws Exception {
		Token firstToken = t;
		List<Exp> list = new ArrayList<>();
		
		list.add(exp());
		while (isKind(COMMA)) {
			consume();
			list.add(exp());
		}
		
		return new ExpList(firstToken, list);
	}
	
	Exp prefixexpTail(Exp e0) throws Exception {
		Token firstToken = t;
		
		if (isKind(LSQUARE)) {
			consume();
			Exp e1 = exp();
			match(RSQUARE);
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpTableLookup(firstToken, e0, e1));
			else return new ExpTableLookup(firstToken, e0, e1);
		}
		else if (isKind(DOT)) {
			consume();
			Exp s = new ExpString(consume());
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpTableLookup(firstToken, e0, s));
			else return new ExpTableLookup(firstToken, e0, s);
		}
		else if (isKind(LPAREN)) {
			consume();
			List<Exp> args = new ArrayList<>();
			if (isKind(KW_nil) || isKind(KW_false) || isKind(KW_true) || isKind(INTLIT) || isKind(STRINGLIT) || isKind(DOTDOTDOT) || isKind(KW_function) || isKind(NAME) || isKind(LPAREN) || isKind(LCURLY) || isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)) {
				args = explist().list;
				match(RPAREN);
			}
			else match(RPAREN);
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpFunctionCall(firstToken, e0, args));
			else return new ExpFunctionCall(firstToken, e0, args);
		}
		else if (isKind(LCURLY)) {
			ExpTable table = tableconstructor();
			List<Exp> args = new ArrayList<>();
			args.add(table);
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpFunctionCall(firstToken, e0, args));
			else return new ExpFunctionCall(firstToken, e0, args);
 		}
		else if (isKind(STRINGLIT)) {
			List<Exp> args = new ArrayList<>();
			args.add(new ExpString(consume()));
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpFunctionCall(firstToken, e0, args));
			else return new ExpFunctionCall(firstToken, e0, args);
		}
		else if (isKind(COLON)) {
			consume();
			Exp s = new ExpString(consume());
			Exp e1 = new ExpTableLookup(firstToken, e0, s);
			List<Exp> args = args();
			args.add(0, e0);
			if (ifNextPrefixexpTail()) return prefixexpTail(new ExpFunctionCall(firstToken, e1, args));
			else return new ExpFunctionCall(firstToken, e1, args);
		}
		else return e0;
	}
	
	boolean ifNextPrefixexpTail() {
		if (isKind(LSQUARE) || isKind(DOT) || isKind(LPAREN) || isKind(LCURLY) || isKind(STRINGLIT) || isKind(COLON)) {
			return true;
		}
		return false;
	}
	
	List<Exp> varlist() throws Exception {
		Token firstToken = t;
		List<Exp> vars = new ArrayList<>();
		
		vars.add(var());
		while (isKind(COMMA)) {
			consume();
			vars.add(var());
		}
		
		return vars;
	}
	
	Exp var() throws Exception {
		Token firstToken = t;
		
		if (isKind(NAME)) {
			Exp e0 = new ExpName(consume());
			if (ifNextPrefixexpTail()) return varTail(e0);
			else return e0;
		}
		else if (isKind(LPAREN)) {
			consume();
			Exp e0 = exp();
			match(RPAREN);
			return varTail(e0);
		}
		else error(firstToken, "Invalid var");
		return null;
	}
	
	Exp varTail(Exp e0) throws Exception {
		Token firstToken = t;
		
		Exp e1 = prefixexpTail(e0);
		if (e1 == e0 || e1 instanceof ExpFunctionCall) error(firstToken, "Invalid var, exp type not match");
		return e1;
	}
	
	List<Exp> args() throws Exception {
		Token firstToken = t;
		List<Exp> exps = new ArrayList<>();
		
		if (isKind(LPAREN)) {
			consume();
//			System.out.println("test");
			if (isKind(KW_nil) || isKind(KW_false) || isKind(KW_true) || isKind(INTLIT) || isKind(STRINGLIT) || isKind(DOTDOTDOT) || isKind(KW_function) || isKind(NAME) || isKind(LPAREN) || isKind(LCURLY) || isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)) {
				exps = explist().list;
				match(RPAREN);
			}
			else match(RPAREN);
		}
		else if (isKind(LCURLY)) {
			ExpTable table = tableconstructor();
			exps.add(table);
		}
		else if (isKind(STRINGLIT)) {
			Token stringLit = consume();
			exps.add(new ExpString(stringLit));
		}
		else error(firstToken, "Invalid args");
		
		return exps;
	}

	Exp exp() throws Exception {
		Token first = t;
		Exp e0 = andExp();
		while (isKind(KW_or)) {
			Token op = consume();
			Exp e1 = andExp();
			e0 = new ExpBinary(first, e0, op, e1);
		}
		
		return e0;
	}
	
	Exp Exp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = AndExp(name);
		while (isKind(KW_and)) {
			Token and = consume();
			Exp e1 = andExp();
			e0 = new ExpBinary(first_token, e0, and, e1);
		}
		return e0;
	}
	
	private Exp andExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = compareExp();
		while (isKind(KW_and)) {
			Token and = consume();
			Exp e1 = compareExp();
			e0 = new ExpBinary(first_token, e0, and, e1);
		}
		
		return e0;
	}
	
	private Exp AndExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = CompareExp(name);
		while (isKind(KW_and)) {
			Token and = consume();
			Exp e1 = compareExp();
			e0 = new ExpBinary(first_token, e0, and, e1);
		}
		return e0;
	}
	
	private Exp compareExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = orExp();
		while (isKind(REL_LT) || isKind(REL_GT) || isKind(REL_LE) || isKind(REL_GE) || isKind(REL_NOTEQ) || isKind(REL_EQEQ)) {
			Token rel = consume();
			Exp e1 = orExp();
			e0 = new ExpBinary(first_token, e0, rel, e1);
		}
		
		return e0;
	}
	
	private Exp CompareExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = OrExp(name);
		while (isKind(REL_LT) || isKind(REL_GT) || isKind(REL_LE) || isKind(REL_GE) || isKind(REL_NOTEQ) || isKind(REL_EQEQ)) {
			Token rel = consume();
			Exp e1 = orExp();
			e0 = new ExpBinary(first_token, e0, rel, e1);
		}
		
		return e0;
	}
	
	private Exp orExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = xorExp();
		while (isKind(BIT_OR)) {
			Token or = consume();
			Exp e1 = xorExp();
			e0 = new ExpBinary(first_token, e0, or, e1);
		}
		
		return e0;
	}
	
	private Exp OrExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = XorExp(name);
		while (isKind(BIT_OR)) {
			Token or = consume();
			Exp e1 = xorExp();
			e0 = new ExpBinary(first_token, e0, or, e1);
		}
		
		return e0;
	}
	
	private Exp xorExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = ampExp();
		while (isKind(BIT_XOR)) {
			Token xor = consume();
			Exp e1 = ampExp();
			e0 = new ExpBinary(first_token, e0, xor, e1);
		}
		
		return e0;
	}
	
	private Exp XorExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = AmpExp(name);
		while (isKind(BIT_XOR)) {
			Token xor = consume();
			Exp e1 = ampExp();
			e0 = new ExpBinary(first_token, e0, xor, e1);
		}
		
		return e0;
	}
	
	private Exp ampExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = shiftExp();
		while (isKind(BIT_AMP)) {
			Token amp = consume();
			Exp e1 = shiftExp();
			e0 = new ExpBinary(first_token, e0, amp, e1);
		}
		
		return e0;
	}
	
	private Exp AmpExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = ShiftExp(name);
		while (isKind(BIT_AMP)) {
			Token amp = consume();
			Exp e1 = shiftExp();
			e0 = new ExpBinary(first_token, e0, amp, e1);
		}
		
		return e0;
	}
	
	private Exp shiftExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = connectExp();
		while (isKind(BIT_SHIFTL) || isKind(BIT_SHIFTR)) {
			Token shift = consume();
			Exp e1 = connectExp();
			e0 = new ExpBinary(first_token, e0, shift, e1);
		}
		
		return e0;
	}
	
	private Exp ShiftExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = ConnectExp(name);
		while (isKind(BIT_SHIFTL) || isKind(BIT_SHIFTR)) {
			Token shift = consume();
			Exp e1 = connectExp();
			e0 = new ExpBinary(first_token, e0, shift, e1);
		}
		
		return e0;
	}
	
	private Exp connectExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = addExp();
		if (isKind(DOTDOT)) {
			Token dotdot = consume();
			Exp e1 = connectExp();
			e0 = new ExpBinary(first_token, e0, dotdot, e1);
		}
		
		return e0;
	}
	
	private Exp ConnectExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = AddExp(name);
		if (isKind(DOTDOT)) {
			Token dotdot = consume();
			Exp e1 = connectExp();
			e0 = new ExpBinary(first_token, e0, dotdot, e1);
		}
		
		return e0;
	}
	
	private Exp addExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = multiExp();
		while (isKind(OP_PLUS) || isKind(OP_MINUS)) {
			Token add = consume();
			Exp e1 = multiExp();
			e0 = new ExpBinary(first_token, e0, add, e1);
		}
		return e0;
	}
	
	private Exp AddExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = MultiExp(name);
		while (isKind(OP_PLUS) || isKind(OP_MINUS)) {
			Token add = consume();
			Exp e1 = multiExp();
			e0 = new ExpBinary(first_token, e0, add, e1);
		}
		return e0;
	}
	
	private Exp multiExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = unaryExp();
		while (isKind(OP_TIMES) || isKind(OP_DIV) || isKind(OP_DIVDIV) || isKind(OP_MOD)) {
			Token multi = consume();
			Exp e1 = unaryExp();
			e0 = new ExpBinary(first_token, e0, multi, e1);
		}
		return e0;
	}
	
	private Exp MultiExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = PowExp(name);
		while (isKind(OP_TIMES) || isKind(OP_DIV) || isKind(OP_DIVDIV) || isKind(OP_MOD)) {
			Token multi = consume();
			Exp e1 = unaryExp();
			e0 = new ExpBinary(first_token, e0, multi, e1);
		}
		return e0;
	}
	
	private Exp unaryExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = null;
		if (isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)) {
			Kind unary = consume().kind;
			Exp e1 = unaryExp();
			e0 = new ExpUnary(first_token, unary, e1);
		}
		else {
			e0 = powExp();
		}
		
		return e0;
	}
	
	private Exp powExp() throws Exception {
		Token first_token = t;
		
		Exp e0 = Expr();
		if (isKind(OP_POW)) {
			Token pow = consume();
			Exp e1 = powExp();
			e0 = new ExpBinary(first_token, e0, pow, e1);
		}
		return e0;
	}
	
	private Exp PowExp(Name name) throws Exception {
		Token first_token = name.firstToken;
		
		Exp e0 = EExpr(name);
		if (isKind(OP_POW)) {
			Token pow = consume();
			Exp e1 = powExp();
			e0 = new ExpBinary(first_token, e0, pow, e1);
		}
		return e0;
	}
	
	private Exp Expr() throws Exception {
		Token first_token = t;
		Exp e0 = null;
		
		if (isKind(KW_nil)) {
			Token kw_nil = consume();
			e0 = new ExpNil(kw_nil);
		}
		else if (isKind(KW_false)) {
			Token kw_false = consume();
			e0 = new ExpFalse(kw_false);
		}
		else if (isKind(KW_true)) {
			Token kw_true = consume();
			e0 = new ExpTrue(kw_true);
		}
		else if (isKind(INTLIT)) {
			Token intlit = consume();
			e0 = new ExpInt(intlit);
		}
		else if (isKind(STRINGLIT)) {
			Token stringlit = consume();
			e0 = new ExpString(stringlit);
		}
		else if (isKind(DOTDOTDOT)) {
			Token dotdotdot = consume();
			e0 = new ExpVarArgs(dotdotdot);
		}
		else if (isKind(KW_function)) {
			e0 = functiondef();
		}
		else if (isKind(NAME)) {
			e0 = new ExpName(consume());
			e0 = prefixexpTail(e0);
		}
		else if (isKind(LPAREN)) {
			consume();
			e0 = exp();
			match(RPAREN);
			e0 = prefixexpTail(e0);
		}
		else if (isKind(LCURLY)) {
			e0 = tableconstructor();
		}
		else {
			error(first_token, "No valid exp match");
		}
		
		return e0;
	}
	
	Exp EExpr(Name name) throws Exception{
		return new ExpName(name.toString());
	}
	
	ExpFunction functiondef() throws Exception {
		Token first_token = t;
		
		match(KW_function);
		FuncBody funcbody = funcbody();
		
		return new ExpFunction(first_token, funcbody);
	}
	
	FuncBody funcbody() throws Exception {
		Token first_token = t;
		ParList parlist = null;
		Block block;
		
		match(LPAREN);
//		System.out.println("ok");
		if (isKind(RPAREN)) {
			consume();
			block = block();
		}
		else {
			parlist = parlist();
			match(RPAREN);
			block = block();
		}
		match(KW_end);
		
		return new FuncBody(first_token, parlist, block);
	}
	
	ParList parlist() throws Exception {
		Token first_token = t;
		List<Name> name_list = new ArrayList<>();
		boolean ifVarArgs = false;
		
		if (isKind(DOTDOTDOT)) {
			consume();
			ifVarArgs = true;
			return new ParList(first_token, name_list, ifVarArgs);
		}
		else {
			name_list.add(Name());
			while (isKind(COMMA)) {
				consume();
				if (isKind(NAME)) {
					name_list.add(Name());
				}
				else if (isKind(DOTDOTDOT)) {
					consume();
					ifVarArgs = true;
					break;
				}
				else {
					error(first_token, "Invalid parlist");
				}
			}
		}
		return new ParList(first_token, name_list, ifVarArgs);
	}
	
	Name Name() throws Exception {
		Token first_token = t;
		String name = t.text;
		
		consume();
		
		return new Name(first_token, name);
	}
	
	ExpTable tableconstructor() throws Exception {
		Token first_token = t;
		List<Field> fields = new ArrayList<>();
		
		match(LCURLY);
		if (isKind(RCURLY)) {
			consume();
			return new ExpTable(first_token, fields);
		}
		else {
			fields = fieldlist().getFields();
			match(RCURLY);
			return new ExpTable(first_token, fields);
		}
	}
	
	FieldList fieldlist() throws Exception {
		Token first_token = t;
		List<Field> fields = new ArrayList<>();
		
		fields.add(field());
		while (isKind(COMMA) || isKind(SEMI)) {
			consume();
			if (isKind(LSQUARE)) {
				consume();
				Exp e0 = exp();
				match(RSQUARE);
				match(ASSIGN);
				Exp e1 = exp();
				fields.add(new FieldExpKey(first_token, e0, e1));
			}
			else if (isKind(NAME)) {
				Name name = Name();
				
				if (isKind(ASSIGN)) {
					match(ASSIGN);
					Exp e0 = exp();
					fields.add(new FieldNameKey(first_token, name, e0));
				}
				else {
//					Exp e0 = Exp(name);
					Exp e0 = new ExpName(name.name);
					fields.add(new FieldImplicitKey(first_token, e0));
				}
			}
			else if (isKind(KW_nil) || isKind(KW_false) || isKind(KW_true) || isKind(INTLIT) || isKind(STRINGLIT) || isKind(DOTDOTDOT) || isKind(KW_function) || isKind(NAME) || isKind(LPAREN) || isKind(LCURLY) || isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)) {
				Exp e0 = exp();
				fields.add(new FieldImplicitKey(first_token, e0));
			}
			else {
				break;
			}
		}
		
		return new FieldList(first_token, fields);
	}
	
	Field field() throws Exception {
		Token first_token = t;
		
		if (isKind(LSQUARE)) {
			consume();
			Exp e0 = exp();
			match(RSQUARE);
			match(ASSIGN);
			Exp e1 = exp();
			return new FieldExpKey(first_token, e0, e1);
		}
		else if (isKind(NAME)) {
			Name name = Name();
			if (isKind(ASSIGN)) {
				match(ASSIGN);
				Exp e0 = exp();
				return new FieldNameKey(first_token, name, e0);
			}
			else {
//				Exp e0 = Exp(name);
				Exp e0 = new ExpName(name.name);
				return new FieldImplicitKey(first_token, e0);
			}
		}
		else if (isKind(KW_nil) || isKind(KW_false) || isKind(KW_true) || isKind(INTLIT) || isKind(STRINGLIT) || isKind(DOTDOTDOT) || isKind(KW_function) || isKind(NAME) || isKind(LPAREN) || isKind(LCURLY) || isKind(KW_not) || isKind(BIT_XOR) || isKind(OP_MINUS) || isKind(OP_HASH)){
			Exp e0 = exp();
			return new FieldImplicitKey(first_token, e0);
		}
		else {
			error(first_token, "Invalid field");
			Exp e0 = null;
			return new FieldImplicitKey(first_token, e0);
		}
	}


	private Block block() throws Exception{
		Token firstToken = t;
		List<Stat> stats = new ArrayList<>();
		
		while (isKind(SEMI) || isKind(NAME) || isKind(LPAREN) || isKind(COLONCOLON) || isKind(KW_break) || isKind(KW_goto) || isKind(KW_do) || isKind(KW_while) || isKind(KW_repeat) || isKind(KW_if) || isKind(KW_for) || isKind(KW_function) || isKind(KW_local)) {
			if (isKind(SEMI)) {
				consume();
				continue;
			}
			stats.add(stat());
		}
		if (isKind(KW_return)) {
			stats.add(retstat());
		}
		return new Block(firstToken, stats);
	}


	protected boolean isKind(Kind kind) {
		return t.kind == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind)
				return true;
		}
		return false;
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind kind) throws Exception {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		error(kind);
		return null; // unreachable
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind... kinds) throws Exception {
		Token tmp = t;
		if (isKind(kinds)) {
			consume();
			return tmp;
		}
		StringBuilder sb = new StringBuilder();
		for (Kind kind1 : kinds) {
			sb.append(kind1).append(kind1).append(" ");
		}
		error(kinds);
		return null; // unreachable
	}

	Token consume() throws Exception {
		Token tmp = t;
        t = scanner.getNext();
		return tmp;
	}
	
	void error(Kind... expectedKinds) throws SyntaxException {
		String kinds = Arrays.toString(expectedKinds);
		String message;
		if (expectedKinds.length == 1) {
			message = "Expected " + kinds + " at " + t.line + ":" + t.pos;
		} else {
			message = "Expected one of" + kinds + " at " + t.line + ":" + t.pos;
		}
		throw new SyntaxException(t, message);
	}

	void error(Token t, String m) throws SyntaxException {
		String message = m + " at " + t.line + ":" + t.pos;
		throw new SyntaxException(t, message);
	}
	


}
