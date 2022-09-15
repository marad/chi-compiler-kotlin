package gh.marad.chi.core.namespace

import gh.marad.chi.core.FnType
import gh.marad.chi.core.OverloadedFnType
import gh.marad.chi.core.Type

enum class ScopeType { Package, Function, Virtual }
enum class SymbolType { Local, Argument, Overwrite }
data class SymbolInfo(
    val name: String,
    val type: Type,
    val symbolType: SymbolType,
    val scopeType: ScopeType,
    val slot: Int,
    val mutable: Boolean
)

data class CompilationScope(val type: ScopeType, private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()

    init {
        if (type == ScopeType.Package) {
            assert(parent == null) { "Only package scopes can not have parent scope." }
        }
    }

    fun addSymbol(name: String, type: Type, scope: SymbolType, mutable: Boolean = false) {
        val existingType = getSymbolType(name)
        val finalType = if (type is FnType) {
            if (existingType != type) {
                when (existingType) {
                    is FnType ->
                        OverloadedFnType(setOf(existingType, type))
                    is OverloadedFnType ->
                        existingType.addFnType(type)
                    else ->
                        type
                }
            } else {
                existingType
            }
        } else {
            type
        }
        symbols[name] = SymbolInfo(name, finalType, scope, this.type, -1, mutable)
    }

    fun getSymbolType(name: String): Type? = symbols[name]?.type ?: parent?.getSymbolType(name)

    fun getSymbol(name: String, ignoreOverwrites: Boolean = true): SymbolInfo? =
        symbols[name]?.let {
            if (ignoreOverwrites && it.symbolType == SymbolType.Overwrite) {
                null
            } else {
                it
            }
        } ?: parent?.getSymbol(name, ignoreOverwrites)

    fun containsSymbol(name: String): Boolean = getSymbolType(name) != null

    fun containsInNonVirtualScope(name: String): Boolean = symbols.contains(name)
            || (type == ScopeType.Virtual && parent?.containsInNonVirtualScope(name) == true)

    fun countNonVirtualScopesToName(name: String): Int {
        var nonVirtualScopes = 0
        var currentScope = this
        while (true) {
            if (currentScope.symbols[name] != null) {
                return nonVirtualScopes
            } else {
                currentScope = currentScope.parent ?: TODO("Symbol $name does not exist at all!")
                if (currentScope.type != ScopeType.Virtual) {
                    nonVirtualScopes += 1
                }
            }
        }
    }

    fun updateSlot(name: String, slot: Int) {
        symbols.compute(name) { _, symbol ->
            symbol?.copy(slot = slot)
        }
    }
}
