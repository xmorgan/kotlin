// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KFunction2
import kotlin.reflect.KSuspendFunction2

interface Foo {
    fun resolve(var1: Int): String
    fun resolve(var1: String): String

    suspend fun resolve2(var1: Int): String
    suspend fun resolve2(var1: String): String
}

fun <K> bar1(f: KFunction2<K, String, String>) {}

fun <K> bar2(f: KFunction2<out K, String, String>) {}

fun <K> bar3(f: Any?) {}

fun <K> bar4(f: Function2<K, String, String>) {}

fun <K> bar5(f: suspend (K, String) -> String) {}

fun <K> bar6(f: KSuspendFunction2<K, String, String>) {}

fun <K> bar7(f: K.(String) -> String) {}

fun resolve(var2: Number, var1: Int) = ""
fun resolve(var2: Number, var1: String) = ""

fun <T : Foo, R: Number> main() {
    // with LHS
    bar1<T>(Foo::resolve) // ERROR before the fix in NI
    bar1<Foo>(Foo::resolve) // OK
    bar1(Foo::resolve) // OK

    // without LHS
    bar1<R>(::resolve) // OK
    bar1<Number>(::resolve) // OK
    bar1(::resolve) // OK

    // with LHS and conflicting projection
    bar2<T>(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)
    bar2<Foo>(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)
    bar2(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)

    // with LHS and Any? expected type
    bar3<T>(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)
    bar3<Foo>(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)
    bar3(<!UNRESOLVED_REFERENCE!>Foo::resolve<!>)

    // with LHS and `Function` expected type
    bar4<T>(Foo::resolve) // ERROR before the fix in NI
    bar4<Foo>(Foo::resolve) // OK
    bar4(Foo::resolve) // OK

    // with LHS and `SuspendFunction` expected type
    bar5<T>(Foo::resolve2) // ERROR before the fix in NI
    bar5<Foo>(Foo::resolve2) // OK
    bar5(Foo::resolve2) // OK

    // with LHS and `KSuspendFunction` expected type
    bar6<T>(Foo::resolve2) // ERROR before the fix in NI
    bar6<Foo>(Foo::resolve2) // OK
    bar6(Foo::resolve2) // OK

    // with LHS and sentension function expected type
    bar7<T>(Foo::resolve) // ERROR before the fix in NI
    bar7<Foo>(Foo::resolve) // OK
    bar7(Foo::resolve) // OK
}
