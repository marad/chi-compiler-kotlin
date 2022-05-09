parser grammar ChiParser;

options { tokenVocab=ChiLexer; }

program : expression* EOF ;

expression
    : expression AS type # Cast
    | expression NEWLINE # ExprWithNewline
    | '(' expression ')' # GroupExpr
    | assignment # AssignmentExpr
    | name_declaration NEWLINE? #NameDeclarationExpr
    | string # StringExpr
    | expression MUL_DIV expression # BinOp
    | expression MOD expression # BinOp
    | expression ADD_SUB expression # BinOp
    | expression COMP_OP expression # BinOp
    | NOT expression # NotOp
    | expression AND expression # BinOp
    | expression OR expression # BinOp
    | func # FuncExpr
    | block # BlockExpr
    | expression '(' expr_comma_list ')' # FnCallExpr
    | if_expr # IfExpr
    | NUMBER # NumberExpr
    | bool # BoolExpr
    | ID # IdExpr
    ;

expr_comma_list : expression? (COMMA expression)*;


assignment
    : ID EQUALS expression
    ;

type
    : ID
    | LPAREN type? (COMMA type)* RPAREN ARROW func_return_type
    ;

name_declaration
    : (VAL | VAR) ID (COLON type)? EQUALS expression
    ;

func
    : FN LPAREN (ID COLON type)? (COMMA ID COLON type)* RPAREN (COLON func_return_type)? func_body
    ;

func_body : block;
block : LBRACE expression* RBRACE;

func_return_type : type ;

string : DB_QUOTE string_part* CLOSE_STRING;
string_part : STRING_TEXT | STRING_ESCAPE;

if_expr : IF LPAREN condition RPAREN LBRACE then_expr RBRACE (ELSE LBRACE else_expr RBRACE)? ;
condition : expression ;
then_expr : expression* ;
else_expr : expression* ;

bool : TRUE | FALSE ;
