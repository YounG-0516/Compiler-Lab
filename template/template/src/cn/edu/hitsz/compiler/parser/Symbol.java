package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol{
    Token token;
    NonTerminal nonTerminal;
    SourceCodeType type = null;
    IRValue value = null;

    private Symbol(Token token, NonTerminal nonTerminal){
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token){
        this(token, null);
    }
    public Symbol(NonTerminal nonTerminal){
        this(null, nonTerminal);
    }
    public boolean isToken(){
        return this.token != null;
    }
    public boolean isNonterminal(){
        return this.nonTerminal != null;
    }
}