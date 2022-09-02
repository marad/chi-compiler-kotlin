package gh.marad.chi.core.parser2

class ChiSource(val code: String) {
    fun getSection(startIndex: Int, endIndex: Int): Section = Section(this, startIndex, endIndex)

    data class Section(val source: ChiSource, val start: Int, val end: Int) {
        val startLine get() = lineAt(start)
        val startColumn get() = columnAt(start)
        val endLine get() = lineAt(end + 1)
        val endColumn get() = columnAt(end + 1)
        fun getCode(): String = source.code.substring(start, end + 1)

        private fun lineAt(index: Int) = source.code.substring(0, index).lines().count()
        private fun columnAt(index: Int) = index - source.code.substring(0, index).lastIndexOf('\n') - 1
    }
}