package cop5556fa19;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import cop5556fa19.Scanner.LexicalException;

import static cop5556fa19.Token.Kind.*;

class ScannerTest {
	
	//I like this to make it easy to print objects and turn this output on and off
	static boolean doPrint = true;
	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}
	
	

//	 /**
//	  * Example showing how to get input from a Java string literal.
//	  * 
//	  * In this case, the string is empty.  The only Token that should be returned is an EOF Token.  
//	  * 
//	  * This test case passes with the provided skeleton, and should also pass in your final implementation.
//	  * Note that calling getNext again after having reached the end of the input should just return another EOF Token.
//	  * 
//	  */
	
	
	@Test
	void testFailure0() throws Exception {
		Reader r = new StringReader("\" \\ \"");
		Scanner s = new Scanner(r);
		assertThrows(LexicalException.class, ()->{
			s.getNext();
		});
	}
	
	@Test
	void testFailure1() throws Exception {
		Reader r = new StringReader("t \n\n--and this is a comment\n\nand and1 --a comment\n\n\"strings\"\n'stringb'\n12345\n12 345\n");
		Scanner s = new Scanner(r);
		Token t;
		t = s.getNext();
		assertEquals(t.text, "t");
		
		t = s.getNext();
		assertEquals(t.text, "and");
		
		t = s.getNext();
		assertEquals(t.text, "and1");
		
		t = s.getNext();
		assertEquals(t.kind, STRINGLIT);
		assertEquals(t.text, "\"strings\"");
		
		t = s.getNext();
		assertEquals(t.kind, STRINGLIT);
		assertEquals(t.text, "'stringb'");
		
		t = s.getNext();
		assertEquals(t.kind, INTLIT);
		assertEquals(t.text, "12345");
		
		t = s.getNext();
		assertEquals(t.kind, INTLIT);
		assertEquals(t.text, "12");
		
		t = s.getNext();
		assertEquals(t.kind, INTLIT);
		assertEquals(t.text, "345");
		
		t = s.getNext();
		assertEquals(t.kind, EOF);
	}
	
	@Test
	void testFailure2() throws Exception {
		Reader r = new StringReader("aaaaa--comment\n1234--comment\r\nbbbbb");
		Scanner s = new Scanner(r);
		Token t;
		
		t = s.getNext();
		assertEquals(t.kind, NAME);
		
		t = s.getNext();
		assertEquals(t.kind, INTLIT);
		assertEquals(t.text, "1234");
		
		t = s.getNext();
		assertEquals(t.kind, NAME);
		assertEquals(t.text, "bbbbb");
	}
	
	@Test
	void testFailure3() throws Exception {
		Reader r = new StringReader("--cc");
		Scanner s = new Scanner(r);
		Token t;
		
		assertThrows(LexicalException.class, ()->{
			s.getNext();
		});
	}

	/**
	 * Example showing how to create a test case to ensure that an exception is thrown when illegal input is given.
	 * 
	 * This "@" character is illegal in the final scanner (except as part of a String literal or comment). So this
	 * test should remain valid in your complete Scanner.
	 */
	@Test
	void test1() throws Exception {
		Reader r = new StringReader("@");
		Scanner s = new Scanner(r);
        assertThrows(LexicalException.class, ()->{
		   s.getNext();
        });
	}
	
	/**
	 * Another example.  This test case will fail with the provided code, but should pass in your completed Scanner.
	 * @throws Exception
	 */
	@Test
	void test3() throws Exception {
		Reader r = new StringReader(",,::==");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind,COMMA);
		assertEquals(t.text,",");
		
		show(t = s.getNext());
		assertEquals(t.kind,COMMA);
		assertEquals(t.text,",");
		
		show(t = s.getNext());
		assertEquals(t.kind,COLONCOLON);
		assertEquals(t.text,"::");
		
		show(t = s.getNext());
		assertEquals(t.kind,REL_EQEQ);
		assertEquals(t.text,"==");
	}
//	
	/**
	 * This test case will test NAME
	 * @throws Exception
	 */
	@Test
	void test4() throws Exception {
		Reader r = new StringReader("abcdefghi0daf1234_dfaeweASLF$");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind, NAME);
		assertEquals(t.text, "abcdefghi0daf1234_dfaeweASLF$");
	}
//	
	/**
	 * This test case will test Keyword
	 * @throws Exception
	 */
	@Test
	void test5() throws Exception {
		Reader r = new StringReader("if");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind, KW_if);
		assertEquals(t.text, "if");
	}
//	
	/**
	 * This test case will test INTLIT
	 * @throws Exception
	 */
	@Test
	void test6() throws Exception {
		Reader r = new StringReader("1233423");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind, INTLIT);
		assertEquals(t.text, "1233423");
	}
//	
	/**
	 * This test case will fail due to Integer out of range
	 * @throws Exception
	 */
	@Test
	void test7() throws Exception {
		Reader r = new StringReader("123342675757576573");
		Scanner s = new Scanner(r);
		Token t;
		
		assertThrows(LexicalException.class, ()->{
			   s.getNext();
	    });
	}
//	
	/**
	 * This test case will test STRINGLIT without escape sequence and whitespace
	 * @throws Exception
	 */
	@Test
	void test8() throws Exception {
		Reader r = new StringReader("\"abc\"");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind, STRINGLIT);
	}
//	
	/**
	 * This test case will test STRINGLIT without escape sequence but whitespace
	 * @throws Exception
	 */
	@Test
	void test9() throws Exception {
		Reader r = new StringReader("\"abc defg 1243 adlf\"");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext());
		assertEquals(t.kind, STRINGLIT);
	}
//	
	/**
	 * This test case will test STRINGLIT with escape sequence and whitespace
	 * @throws Exception
	 */
	@Test
	void test10() throws Exception {
		Reader r = new StringReader("\"abc dedgh \\'dcal;dnl;adn;l\"");
		Scanner s = new Scanner(r);
		Token t;
		show(t = s.getNext());
		assertEquals(t.kind, STRINGLIT);
		assertEquals(t.text, "\"abc dedgh 'dcal;dnl;adn;l\"");
	}
//	
	/**
	 * This test case will test STRINGLIT with error
	 * @throws Exception
	 */
	@Test
	void test11() throws Exception {
		Reader r = new StringReader("\"abc dedgh \\'dcal;dnl;adn;l");
		Scanner s = new Scanner(r);
		Token t;
		assertThrows(LexicalException.class, ()->{
			   s.getNext();
	    });
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test12() throws Exception {
		Reader r = new StringReader(" int  okisee_$$ = (15 + 8) * 23 ** &");
		Scanner s = new Scanner(r);
		Token t;
		
//		show(t = s.getNext()); 
//		assertEquals(t.text, "int");
//		
//		show(t = s.getNext());
//		assertEquals(t.text, "print__");
//		
//		show(t = s.getNext());
//		assertEquals(t.text, "=");
//		
//		show(t = s.getNext());
//		assertEquals(t.text, "(");
//		
//		show(t = s.getNext());
//		assertEquals(t.text, "100");
		
		show(t = s.getNext()); 
		show(t = s.getNext()); 
		show(t = s.getNext()); 
		show(t = s.getNext()); 
		show(t = s.getNext()); 
		show(t = s.getNext()); 
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test13() throws Exception {
		Reader r = new StringReader("int pri;\n" + "5 <= 6\n" + " 6 >2\n" + "5 <=10\n" + "&");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); //int
		show(t = s.getNext()); //pri
		show(t = s.getNext()); //;
		show(t = s.getNext()); //5
		show(t = s.getNext()); //<=
		show(t = s.getNext()); //6
		show(t = s.getNext()); //6
		show(t = s.getNext()); //>
		show(t = s.getNext()); //2
		show(t = s.getNext()); //5
		show(t = s.getNext()); //<=
		show(t = s.getNext()); //10
		show(t = s.getNext()); //&
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test14() throws Exception {
		Reader r = new StringReader("int int_a1 =569;\n" + "%{" + "I love\n cs %}\n" + "long l = 110;");
		Scanner s = new Scanner(r);
		Token t;
		show(t = s.getNext());
		
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test15() throws Exception {
		Reader r = new StringReader("char ch = 'a';\n" + "Character a=33123;\n" + "int arr[5];");
		Scanner s = new Scanner(r);
		Token t;
		show(t = s.getNext()); //char
		show(t = s.getNext()); //ch
		show(t = s.getNext()); //=
		show(t = s.getNext()); //'a'
		show(t = s.getNext()); //;
		show(t = s.getNext()); //Character
		show(t = s.getNext()); //a
		show(t = s.getNext()); //=
		show(t = s.getNext()); //33123
		show(t = s.getNext()); //;
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test16() throws Exception {
		Reader r = new StringReader("string str_len = \"OK Tha\";");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); //string
		show(t = s.getNext()); //String123_
		show(t = s.getNext()); //=
		show(t = s.getNext());
		show(t = s.getNext());
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test17() throws Exception {
		Reader r = new StringReader("int sle=1;\n" + "sleep(sle);");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); //int 
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
		
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test18() throws Exception {
		Reader r = new StringReader("3467 int 888prINt_456 string sleep Sleep String;");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test19() throws Exception {
		Reader r = new StringReader("3467 true1_ 888 fals FALlsefloat flo float boolea %{break%}");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test20() throws Exception {
		Reader r = new StringReader(
				"boolean a;\n" + 
				"int b, x, y;\n" + 
				"char  c;\n" + 
				"long d, t;\n" +
				"string e;\n" +
				"\n" +
				"a = true;\n" +
				"b = 10;\n" +
				"c = 'a';\n" +
				"d = 232;\n" +
				"e = \"Hello, World!\"\n" +
				"a = 1+2;\n" +
				"d = 212 - 1;\n" +
				"a==3;\n" +
				"a  = 1 + 2 *45;\n" +
				"t = (1+2) * 45;\n" +
				"t = (((4-2)*56)/3)+2;\n" +
				"t = 4 - 2 * 56 / 3;\n" +
				"\n" +
				"int score = 100;\n" +
				"\n" +
				"if ( a==100 ) {\n" +
				"    print (\"Value of a is 100\");\n" +
				"}\n" +
				"if ( score > 100 ) {\n" +
				"    print( a );\n" +
				"    print(b);\n" +
				"    print(score);\n" +
				"}");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test21() throws Exception {
		Reader r = new StringReader("int a = 5@;");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		assertThrows(LexicalException.class, ()->{
			   s.getNext();
	    });
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test23() throws Exception {
		Reader r = new StringReader("int a = CISE.05");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test24() throws Exception {
		Reader r = new StringReader("int a = abc123.");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		//show(t = s.getNext());
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test25() throws Exception {
		Reader r = new StringReader("String abc = \"I want to do \\nmany things\"");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
		assertEquals(t.kind, STRINGLIT);
		show(t = s.getNext());
		
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test26() throws Exception {
		Reader r = new StringReader("int a = 'ab'");
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		show(t = s.getNext());
		show(t = s.getNext());
		show(t = s.getNext());
	}
	
	/**
	 * This test case will test expression
	 * @throws Exception
	 */
	@Test
	void test27() throws Exception {
		Reader r = new StringReader(
			"float ANN_123$=sin(5);\n" + 
	   	    "float sin= cos(52)\n" +
    	    "float log1 =log(14)\n" +
			"float atany =  atan(0)\n" +
	        "int a = abs(-5);\n" +
		    "while (a >= 0) a = a - 1;\n" +
		    "a = a | 5;\n" +
		    "a = a & 1;\n" +
	        "a = a > 0 : a : 0;");
		
		Scanner s = new Scanner(r);
		Token t;
		
		show(t = s.getNext()); 
		while (t.kind != EOF) {
			show(t = s.getNext());
		}
	}
}






























