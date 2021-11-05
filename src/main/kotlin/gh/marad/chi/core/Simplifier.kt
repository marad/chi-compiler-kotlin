package gh.marad.chi.core

/**
 * Simplifies AST so it's easier to evaluate and/or emit C code.
 *
 * Things that it does:
 * - takes inline functions outside, renames them to avoid collisions and updates call sites accordingly
 */
fun simplify(asts: List<Expression>): List<Expression> {
    return asts
}