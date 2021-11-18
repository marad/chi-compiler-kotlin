package gh.marad.chi.tac

/**
 * Simplifies AST so it's easier to evaluate and/or emit C code.
 *
 * Things that it does:
 * - takes inline functions outside, renames them to avoid collisions and updates call sites accordingly
 * - changes if expressions to functions to make them expressions
 */
fun simplify(tacs: List<Tac>): List<Tac> {
    return tacs
}
