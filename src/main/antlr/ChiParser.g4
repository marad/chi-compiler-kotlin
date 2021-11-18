parser grammar ChiParser;

options { tokenVocab=ChiLexer; }

program : expression* ;

expression
    : expression NEWLINE # ExprWithNewline
    | assignment # AssignmentExpr
    | name_declaration NEWLINE? #NameDeclarationExpr
    | string # StringExpr
    | expression MUL_DIV expression # BinOp
    | expression MOD expression # BinOp
    | expression ADD_SUB expression # BinOp
    | NOT expression # NotOp
    | expression AND expression # BinOp
    | expression OR expression # BinOp
    | func # FuncExpr
    | fn_call # FnCallExpr
    | if_expr # IfExpr
    | NUMBER # NumberExpr
    | bool # BoolExpr
    | ID # IdExpr
    ;


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
    : FN LPAREN (ID COLON type)? (COMMA ID COLON type)* RPAREN (COLON func_return_type)? LBRACE expression* RBRACE
    ;

func_return_type : type ;

fn_call : ID LPAREN expression? (COMMA expression)* RPAREN ;


string : DB_QUOTE (STRING_TEXT | STRING_ESCAPE)* CLOSE_STRING;

if_expr : IF LPAREN condition RPAREN LBRACE then_expr RBRACE (ELSE LBRACE else_expr RBRACE)? ;
condition : expression ;
then_expr : expression* ;
else_expr : expression* ;

bool : TRUE | FALSE ;
