package cop5556fa19;

import static cop5556fa19.Token.Kind.*;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class Scanner {
	
	Reader r;
	StringBuilder tmp_token = new StringBuilder();
	String token_str;
	Token return_token;
	Token.Kind tmp_kind;
	int int_ch, pos = 0, line = 0, tmp_pos = 0, tmp_line = 0, comment_flag = -1, stringlit_flag = -2, len;
	int int_val;
	char ch;
	boolean if_new_line = true, if_read = true, end_flag = false, pos_flag = true;
	
	@SuppressWarnings({ "serial"})
	Map<String, Token.Kind> keyword_map = new HashMap<String, Token.Kind>() {
		{
			this.put("and", KW_and);
			this.put("break", KW_break);
			this.put("do", KW_do);
			this.put("else", KW_else);
			this.put("elseif", KW_elseif);
			this.put("end", KW_end);
			this.put("false", KW_false);
			this.put("for", KW_for);
			this.put("function", KW_function);
			this.put("goto", KW_goto);
			this.put("if", KW_if);
			this.put("in", KW_in);
			this.put("local", KW_local);
			this.put("nil", KW_nil);
			this.put("not", KW_not);
			this.put("or", KW_or);
			this.put("repeat", KW_repeat);
			this.put("return", KW_return);
			this.put("then", KW_then);
			this.put("true", KW_true);
			this.put("until", KW_until);
			this.put("while", KW_while);
		}
	};
	
	@SuppressWarnings("serial")
	Map<String, Token.Kind> other_token_map = new HashMap<String, Token.Kind>() {
		{
			this.put("+", OP_PLUS);
			this.put("-", OP_MINUS);
			this.put("*", OP_TIMES);
			this.put("/", OP_DIV);
			this.put("%", OP_MOD);
			this.put("^", OP_POW);
			this.put("#", OP_HASH);
			this.put("&", BIT_AMP);
			this.put("~", BIT_XOR);
			this.put("|", BIT_OR);
			this.put("<<", BIT_SHIFTL);
			this.put(">>", BIT_SHIFTR);
			this.put("//", OP_DIVDIV);
			this.put("==", REL_EQEQ);
			this.put("~=", REL_NOTEQ);
			this.put("<=", REL_LE);
			this.put(">=", REL_GE);
			this.put("<", REL_LT);
			this.put(">", REL_GT);
			this.put("=", ASSIGN);
			this.put("(", LPAREN);
			this.put(")", RPAREN);
			this.put("{", LCURLY);
			this.put("}", RCURLY);
			this.put("[", LSQUARE);
			this.put("]", RSQUARE);
			this.put("::", COLONCOLON);
			this.put(";", SEMI);
			this.put(":", COLON);
			this.put(",", COMMA);
			this.put(".", DOT);
			this.put("..", DOTDOT);
			this.put("...", DOTDOTDOT);
		}
	};
	
	@SuppressWarnings("serial")
	Map<String, Character> escape_sequence_map = new HashMap<String, Character>() {
		{
			this.put("\\a", '\u0007');
			this.put("\\b", '\b');
			this.put("\\f", '\f');
			this.put("\\n", '\n');
			this.put("\\r", '\r');
			this.put("\\t", '\t');
			this.put("\\v", '\u000B');
			this.put("\\\\", '\\');
			this.put("\\\'", '\'');
			this.put("\\\"", '\"');
		}
	};
	
	@SuppressWarnings("serial")
	public static class LexicalException extends Exception {	
		public LexicalException(String arg0) {
			super(arg0);
		}
	}
	
	public Scanner(Reader r) throws IOException {
		this.r = r;
	}


	public Token getNext() throws Exception {
		stringlit_flag = -2;
		if (if_read) {
			int_ch = r.read();
		}
		if_read = true;
		if (int_ch == -1) {
			return_token = new Token(EOF, "eof", pos, line);
			return return_token;
		}
		
		while (!end_flag) {
			//System.out.println("\n");
			
			if (int_ch == -1) {
				end_flag = true;
				if (tmp_kind != null) {
					return new Token(tmp_kind, token_str, pos - 1, line);
				}
				else {
					if (comment_flag == 0) {
						throw new LexicalException("Error comment at line: " + tmp_line + " pos: " + tmp_pos);
					}
					else if (stringlit_flag == 0 || stringlit_flag == 1) {
						throw new LexicalException("Error stringlit at line: " + tmp_line + " pos " + tmp_pos);
					}
					else {
						return new Token(EOF, "eof", pos - 1, line);
					}
				}
			}
			
			ch = (char)int_ch;
			if (pos_flag) {
				tmp_pos ++;
			}
			
			if (If_WhiteSpace(ch)) {
				if (If_LineTerminator(ch)) {
					if (comment_flag == 0) {
						tmp_token.delete(0, tmp_token.length());
						tmp_kind = null;
						int_ch = r.read();
						comment_flag = -1;
						stringlit_flag = -2;
						//System.out.println("Is a comment");
						//System.out.println("String flag:" + stringlit_flag);
						continue;
					}
					
					if (ch == '\r' && pos_flag) {
						tmp_line ++;
						tmp_pos = 0;
						if_new_line = false;
					}
					else {
						if (if_new_line && pos_flag) {
							tmp_line ++;
							tmp_pos = 0;
						}
					}
				}
				if_new_line = true;
				if (comment_flag != -1) {
					tmp_token.append(ch);
					int_ch = r.read();
					pos_flag = true;
					continue;
				}
				if (stringlit_flag != -1 && stringlit_flag != -2) {
					tmp_token.append(ch);
					stringlit_flag = If_StringLiteral(tmp_token, stringlit_flag);
					int_ch = r.read();
					pos_flag = true;
					continue;
				}
				else {
					if (tmp_kind == null) {
						int_ch = r.read();
						pos_flag = true;
						continue;
					}
					else {
						return_token = new Token(tmp_kind, token_str, pos - 1, line);
						tmp_kind = null;
						token_str = "";
						tmp_token.delete(0, tmp_token.length());
						if_read = false;
						pos_flag = false;
						return return_token;
					}
				}
			}
			else {
				tmp_token.append(ch);
				//System.out.println(stringlit_flag);
				//System.out.println("Current tmp_token:" + tmp_token);
				
				if (comment_flag == -1 && ch == '-' && tmp_token.length() == 1) {
					tmp_kind = OP_MINUS;
					token_str = tmp_token.toString();
					pos = tmp_pos;
					line = tmp_line;
				}
				
				comment_flag = If_Comments(tmp_token);
				stringlit_flag = If_StringLiteral(tmp_token, stringlit_flag);
				//System.out.println("Comment Flag: " + comment_flag + "\n" + "String Flag: " + stringlit_flag) ;
				
				if (comment_flag != -1) {
					if (comment_flag == 0) {
						//System.out.println("Is half comment");
						int_ch = r.read();
						//System.out.println("Here is:" + int_ch);
						pos_flag = true;
						continue;
					}
				}
				
				else if (If_Keyword(tmp_token) != "") {
					//System.out.println("Is keyword");
					tmp_kind = keyword_map.get(tmp_token.toString());
					token_str = tmp_token.toString();
					pos = tmp_pos;
					line = tmp_line;
					int_ch = r.read();
					pos_flag = true;
					continue;
				}
				
				else if (If_Name(tmp_token)) {
					//System.out.println("Is name");
					tmp_kind = NAME;
					token_str = tmp_token.toString();
					pos = tmp_pos;
					line = tmp_line;
					int_ch = r.read();
					pos_flag = true;
					continue;
				}
				
				else if (If_IntegerLiteral(tmp_token)) {
					try {
						//System.out.println("Is numlit");
						int_val = Integer.parseInt(tmp_token.toString());
						tmp_kind = INTLIT;
						token_str = tmp_token.toString();
						pos = tmp_pos;
						line = tmp_line;
						int_ch = r.read();
						pos_flag = true;
						continue;
					}catch (NumberFormatException e) {
						throw new LexicalException("IntegerLiteral value our of range at line: " + tmp_line + " pos: " + tmp_pos);
					}
				}
				else if (stringlit_flag != -1 && stringlit_flag != -2) {
					if (stringlit_flag == 2) {
						//System.out.println("Is stringlit");
						
						return_token = new Token(STRINGLIT, tmp_token.toString(), tmp_pos - 1, tmp_line);
						tmp_kind = null;
						tmp_token.delete(0, tmp_token.length());
						token_str = "";
						if_read = true;
						pos_flag = true;
						return return_token;
					}
					else {
						//System.out.println("Is half stringlit");
						int_ch = r.read();
						pos_flag = true;
						continue;
					}
				}
				
				else if (If_OtherToken(tmp_token) != "") {
					//System.out.println("Is othertoken");
					tmp_kind = other_token_map.get(tmp_token.toString());
					token_str = tmp_token.toString();
					pos = tmp_pos;
					line = tmp_line;
					int_ch = r.read();
					pos_flag = true;
					continue;
				}
				else {
					if (tmp_kind == null) {
						throw new LexicalException("No token match at line: " + tmp_line + " pos: " + tmp_pos);
					}
					else {
						return_token = new Token(tmp_kind, token_str, pos - 1, line);
						tmp_kind = null;
						tmp_token.delete(0, tmp_token.length());
						token_str = "";
						if_read = false;
						pos_flag = false;
						return return_token;
					}
				}
			}
		}
		return new Token(EOF, "eof", pos, line);
	}

	public boolean If_LineTerminator(char ch) {
		if (ch == '\n' || ch == '\r') {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean If_WhiteSpace(char ch) {
		if (ch == ' ' || ch == '\t' || ch == '\f' || If_LineTerminator(ch)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int If_Comments(StringBuilder tmp_token) {
		int len = tmp_token.length();
		if (tmp_token.charAt(0) == '-') {
			if (len < 2) {
				return 0;
			}
			else {
				if (tmp_token.charAt(1) != '-') {
					return -1;
				}
				else {
					tmp_kind = null;
					//System.out.println("The last of comment is:" + tmp_token.charAt(len - 1));
					if (If_LineTerminator(tmp_token.charAt(len - 1))) {
						return 1;
					}
					else {
						return 0;
					}
				}
			}
		}
		else {
			return -1;
		}
	}
	
	public boolean If_Name(StringBuilder tmp_token) {
		if (If_IdentifierChars(tmp_token) && If_Keyword(tmp_token).toString() == "") {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean If_IdentifierChars(StringBuilder tmp_token) {
		for (int i = 0; i < tmp_token.length(); i++) {
			if (i == 0) {
				if (!If_IdentifierStart(tmp_token.charAt(i))) {
					return false;
				}
			}
			else {
				if (!If_IdentifierPart(tmp_token.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean If_IdentifierStart(char ch) {
		if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean If_IdentifierPart(char ch) {
		if (If_IdentifierStart(ch) || If_Digit(ch) || ch == '_' || ch == '$') {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int If_StringLiteral(StringBuilder tmp_token, int flag) throws Exception {
		int len = tmp_token.length();
		char type = tmp_token.charAt(0);
		
		if (flag == -1) {
			return -1;
		}
		else {
			if (len == 1) {
				if (tmp_token.charAt(0) != '\"' && tmp_token.charAt(0) != '\'') {
					//System.out.println("Fail to find quote");
					return -1;
				}
				else {
					return 0;
				}
			}
			else {
				if (tmp_token.charAt(len - 1) == type) {
					return 2;
				}
				else {
					if (flag == 1) { //if last char is \
						String seq = Character.toString(tmp_token.charAt(len - 2)) + Character.toString(tmp_token.charAt(len - 1));
						if (escape_sequence_map.containsKey(seq)) {
							String tmp_esp = escape_sequence_map.get(seq).toString();
							tmp_token.replace(len - 2, len, tmp_esp);
							if (tmp_esp == "\\n" || tmp_esp == "\\r") {
								tmp_line ++;
							}
							return 0;
						}
						else {
							throw new LexicalException("Error in string: invalid escape sequence");
						}
					}
					else {
						if (tmp_token.charAt(len - 1) == '"' || tmp_token.charAt(len - 1) == '\'') {
							return -1;
						}
						else {
							if (tmp_token.charAt(len - 1) == '\\') {
								return 1;
							}
							else {
								return 0;
							}
						}
					}
				}
			}
		}
	}
	
	public boolean If_IntegerLiteral(StringBuilder tmp_token) {
		if (tmp_token.length() == 1 && tmp_token.charAt(0) == '0') {
			return true;
		}
		else {
			for (int i = 0; i < tmp_token.length(); i++) {
				if (i == 0) {
					if (!If_NonZeroDigit(tmp_token.charAt(i))) {
						return false;
					}
				}
				else {
					if (!If_Digit(tmp_token.charAt(i))) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public boolean If_NonZeroDigit(char ch) {
		if (ch > '0' && ch <= '9') {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean If_Digit(char ch) {
		if (If_NonZeroDigit(ch) || ch == '0') {
			return true;
		}
		else {
			return false;
		}
	}
	
	public String If_OtherToken(StringBuilder tmp_token) {
		return other_token_map.containsKey(tmp_token.toString()) ? tmp_token.toString() : "";
	}
	
	public String If_Keyword(StringBuilder tmp_token) {
		return keyword_map.containsKey(tmp_token.toString()) ? tmp_token.toString() : "";
	}
}
