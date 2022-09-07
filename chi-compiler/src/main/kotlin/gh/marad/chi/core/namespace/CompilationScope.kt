package gh.marad.chi.core.namespace

import gh.marad.chi.core.FnType
import gh.marad.chi.core.OverloadedFnType
import gh.marad.chi.core.Type

enum class SymbolScope { Local, Argument, Package }
data class SymbolInfo(val name: String, val type: Type, val scope: SymbolScope, val slot: Int, val mutable: Boolean)

data class CompilationScope(private val parent: CompilationScope? = null) {
    private val symbols: MutableMap<String, SymbolInfo> = mutableMapOf()
    val isTopLevel = parent == null

    fun addSymbol(name: String, type: Type, scope: SymbolScope, mutable: Boolean = false) {
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
        symbols[name] = SymbolInfo(name, finalType, scope, -1, mutable)
    }

    fun getSymbolType(name: String): Type? = symbols[name]?.type ?: parent?.getSymbolType(name)

    fun getSymbol(name: String): SymbolInfo? = symbols[name] ?: parent?.getSymbol(name)

    fun containsSymbol(name: String): Boolean = getSymbolType(name) != null

    fun containsDirectly(name: String) = symbols.contains(name)

    fun updateSlot(name: String, slot: Int) {
        symbols.compute(name) { _, symbol ->
            symbol?.copy(slot = slot)
        }
    }
}
