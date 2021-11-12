lexer grammar ChiLexer;

fragment DIGIT : [0-9] ;
fragment LETTER : [a-zA-Z] ;

VAL : 'val';
VAR : 'var';
FN : 'fn';
IF : 'if';
ELSE : 'else';


ARROW : '->' ;
COLON : ':' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
COMMA : ',' ;
DB_QUOTE : '"' -> pushMode(STRING_READING) ;
EQUALS : '=' ;
ADD_SUB : '+' | '-' ;
MUL_DIV : '*' | '/' ;

TRUE : 'true' ;
FALSE : 'false' ;

NUMBER : DIGIT+ ('.' DIGIT+)? ;
ID : LETTER (LETTER | DIGIT | '-' | '_')* ;
NEWLINE : ('\r'? '\n' | '\r')+ -> skip;
WHITESPACE : [ \t\r\n]+ -> skip ;

SINGLE_LINE_COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

mode STRING_READING;

CLOSE_STRING : '"' -> popMode;
STRING_TEXT : ~('\\' | '"' )+ ;

STRING_ESCAPE
    : '\\' ('r' | 'n' | '"')
    ;
