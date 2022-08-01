parser grammar ChiParser;

options { tokenVocab=ChiLexer; }

program : package_definition? import_definition* expression* EOF ;

package_definition : 'package' module_name? '/' package_name? NEWLINE;
import_definition : 'import' module_name? '/' package_name? ('as' package_import_alias)? '{' (import_name ('as' name_import_alias)?)+'}' NEWLINE;

package_import_alias : ID;
import_name : ID;
name_import_alias : ID;

module_name : ID ('.' ID)*;
package_name : ID ('.' ID)*;

expression
    : expression AS type # Cast
    | '(' expression ')' # GroupExpr
    | func # FuncExpr
    | expression '(' expr_comma_list ')' # FnCallExpr
    | fully_qualified_name # FullyQualifiedNameExpr
    | 'while' expression block # WhileLoopExpr
    | assignment # AssignmentExpr
    | name_declaration #NameDeclarationExpr
    | string # StringExpr
    | expression MUL expression # BinOp
    | expression DIV expression # BinOp
    | expression MOD expression # BinOp
    | expression ADD_SUB expression # BinOp
    | expression COMP_OP expression # BinOp
    | NOT expression # NotOp
    | expression AND expression # BinOp
    | expression OR expression # BinOp
    | block # BlockExpr
    | if_expr # IfExpr
    | NUMBER # NumberExpr
    | bool # BoolExpr
    | ID # IdExpr
    ;

fully_qualified_name : module_name '/' package_name '.' ID;

expr_comma_list : expression? (COMMA expression)*;

assignment
    : ID EQUALS expression
    ;

type
    : ID
    | '(' type? (COMMA type)* ')' ARROW func_return_type
    ;

name_declaration
    : (VAL | VAR) ID (COLON type)? EQUALS expression
    ;

func
    : FN '(' (ID COLON type)? (COMMA ID COLON type)* ')' (COLON func_return_type)? func_body
    ;

func_body : block;
block : '{' expression* '}';

func_return_type : type ;

string : DB_QUOTE string_part* CLOSE_STRING;
string_part : STRING_TEXT | STRING_ESCAPE;

if_expr : IF '(' condition ')' then_expr (ELSE else_expr)? ;
condition : expression ;
then_expr : expression ;
else_expr : expression ;

bool : TRUE | FALSE ;
