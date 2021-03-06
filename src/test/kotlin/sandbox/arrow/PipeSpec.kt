package sandbox.arrow

import arrow.syntax.function.pipe
import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec
import sandbox.arrow.effects.allUpper

class PipeSpec : DescribeSpec({
    val pipeStrong: (String) -> String = { body -> "<strong>$body</strong>" }
    describe("Pipe can chain function calls") {
        it("does not produce new functions") {
            val result = "From a pipe".pipe(pipeStrong).pipe(::allUpper)
            result shouldBe "<STRONG>FROM A PIPE</STRONG>"
        }
    }
})
