package csc426.syntax;

import java.util.ArrayList;
import java.util.List;

import csc426.ast.*;

public class Parser {
	public Parser(Scanner scanner) {
		this.scanner = scanner;
		this.current = scanner.next();
	}

	/**
	 * If the current token matches the given type, return it and advance to the
	 * next token; otherwise print an error message and exit. Harsh.
	 * 
	 * @param type
	 */
	private Token match(TokenType type) {
		if (current.type == type) {
			Token result = current;
			current = scanner.next();
			return result;
		} else {
			System.err.println("Expected " + type + " but found " + current);
			System.exit(1);
			return null; // never reaches this
		}
	}

	/**
	 * Check whether the current token's type is one of a given list of type.
	 * 
	 * @param types a variable number of arguments listing the expected token types
	 * @return true if the current token type is in the given list
	 */
	private boolean check(TokenType... types) {
		for (TokenType type : types) {
			if (current.type == type) {
				return true;
			}
		}
		return false;
	}

	/*-
	 * <Program> --> program id ; <Block> . EOF    FIRST = program
	 */
	public Program parseProgram() {
		match(TokenType.PROGRAM);
		Token nameToken = match(TokenType.ID);
		String name = nameToken.lexeme;
		match(TokenType.SEMI);
		Block block = parseBlock();
		match(TokenType.PERIOD);
		match(TokenType.EOF);
		
		return new Program(name, block);
	}
	
	/*
	 * <Block> --> <ValDecls> <VarDecls> <FunDecls> <Stmt>     FIRST = VAL, VAR, FUN, FIRST(Stmt)
	 */
	public Block parseBlock() {
		ValDecls vs = parseValDecls();
		VarDecls rs = parseVarDecls();
		FunDecls fs = parseFunDecls();
		
		return new Block(vs, rs, fs);
	}
	
	/*
	 * <ValDecls> --> <ValDecl> <ValDecls>		   FIRST = VAL
	 *              | ε							   FOLLOW = VAR
	 */
	public ValDecls parseValDecls() {
		List<ValDecl> vdList = new ArrayList<ValDecl>();
		while (check(TokenType.VAL)) {
			vdList.add(parseValDecl());
		}
		return new ValDecls(vdList);
	}
	
	/*
	 * <ValDecl> --> val id = <Sign> num ;         FIRST = VAL
	 */
	public ValDecl parseValDecl() {
		match(TokenType.VAL);
		Token id = match(TokenType.ID);
		match(TokenType.ASSIGN);
		int sign = parseSign();
		Token numToken = match(TokenType.NUM);
		int num = sign * Integer.parseInt(numToken.lexeme);
		match(TokenType.SEMI);
		return new ValDecl(id.lexeme, num);
	}
	
	/*
	 * <Sign> --> -								   FIRST = MINUS
	 *          | ε								   FOLLOW = NUM
	 */
	public int parseSign() {
		if (check(TokenType.MINUS)) {
			match(TokenType.MINUS);
			return -1;
		}
		return 1;
	}
	
	/*
	 * <VarDecls> --> <VarDecl> <VarDecls>         FIRST = VAR
	 *              | ε                            FOLLOW = FUN, FIRST(Stmt)
	 */
	public VarDecls parseVarDecls() {
		List<VarDecl> vrList = new ArrayList<VarDecl>();
		while (check(TokenType.VAR)) {
			vrList.add(parseVarDecl());
		}
		return new VarDecls(vrList);
	}
	
	/*
	 * <VarDecl> --> var id : <Type> ;             FIRST = VAR
	 */
	public VarDecl parseVarDecl() {
		match(TokenType.VAR);
		Token id = match(TokenType.ID);
		match(TokenType.COLON);
		Type type = parseType();
		match(TokenType.SEMI);
		return new VarDecl(id.lexeme, type);
	}
	
	// Better implementation? Too much redundant code
	public Type parseType() {
		
		if (check(TokenType.INT)) {
			match(TokenType.INT);
			return new Type(TokenType.INT);
		} else if (check(TokenType.BOOL)) {
			match(TokenType.BOOL);
			return new Type(TokenType.BOOL);
		} else if (check(TokenType.VOID)) {
			match(TokenType.VOID);
			return new Type(TokenType.VOID);
		} else {
			System.err.println("Invalid var type: Var type has to be INT, BOOL, or VOID");
			System.exit(1);
		}
		
		return null; // Unreachable
	}
	
	/*
	 * <FunDecls> --> <FunDecl> <FunDecls>         FIRST = FUN
	 *              | ε                            FOLLOW = FIRST(Stmt)
	 */
	public FunDecls parseFunDecls() {
		List<FunDecl> fdList = new ArrayList<FunDecl>();
		while (check(TokenType.FUN)) {
			fdList.add(parseFunDecl());
		}
		return new FunDecls(fdList);
	}
	
	/*
	 * <FunDecl> --> fun id ( <ParamList> ) : <Type> ; <Block> ;    FIRST = FUN
	 */
	public FunDecl parseFunDecl() {
		match(TokenType.FUN);
		Token id = match(TokenType.ID);
		match(TokenType.LPAREN);
		List<Param> ps = parseParamList();
		match(TokenType.RPAREN);
		match(TokenType.COLON);
		Type type = parseType();
		match(TokenType.SEMI);
		Block block = parseBlock();
		match(TokenType.SEMI);
		return new FunDecl(id.lexeme, type, ps, block);
	}
	
	public List<Param> parseParamList() {
		if (check(TokenType.ID))
			return parseParams().getParams();
		else 
			return new ArrayList<Param>();
	}
	
	/*
	 * <Params> --> <Param> <ParamsRest>           FIRST = ID
	 */
	public Params parseParams() {
		List<Param> ps = new ArrayList<Param>();
		Param param = parseParam();
		ps.add(param);
		ps.addAll(parseParamsRest());
		return new Params(ps);
	}
	
	/*
	 * <ParamsRest> --> , <Params>                 FIRST = COMMA
	 *                | ε                          FOLLOW = SEMI
	 */
	public List<Param> parseParamsRest() {
		if (check(TokenType.COMMA)) {
			match(TokenType.COMMA);
			Params ps = parseParams();
			return ps.getParams();
		} else
			return new ArrayList<Param>();
	}
	
	/*
	 * <Param> --> id : <Type>                     FIRST = ID
	 */
	public Param parseParam() {
		Token id = match(TokenType.ID);
		match(TokenType.COLON);
		Type type = parseType();
		return new Param(id.lexeme, type);
	}

	private Scanner scanner;
	private Token current;
}