package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    public String sourceString = "";
    public List<Token> tokens = new ArrayList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // 词法分析前的缓冲区实现
        try (BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;
            while ((line = br.readLine()) != null) {
                sourceString += line;
            }
            //System.out.println(sourceString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        //自动机实现的词法分析过程
        String status;
        int i = 0;
        char ch;
        char[] word = sourceString.toCharArray();
        while(i < sourceString.length()) {
            ch = word[i];

            //为了结构更清晰，我们将读入的字符进行分类
            if (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t') {
                //特殊字符
                status = "SKIP";
            } else if (ch == ',' || ch == ';' || ch == '=' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '(' || ch == ')') {
                //仅判断一个字符便可以得出结果的情况
                status = "SINGLE-PUNCTUATION";
            } else if (Character.isLetter(ch)) {
                //关键字和标识符
                status = "LETTER";
            } else if (Character.isDigit(ch)) {
                //数字
                status = "DIGIT";
            } else {
                status = "ERROR";
            }


            //对于每一种状态单独分析，其实也就是自动机的工作过程
            switch (status) {
                case "SKIP" -> ++i;
                case "SINGLE-PUNCTUATION" -> {
                    switch (ch) {
                        case ',' -> tokens.add(Token.simple(","));
                        case ';' -> tokens.add(Token.simple("Semicolon"));
                        case '=' -> tokens.add(Token.simple("="));
                        case '+' -> tokens.add(Token.simple("+"));
                        case '-' -> tokens.add(Token.simple("-"));
                        case '*' -> tokens.add(Token.simple("*"));
                        case '/' -> tokens.add(Token.simple("/"));
                        case '(' -> tokens.add(Token.simple("("));
                        case ')' -> tokens.add(Token.simple(")"));
                        default -> {
                        }
                    }
                    i++;
                }

                case "DIGIT" -> {
                    int index = i;
                    while (Character.isDigit(ch) && (i+1 < sourceString.length())) {
                        ch = word[++i];
                    }
                    String digit = sourceString.substring(index, i);
                    tokens.add(Token.normal("IntConst", digit));
                }

                case "LETTER" -> {
                    int index = i;
                    while (Character.isLetter(ch) && (i+1 < sourceString.length())) {
                        ch = word[++i];
                    }
                    String key = sourceString.substring(index, i);

                    //当前读入字符串为标识符
                    if (TokenKind.isAllowed(key)) {
                        tokens.add(Token.simple(key));
                    } else {
                        //当前读入字符串为关键字
                        tokens.add(Token.normal("id",key));
                        if (!symbolTable.has(key)) {
                            symbolTable.add(key);
                        }
                    }
                }

                default -> System.out.println("errorInput");
            }
        }

        //插入结束符
        tokens.add(Token.eof());
    }


    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 从词法分析过程中获取 Token 列表
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
