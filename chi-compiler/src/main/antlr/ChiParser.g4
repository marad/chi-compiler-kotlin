parser grammar ChiParser;

options { tokenVocab=ChiLexer; }

program : ws package_definition? ws import_definition* ws ((expression | variantTypeDefinition) ws)* EOF ;

package_definition : 'package' module_name? '/' package_name? NEWLINE?;
import_definition : 'import' module_name '/' package_name ('as' package_import_alias)? (LBRACE (import_entry ','?)+ RBRACE)? NEWLINE? ;

package_import_alias : ID;
import_entry : import_name ('as' name_import_alias)?;
import_name : ID;
name_import_alias : ID;

module_name : ID ('.' ID)*;
package_name : ID ('.' ID)*;

variantTypeDefinition : fullVariantTypeDefinition | simplifiedVariantTypeDefinition;
fullVariantTypeDefinition: 'data' typeName=ID generic_type_definitions? '=' (WS* | NEWLINE*) variantTypeConstructors;
simplifiedVariantTypeDefinition : 'data' PUB? typeName=ID generic_type_definitions? ('(' variantFields? ')')?;

variantTypeConstructors : variantTypeConstructor ( (WS* | NEWLINE*) '|' variantTypeConstructor)*;
variantTypeConstructor : PUB? variantName=ID ('(' variantFields? ')')? ;

variantFields : ws variantField ws (',' ws variantField ws)*;
variantField: PUB? name=ID ':' type ;

whenExpression : WHEN LBRACE (ws whenConditionCase)+ ws whenElseCase? ws RBRACE ;
whenConditionCase: condition=expression ws '->' ws body=whenCaseBody;
whenElseCase: ELSE ws '->' ws body=whenCaseBody;
whenCaseBody : block | expression;

lambda: LBRACE ws (argumentsWithTypes '->')? ws (expression ws)* RBRACE;
block : LBRACE ws (expression ws)* RBRACE;

effectDefinition : PUB? 'effect' effectName=ID generic_type_definitions? arguments=func_argument_definitions (COLON type)?;
handleExpression : HANDLE ws block ws WITH ws LBRACE ws handleCase*  RBRACE;
handleCase : effectName=ID '(' handleCaseEffectParam (',' handleCaseEffectParam)* ')' ws '->' ws handleCaseBody ws;
handleCaseEffectParam : ID;
handleCaseBody : block | expression;

expression
    : expression AS type # Cast
    | receiver=expression ws PERIOD methodName=ID callGenericParameters? '(' arguments=expr_comma_list ')' # MethodInvocation
    | receiver=expression ws PERIOD memberName=ID '=' value=expression # FieldAssignment
    | receiver=expression ws PERIOD memberName=ID # FieldAccessExpr
    | effectDefinition # EffectDef
    | handleExpression # HandleExpr
    | expression IS variantName=ID  # IsExpr
    | 'while' expression block # WhileLoopExpr
    | whenExpression # WhenExpr
    | '(' expression ')' # GroupExpr
    | expression callGenericParameters? '(' expr_comma_list ')' # FnCallExpr
    | variable=expression '[' index=expression ']' '=' value=expression # IndexedAssignment
    | variable=expression '[' index=expression ']' # IndexOperator
    | assignment # AssignmentExpr
    | func_with_name # FuncWithName
    | name_declaration #NameDeclarationExpr
    | string # StringExpr
    | expression BIT_SHL expression # BinOp
    | expression BIT_SHR expression # BinOp
    | expression divMul expression # BinOp
    | expression MOD expression # BinOp
    | expression plusMinus expression # BinOp
    | expression COMP_OP expression # BinOp
    | NOT expression # NotOp
    | expression and expression # BinOp
    | expression or expression # BinOp
    | expression BIT_AND expression # BinOp
    | expression BIT_OR expression # BinOp
    | lambda # LambdaExpr
    | if_expr # IfExpr
    | input=expression ws WEAVE ws template=expression ws # WeaveExpr
    | variable=ID opEqual value=expression # OpEqualExpr
    | MINUS expression # NegationExpr
    | NUMBER # NumberExpr
    | bool # BoolExpr
    | ID # IdExpr
    | PLACEHOLDER # PlaceholderExpr
    | BREAK # BreakExpr
    | CONTINUE # ContinueExpr
    ;

divMul: DIV | MUL;
plusMinus: PLUS | MINUS;

opEqual: PLUS_EQUAL | MINUS_EQUAL | MUL_EQUAL | DIV_EQUAL;

and : BIT_AND BIT_AND;
or : BIT_OR BIT_OR;

callGenericParameters
    : '[' type (',' type)* ']'
    ;

expr_comma_list : expression? (COMMA expression)*;

assignment
    : ID EQUALS value=expression
    ;

type : typeNameRef | functionTypeRef | typeConstructorRef;
typeNameRef : (packageName=ID '.')? name=ID;
functionTypeRef : '(' type? (COMMA type)* ')' ARROW func_return_type;
typeConstructorRef : typeNameRef '[' type (',' type)* ']';

name_declaration
    : PUB? (VAL | VAR) ID (COLON type)? EQUALS expression
    ;

func_with_name
    : PUB? FN funcName=ID generic_type_definitions? arguments=func_argument_definitions (COLON func_return_type)? func_body
    ;

generic_type_definitions
    : '[' ID (COMMA ID)* ']'
    ;

func_argument_definitions : '(' ws argumentsWithTypes? ')';
argumentsWithTypes : argumentWithType ws (',' argumentWithType ws)*;
argumentWithType : ID ':' type;

func_body : block;

func_return_type : type ;

string : DB_QUOTE stringPart* CLOSE_STRING;
stringPart
    : TEXT
    | ESCAPED_QUOTE
    | ESCAPED_DOLLAR
    | ESCAPED_NEWLINE
    | ESCAPED_CR
    | ESCAPED_SLASH
    | ESCAPED_TAB
    | ID_INTERP
    | ENTER_EXPR expression RBRACE;

if_expr : IF '(' condition=expression ')' then_expr (NEWLINE? ELSE else_expr)? ;
//condition : expression ;
then_expr : block | expression ;
else_expr : block | expression ;

bool : TRUE | FALSE ;

ws : (WS | NEWLINE)* ;
