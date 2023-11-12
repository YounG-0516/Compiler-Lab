package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    public SymbolTable table;
    private final Stack<Symbol> tokenStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // no action
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        Symbol curToken1, curToken2;
        Symbol curNonTerminal;
        switch(production.index()){
            case 4:     //S -> D id;
                curToken1 = tokenStack.pop();   //弹出id
                curToken2 = tokenStack.pop();   //弹出D
                // 将符号表中id的type更新为D的type
                this.table.get(curToken1.token.getText()).setType(curToken2.type);
                curNonTerminal = new Symbol(production.head());
                curNonTerminal.type = null;
                tokenStack.push(curNonTerminal);
                break;
            case 5:     //D -> int;
                curToken1 = tokenStack.pop();
                curNonTerminal = new Symbol(production.head());
                curNonTerminal.type = curToken1.type;
                tokenStack.push(curNonTerminal);
                break;
            default:
                for(int i=0; i<production.body().size(); i++){
                    tokenStack.pop();
                }
                tokenStack.push(new Symbol(production.head()));
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        Symbol curSymbol = new Symbol(currentToken);

        if(Objects.equals(currentToken.getKindId(), "int")){
            curSymbol.type = SourceCodeType.Int;
        }else{
            curSymbol.type = null;
        }

        tokenStack.push(curSymbol);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.table = table;
    }
}

