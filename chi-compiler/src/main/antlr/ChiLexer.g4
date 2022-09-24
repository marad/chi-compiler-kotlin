lexer grammar ChiLexer;

fragment DIGIT : [0-9] ;
fragment LETTER : [a-zA-Z] ;

VAL : 'val';
VAR : 'var';
FN : 'fn';
IF : 'if';
ELSE : 'else';
AS : 'as';
WHILE : 'while';
FOR : 'FOR';
PACKAGE : 'package';
IMPORT : 'import';
DATA : 'data';
WHEN : 'when';
MATCH : 'match';
IS : 'is';
BREAK : 'break';
CONTINUE : 'continue';
EFFECT : 'effect';
HANDLE : 'handle';
WITH : 'with';

ARROW : '->' ;
COLON : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' {
    if (_modeStack.size() > 0) {
        popMode();
    }
};
LSQUARE : '[';
RSQUARE : ']';
COMMA : ',' ;
PERIOD : '.' ;
DB_QUOTE : '"' -> pushMode(STRING_READING) ;
EQUALS : '=' ;

// Weaving operators
WEAVE : '~>' ;
PLACEHOLDER : '_' ;

// Arithmetic operators
ADD_SUB : '+' | '-' ;
MOD : '%' ;
MUL: '*';
DIV : '/' ;

// Logic operators
NOT : '!' ;

// Bitshift operators
BIT_SHL : '<<';
BIT_SHR : '>>';
BIT_AND : '&';
BIT_OR : '|';

// Comparison operators
COMP_OP : IS_EQ | NOT_EQ | LT | LEQ | GT | GEQ ;
IS_EQ : '==' ;
NOT_EQ : '!=' ;
LT : '<' ;
LEQ : '<=' ;
GT : '>' ;
GEQ : '>=' ;

TRUE : 'true' ;
FALSE : 'false' ;

NUMBER : DIGIT+ ('.' DIGIT+)? ;
ID : LETTER (LETTER | DIGIT | '_')* ;
fragment EOL : ('\r'? '\n' | '\r');
NEWLINE : EOL;
WS : [ \t]+ -> skip;

SINGLE_LINE_COMMENT : '//' ~[\r\n]* EOL -> skip ;
MULTI_LINE_COMMENT : '/*' .*? '*/' EOL? -> skip ;

//    : '$' ID
//    | '$' '{' STRING_TEXT
//    ;


STRING_ESCAPE
    : '\\' ('r' | 'n' | '"' | '$')
    ;

mode STRING_READING;

ENTER_EXPR: '${' -> pushMode(DEFAULT_MODE);
ID_INTERP : '$' LETTER (LETTER | DIGIT | '_')* ;
ESCAPED_DOLLAR : '\\$';
ESCAPED_QUOTE : '\\"';
TEXT : ~('\\' | '"' | '$' )+ ;
CLOSE_STRING : '"' -> popMode;

ANY:'.'+;