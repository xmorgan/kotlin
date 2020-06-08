// FIR_IDENTICAL
// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
interface Foo<T> {
    fun test(p: T) = "fail"
    val T.prop: String
        get() = "fail"
}

interface FooDerived: Foo<String>

class Unspecialized<T> : Foo<T>

open class <!EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE, EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE!>UnspecializedFromDerived<!> : FooDerived

abstract class <!EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE, EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE!>AbstractUnspecializedFromDerived<!> : FooDerived

open class <!EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE, EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE!>Specialized<!> : Foo<String>

abstract class <!EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE, EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE!>AbstractSpecialized<!> : Foo<String>

final class FinalSpecialized : Foo<String>

sealed class SealedSpecialized : Foo<String>

enum class EnumSpecialized : Foo<String>

object ObjectSpecialized : Foo<String>

private class Outer {

    open class InnerSpecialized: Foo<String>
}

fun local() {
    object : Foo<String> {}
}

fun interface F : Foo<String> {
    fun invoke(o: String): String
}

fun test(): String {
    if (F { o -> o + "K" }.invoke("O") != "OK") return "Fail"

    val lambda: (String) -> String = { o -> o + "K" }
    return F(lambda).invoke("O")
}
