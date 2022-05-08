package gh.marad.chi.interpreter

// TODO: przepisac to dla nodów?
//class EvalSpec : FunSpec() {
//
//    init {
//
//        test("should read literal value") {
//            EvalModule.getValue(TacScope(), TacValue("5"), Type.i32) shouldBe Value.i32(5)
//        }
//
//        test("should read value from scope by name") {
//            // given
//            val scope = TacScope()
//            scope.define("x", Value.i32(5))
//
//            // expect
//            EvalModule.getValue(scope, TacName("x"), Type.i32) shouldBe Value.i32(5)
//        }
//
//        test("should eval assignment") {
//            // given
//            val scope = TacScope()
//            val expectedValue = Value.i32(5)
//
//            // when
//            val result = eval(scope, TacAssignment("x", Type.i32, TacValue("5")))
//
//            // then
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//        }
//
//        test("should eval assignment op") {
//            // given
//            val scope = TacScope()
//            val expectedValue = Value.i32(5)
//
//            // when
//            val result = eval(scope, TacAssignmentOp("x", Type.i32, TacValue("2"), "+", TacValue("3")))
//
//            // then
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//        }
//
//        test("should eval function call") {
//            // given
//            val scope = TacScope()
//            scope.define("func", Function(
//                params = listOf(FunctionParam("a", Type.i32)),
//                body = listOf(TacReturn(Type.i32, TacName("a"))),
//                Type.i32
//            ))
//            val expectedValue = Value.i32(5)
//
//            // when
//            val result = eval(scope, TacCall("x", Type.i32, "func", parameters = listOf(TacValue("5"))))
//
//            // then
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//        }
//
//        test("should eval declaration") {
//            // given
//            val scope = TacScope()
//            val expectedValue = Value.i32(5)
//
//            // when
//            val result = eval(scope, TacDeclaration("x", Type.i32, TacValue("5")))
//
//            // then
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//        }
//
//        test("should define a function") {
//            // given
//            val scope = TacScope()
//            val expectedValue = Function(
//                params = listOf(FunctionParam("a", Type.i32)),
//                body = listOf(TacReturn(Type.i32, TacName("a"))),
//                type = Type.fn(Type.i32, Type.i32)
//            )
//
//            // when
//            val result = eval(scope, TacFunction(
//                name = "x",
//                type = Type.fn(Type.i32, Type.i32),
//                functionName = "func",
//                paramNames = listOf("a"),
//                body = listOf(TacReturn(Type.i32, TacName("a")))
//            ))
//
//            // then
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//            scope.get("func") shouldBe expectedValue
//        }
//
//        test("should evaluate if-else`s branches") {
//            forAll(
//                Row2(TacValue("true"), Value.i32(1)),
//                Row2(TacValue("false"), Value.i32(2))
//            ) { condition, expectedValue ->
//                // given
//                val scope = TacScope()
//                val thenBranch = listOf(TacReturn(Type.i32, TacValue("1")))
//                val elseBranch = listOf(TacReturn(Type.i32, TacValue("2")))
//
//                // when
//                val result = eval(scope, TacIfElse("x", Type.i32, condition, thenBranch, elseBranch))
//
//                // then
//                result shouldBe expectedValue
//                scope.get("x") shouldBe expectedValue
//            }
//        }
//
//        test("should return unit on if-else`s else branch when it's missing") {
//            // given
//            val scope = TacScope()
//            val thenBranch = listOf(TacReturn(Type.i32, TacValue("1")))
//
//            // when
//            val result = eval(scope, TacIfElse("x", Type.i32, TacValue("false"), thenBranch, null))
//
//            // then
//            val expectedValue = Value.unit
//            result shouldBe expectedValue
//            scope.get("x") shouldBe expectedValue
//        }
//
//        test("should evaluate return") {
//            eval(TacScope(), TacReturn(Type.i32, TacValue("5"))) shouldBe Value.i32(5)
//        }
//
//        test("should evaluate not operator") {
//            val scope = TacScope()
//            eval(scope, TacNot("x", TacValue("true"))) shouldBe Value.bool(false)
//            scope.get("x") shouldBe Value.bool(false)
//
//            eval(scope, TacNot("y", TacValue("false"))) shouldBe Value.bool(true)
//            scope.get("y") shouldBe Value.bool(true)
//        }
//
//        test("should evaluate '&&' operator") {
//            fun check(a: Boolean, b: Boolean, expected: Boolean) {
//                val scope = TacScope()
//                eval(scope, TacAssignmentOp("x", Type.bool, TacValue(a.toString()), "&&", TacValue(b.toString()))) shouldBe
//                        BoolValue(expected)
//                scope.get("x") shouldBe BoolValue(expected)
//            }
//
//            check(a = true, b = true, expected = true)
//            check(a = true, b = false, expected = false)
//            check(a = false, b = true, expected = false)
//            check(a = false, b = false, expected = false)
//        }
//
//        test("should evaluate '||' operator") {
//            fun check(a: Boolean, b: Boolean, expected: Boolean) {
//                val scope = TacScope()
//                eval(scope, TacAssignmentOp("x", Type.bool, TacValue(a.toString()), "||", TacValue(b.toString()))) shouldBe
//                        BoolValue(expected)
//                scope.get("x") shouldBe BoolValue(expected)
//            }
//
//            check(a = true, b = true, expected = true)
//            check(a = true, b = false, expected = true)
//            check(a = false, b = true, expected = true)
//            check(a = false, b = false, expected = false)
//        }
//
////        test("should evaluate '==' operator") {
////            fun check(a: Boolean, b: Boolean, expected: Boolean) {
////                val scope = TacScope()
////                eval(scope, TacAssignmentOp("x", Type.bool, TacValue(a.toString()), "==", TacValue(b.toString()))) shouldBe
////                        BoolValue(expected)
////                scope.get("x") shouldBe BoolValue(expected)
////            }
////
////            check(a = 3, b = 5, expected = )
////        }
//
//    }
//}