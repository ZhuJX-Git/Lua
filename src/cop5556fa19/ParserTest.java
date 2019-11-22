/* *
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

import static cop5556fa19.Token.Kind.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

import cop5556fa19.AST.*;
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
import cop5556fa19.AST.Expressions;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.ParList;
import cop5556fa19.Parser.SyntaxException;
import cop5556fa19.Parser;

class ParserTest {

	// To make it easy to print objects and turn this output on and off
	static final boolean doPrint = true;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}


	
	// creates a scanner, parser, and parses the input.  
	Exp parseExpAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Exp e = parser.exp();
		show("e=" + e);
		return e;
	}
	
	Block parseBlockAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Method method = Parser.class.getDeclaredMethod("block");
		method.setAccessible(true);
		Block b = (Block) method.invoke(parser);
		show("b=" + b);
		return b;
	}
	
	Chunk parseAndShow(String input) throws Exception {
		show("parser input:\n" + input); // Display the input
		Reader r = new StringReader(input);
		Scanner scanner = new Scanner(r); // Create a Scanner and initialize it
		Parser parser = new Parser(scanner);
		Chunk c = parser.parse();
//		Chunk c = parser.chunk();
		show("c="+c);
		return c;
	}
	
	@Test
	void testEmpty1() throws Exception {
		String input = "{3, a}";
//		Block b = parseBlockAndShow(input);
		Exp e = parseExpAndShow(input);
		List<Field> fieldList = ((ExpTable) e).fields;
		Field n = fieldList.get(1);
		FieldImplicitKey n2 = (FieldImplicitKey) n;
		Exp exp = n2.exp;
		String name = ((ExpName) exp).name;
		assertEquals("a", name);
		
//		Block expected = Expressions.makeBlock();
//		assertEquals(expected, b);
	}
	
	@Test
	void testEmpty2() throws Exception {
		String input = "";
		ASTNode n = parseAndShow(input);
		Block b = Expressions.makeBlock();
		Chunk expected = new Chunk(b.firstToken,b);
		assertEquals(expected,n);
	}
	
	@Test
	void testAssign1() throws Exception {
		String input = "a=b";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("a"));
		List<Exp> rhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("b"));
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);
	}
	
	@Test
	void testAssignChunk1() throws Exception {
		String input = "a=b";
		ASTNode c = parseAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("a"));
		List<Exp> rhs = Expressions.makeExpList(Expressions.makeExpNameGlobal("b"));
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block b = Expressions.makeBlock(s);
		Chunk expected = new Chunk(b.firstToken,b);
		assertEquals(expected,c);
	}
	
	@Test
	void testMultiAssign1() throws Exception {
		String input = "a,c=8,9";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(
					Expressions.makeExpNameGlobal("a")
					,Expressions.makeExpNameGlobal("c"));
		Exp e1 = Expressions.makeExpInt(8);
		Exp e2 = Expressions.makeExpInt(9);
		List<Exp> rhs = Expressions.makeExpList(e1,e2);
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);		
	}
	
	@Test
	void testMultiAssign3() throws Exception {
		String input = "a,c=8,f(x)";
		Block b = parseBlockAndShow(input);		
		List<Exp> lhs = Expressions.makeExpList(
					Expressions.makeExpNameGlobal("a")
					,Expressions.makeExpNameGlobal("c"));
		Exp e1 = Expressions.makeExpInt(8);
		List<Exp> args = new ArrayList<>();
		args.add(Expressions.makeExpNameGlobal("x"));
		Exp e2 = Expressions.makeExpFunCall(Expressions.makeExpNameGlobal("f"),args, null);
		List<Exp> rhs = Expressions.makeExpList(e1,e2);
		StatAssign s = Expressions.makeStatAssign(lhs,rhs);
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,b);			
	}
	
	@Test
	void testAssignToTable() throws Exception {
		String input = "g.a.b = 3";
		Block bl = parseBlockAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp v = Expressions.makeExpTableLookup(gtable, b);
		Exp three = Expressions.makeExpInt(3);		
		Stat s = Expressions.makeStatAssign(Expressions.makeExpList(v), Expressions.makeExpList(three));;
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,bl);
	}
	
	@Test
	void testAssignTableToVar() throws Exception {
		String input = "x = g.a.b";
		Block bl = parseBlockAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp e = Expressions.makeExpTableLookup(gtable, b);
		Exp v = Expressions.makeExpNameGlobal("x");		
		Stat s = Expressions.makeStatAssign(Expressions.makeExpList(v), Expressions.makeExpList(e));;
		Block expected = Expressions.makeBlock(s);
		assertEquals(expected,bl);
	}
	
	@Test
	void testmultistatements6() throws Exception {
		String input = "x = g.a.b ; ::mylabel:: do  y = 2 goto mylabel f=a(0,200) end break"; //same as testmultistatements0 except ;
		ASTNode c = parseAndShow(input);
		ExpName g = Expressions.makeExpNameGlobal("g");
		ExpString a = Expressions.makeExpString("a");
		Exp gtable = Expressions.makeExpTableLookup(g,a);
		ExpString b = Expressions.makeExpString("b");
		Exp e = Expressions.makeExpTableLookup(gtable, b);
		Exp v = Expressions.makeExpNameGlobal("x");		
		Stat s0 = Expressions.makeStatAssign(v,e);
		StatLabel s1 = Expressions.makeStatLabel("mylabel");
		Exp y = Expressions.makeExpNameGlobal("y");
		Exp two = Expressions.makeExpInt(2);
		Stat s2 = Expressions.makeStatAssign(y,two);
		Stat s3 = Expressions.makeStatGoto("mylabel");
		Exp f = Expressions.makeExpNameGlobal("f");
		Exp ae = Expressions.makeExpNameGlobal("a");
		Exp zero = Expressions.makeExpInt(0);
		Exp twohundred = Expressions.makeExpInt(200);
		List<Exp> args = Expressions.makeExpList(zero, twohundred);
		ExpFunctionCall fc = Expressions.makeExpFunCall(ae, args, null);		
		StatAssign s4 = Expressions.makeStatAssign(f,fc);
		StatDo statdo = Expressions.makeStatDo(s2,s3,s4);
		StatBreak statBreak = Expressions.makeStatBreak();
		Block expectedBlock = Expressions.makeBlock(s0,s1,statdo,statBreak);
		Chunk expectedChunk = new Chunk(expectedBlock.firstToken, expectedBlock);
		assertEquals(expectedChunk,c);
	}
	
	@Test
	void testStatComplex() throws Exception {
		String block12 = "goto Magic ";
		String block11 = "; ";
		String block10 = "break ";
		String block9 = "local sevensixer, Rockets, Thunder = 7 + 8, 9 * (-10 + 9), \"I am here\" ";
		String block8 = "local function Raptors () " + block12 + "end ";
		String block7 = "function University.college.building.classroom:student (age, ID, gender, country, race) " + block11 + "end ";
		String block6 = "for Grizzles, Clippers, Caves in \"Nice to meet you\", -4+5*5 % 9 do " + block10 + "end ";
		String exp4 = "function (Heat, Lakers, Mavericks) " + block9 + "end ";
		String block5 = "for bucks = " + "... " + ", " + "false" + ", " + "nil " + "do " + block8 + "end ";
		String exp3 = "5 + 15 / (18 - 9) ";
		String exp2 = "(3*2)[3+2]:ID {[Monday] = \"The day before Tuesday\", Weather = good, -3 + (5 / 9)} ";
		String block4 = "if " + exp3 + "then " + block5 + "elseif " + exp4 + "then " + block6 + "else " + block7 + "end ";
		String block3 = "repeat " + block4 + "until " + exp2;
		String exp1 = "true ";
		String block2 = "while " + exp1 + "do " + block3 + "end ";
		String stat4 = "do " + block2 + "end ";
		String stat3 = "break ";
		String stat2 = ":: Monday :: ";
		String stat1 = "; ";
		String restat = "return; ";
		String stat = stat1 + stat2 + stat3 + stat4;
		String block1 = stat + restat;
		String parlist = "(University, of, Florida, ...) ";
		String input = "function " + parlist + block1 + "end";
		
		Exp e = parseExpAndShow(input);
	}
	
	
	@Test
	void testBasic0() throws Exception {
		String input = "function (today, is, a, good, day, ...) function America.Florida.Gainesville : Florida (University, of, Florida, ...) break end end";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testStatBasic() throws Exception {
		String input = "function (university, college) hello, (3 * 4).student : ID {[3*4] = 12, age = 15}[\"OK\"].parent.gender.ID = 1 + 2 end";
		Exp e = parseExpAndShow(input);
	}
	
	
	
	@Test
	void testStatdo() throws Exception {
		String input = "do local hello, world = 1 + 2, (3 / 5) end";
		ASTNode e = parseAndShow(input);
	}
	
	@Test
	void testIdent0() throws Exception {
		String input = "x";
		Exp e = parseExpAndShow(input);
		assertEquals(ExpName.class, e.getClass());
		assertEquals("x", ((ExpName) e).name);
	}

	@Test
	void testIdent1() throws Exception {
		String input = "(x)";
		Exp e = parseExpAndShow(input);
		assertEquals(ExpName.class, e.getClass());
		assertEquals("x", ((ExpName) e).name);
	}

	@Test
	void testString() throws Exception {
		String input = "\"string\"";
		Exp e = parseExpAndShow(input);
		assertEquals(ExpString.class, e.getClass());
		assertEquals("string", ((ExpString) e).v);
	}

	@Test
	void testBoolean0() throws Exception {
		String input = "true";
		Exp e = parseExpAndShow(input);
		assertEquals(ExpTrue.class, e.getClass());
	}

	@Test
	void testBoolean1() throws Exception {
		String input = "false";
		Exp e = parseExpAndShow(input);
		assertEquals(ExpFalse.class, e.getClass());
	}


	@Test
	void testBinary0() throws Exception {
		String input = "1 + 2";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(1,OP_PLUS,2);
		show("expected="+expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testUnary0() throws Exception {
		String input = "-2";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeExpUnary(OP_MINUS, 2);
		show("expected="+expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testUnary1() throws Exception {
		String input = "-*2\n";
		assertThrows(SyntaxException.class, () -> {
		Exp e = parseExpAndShow(input);
		});	
	}
	

	
	@Test
	void testRightAssoc() throws Exception {
		String input = "\"concat\" .. \"is\"..\"right associative\"";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(
				Expressions.makeExpString("concat")
				, DOTDOT
				, Expressions.makeBinary("is",DOTDOT,"right associative"));
		show("expected=" + expected);
		assertEquals(expected,e);
	}
	
	@Test
	void testLeftAssoc() throws Exception {
		String input = "\"minus\" - \"is\" - \"left associative\"";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(
				Expressions.makeBinary(
						Expressions.makeExpString("minus")
				, OP_MINUS
				, Expressions.makeExpString("is")), OP_MINUS, 
				Expressions.makeExpString("left associative"));
		show("expected=" + expected);
		assertEquals(expected,e);
		
	}

	@Test
	void testMathExp() throws Exception {
		String input = "3 + 4 * 5";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(Expressions.makeInt(3), OP_PLUS, Expressions.makeBinary(4, OP_TIMES, 5));
		show("expected=" + expected);
		assertEquals(expected, e);
	}
	
	@Test
	void testPrenExp() throws Exception {
		String input = "(3 + 4) * 5";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(Expressions.makeBinary(3, OP_PLUS, 4), OP_TIMES, Expressions.makeInt(5));
		show("expected=" + expected);
		assertEquals(expected, e);
	}
	
	@Test
	void testFunc0() throws Exception {
		String input = "function (abc) end";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpFunction) e).toString());
	}

	@Test
	void testFunc1() throws Exception {
		String input = "function (abc, char, boolean) end";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpFunction) e).toString());
	}
	
	@Test
	void testFunc2() throws Exception {
		String input = "function (int, char, long, get, put,...) end";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpFunction) e).toString());
	}
	
	@Test
	void testFunc3() throws Exception {
		String input = "function (int, char, long, get, put,,) end";
		assertThrows(SyntaxException.class, () -> {
			Exp e = parseExpAndShow(input);
			});	
	}
	
	@Test
	void testFunc4() throws Exception {
		String input = "function (...) end";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpFunction) e).toString());
	}
	
	@Test
	void testTable0() throws Exception {
		String input = "{abc = 123}";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpTable) e).toString());
	}
	
	@Test
	void testTable1() throws Exception {
		String input = "{[5 + 6] = 11}";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpTable) e).toString());
	}
	
	@Test
	void testTable2() throws Exception {
		String input = "{abc = 123, int = char, false;}";
		Exp e = parseExpAndShow(input);
		
		System.out.println(((ExpTable) e).toString());
	}
	
	@Test
	void testTable3() throws Exception {
		String input = "{name = Tom, [3 + 5] = 8, age = 15,}";
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testComplex0() throws Exception {
		String input = "(5 + 3 * 15 / (5 + 4)) * 9 - 3";
		Exp e = parseExpAndShow(input);
		Exp expected = Expressions.makeBinary(
				Expressions.makeBinary(
						Expressions.makeBinary(
								Expressions.makeInt(5), 
								OP_PLUS, 
								Expressions.makeBinary(
										Expressions.makeBinary(
												3, 
												OP_TIMES, 
												15), 
										OP_DIV, 
										Expressions.makeBinary(
												5, 
												OP_PLUS, 
												4))), 
						OP_TIMES, 
						Expressions.makeInt(9)), 
				OP_MINUS, 
				Expressions.makeInt(3)
		);
		assertEquals(expected, e);
	}
	
	@Test
	void testComplex1() throws Exception {
		String input = "#parsing + -75 + 28 * ((function (int, long, boolean) end) - {name;})";
		
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testComplex2() throws Exception {
		String input = "{[boolean == true] = false | true, char, long = int + int} + --The comment should be skipped\n 15";
		
		Exp e = parseExpAndShow(input);
	}
	
	@Test
	void testOfficial0() throws Exception {
		String input = "\"hello \" .. \"there\"";
		Exp e = parseExpAndShow(input);
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
