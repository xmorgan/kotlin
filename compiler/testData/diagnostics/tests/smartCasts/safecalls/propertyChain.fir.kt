// See KT-7290

class MyOtherClass2(val z1: String?)
class MyOtherClass(val z: MyOtherClass2?)

class MyClass(val x: MyOtherClass?)

fun foo(y: MyClass?): Int {
    // x here is smartcast but y is not
    val z = y?.x?.z?.z1?.subSequence(0, y.x.z.z1.length)
    return 1
}
