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

ARROW : '->' ;
COLON : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
COMMA : ',' ;
PERIOD : '.' ;
DB_QUOTE : '"' -> pushMode(STRING_READING) ;
EQUALS : '=' ;

// Arithmetic operators
ADD_SUB : '+' | '-' ;
MOD : '%' ;
MUL: '*';
DIV : '/' ;

// Logic operators
NOT : '!' ;
AND : '&&' ;
OR : '||' ;

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
NEWLINE : ('\r'? '\n' | '\r')+ -> skip;
WHITESPACE : [ \t]+ -> skip ;

SINGLE_LINE_COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

mode STRING_READING;

CLOSE_STRING : '"' -> popMode;
STRING_TEXT : ~('\\' | '"' )+ ;

STRING_ESCAPE
    : '\\' ('r' | 'n' | '"')
    ;
