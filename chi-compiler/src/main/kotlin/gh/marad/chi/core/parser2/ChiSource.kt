package gh.marad.chi.core.parser2

class ChiSource(val code: String) {
    fun getSection(startIndex: Int, endIndex: Int): Section = Section(this, startIndex, endIndex)

    class Section(val source: ChiSource, val start: Int, val end: Int) {
        fun getCode(): String = source.code.substring(start, end + 1)
    }
}