parser grammar ChiParser;

options { tokenVocab=ChiLexer; }

program : (package_definition NEWLINE*?)? (import_definition NEWLINE*?)* ((expression | variantTypeDefinition) NEWLINE*?)* EOF ;

package_definition : 'package' module_name? '/' package_name?;
import_definition : 'import' module_name '/' package_name ('as' package_import_alias)? ('{' (import_entry)+'}')? ;

package_import_alias : ID;
import_entry : import_name ('as' name_import_alias)?;
import_name : ID;
name_import_alias : ID;

module_name : ID ('.' ID)*;
package_name : ID ('.' ID)*;

variantTypeDefinition : 'data' typeName=ID generic_type_definitions? '=' (WS* | NEWLINE*) variantTypeConstructors;
variantTypeConstructors : variantTypeConstructor ( (WS* | NEWLINE*) '|' variantTypeConstructor)*;
variantTypeConstructor : variantName=ID func_argument_definitions? ;

expression
    : expression AS type # Cast
    | receiver=expression PERIOD member=expression # DotOp
    | '(' expression ')' # GroupExpr
    | func # FuncExpr
    | expression callGenericParameters? '(' expr_comma_list ')' # FnCallExpr
    | variable=expression '[' index=expression ']' '=' value=expression # IndexedAssignment
    | variable=expression '[' index=expression ']' # IndexOperator
    | 'while' expression block # WhileLoopExpr
    | assignment # AssignmentExpr
    | func_with_name # FuncWithName
    | name_declaration #NameDeclarationExpr
    | string # StringExpr
    | expression BIT_SHL expression # BinOp
    | expression BIT_SHR expression # BinOp
    | expression MUL expression # BinOp
    | expression DIV expression # BinOp
    | expression MOD expression # BinOp
    | expression ADD_SUB expression # BinOp
    | expression COMP_OP expression # BinOp
    | NOT expression # NotOp
    | expression and expression # BinOp
    | expression or expression # BinOp
    | expression BIT_AND expression # BinOp
    | expression BIT_OR expression # BinOp
    | block # BlockExpr
    | if_expr # IfExpr
    | NUMBER # NumberExpr
    | bool # BoolExpr
    | ID # IdExpr
    ;

and : BIT_AND BIT_AND;
or : BIT_OR BIT_OR;

callGenericParameters
    : '[' type (',' type)* ']'
    ;

expr_comma_list : expression? (COMMA expression)*;

assignment
    : ID EQUALS value=expression
    ;

type
    : ID
    | '(' type? (COMMA type)* ')' ARROW func_return_type
    | generic_type
    ;

generic_type : name=ID '[' type (',' type)* ']' ;

name_declaration
    : (VAL | VAR) ID (COLON type)? EQUALS expression
    ;

func
    : FN func_argument_definitions (COLON func_return_type)? func_body
    ;

func_with_name
    : FN funcName=ID generic_type_definitions? arguments=func_argument_definitions (COLON func_return_type)? func_body
    ;

generic_type_definitions
    : '[' ID (COMMA ID)* ']'
    ;

func_argument_definitions : '(' argumentsWithTypes? ')';
argumentsWithTypes : argumentWithType (',' argumentWithType)*;
argumentWithType : ID ':' type;

func_body : block;
block : '{' NEWLINE* (expression NEWLINE*?)* '}';

func_return_type : type ;

string : DB_QUOTE string_part* CLOSE_STRING;
string_part : STRING_TEXT | STRING_ESCAPE;

if_expr : IF '(' condition ')' then_expr (NEWLINE? ELSE else_expr)? ;
condition : expression ;
then_expr : expression ;
else_expr : expression ;

bool : TRUE | FALSE ;
