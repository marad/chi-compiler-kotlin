package gh.marad.chi

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

//class ParsingTest : FreeSpec({
//    "name declaration" - {
//        "should read simple variable/value declaration" {
//            // when
//            val result = compile2("""
//                val x = 5
//                var y = "hello"
//            """.trimIndent())
//
//            // then
//            result.expression(0)
//                .name_declaration()
//                .should {
//                    it.VAR().shouldBeNull()
//                    it.VAL().shouldNotBeNull()
//                    it.ID().text shouldBe "x"
//                    it.expression().text shouldBe "5"
//                }
//
//            result.expression(1)
//                .name_declaration()
//                .should {
//                    it.VAR().shouldNotBeNull()
//                    it.VAL().shouldBeNull()
//                    it.ID().text shouldBe "y"
//                    it.expression().text shouldBe "\"hello\""
//                }
//
//        }
//    }
//
//    "fn parsing" - {
//        "should read basic function expression" {
//            // when
//            val result = parser("fn(x: i32): i32 { x }").func()
//
//            // then
//            result.should {
//                it.ID(0).text shouldBe "x"
//                it.type(0).text shouldBe "i32"
//                it.expression(0).text shouldBe "x"
//            }
//        }
//    }
//
//    "string parsing" - {
//        "should read escaped characters" {
//            // given
//            val string = """
//                "hello \r\n\" world"
//            """.trimIndent()
//
//            // when
//            val result = compile2(string)
//
//            // then
//            result.expression(0)
//                .string()
//                .shouldNotBeNull()
//                .should {
//                    it.getChild(1).text shouldBe "hello "
//                    it.getChild(2).text shouldBe "\\r"
//                    it.getChild(3).text shouldBe "\\n"
//                    it.getChild(4).text shouldBe "\\\""
//                    it.getChild(5).text shouldBe " world"
//                }
//        }
//    }
//
//})