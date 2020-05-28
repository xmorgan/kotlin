interface A
interface B
class Inv<T>(e: T)

fun <S> intersection(x: Inv<in S>, y: Inv<in S>): S = TODO()

fun <K> use(k: K, f: K.(K) -> K) {}
fun <K> useNested(k: K, f: Inv<K>.(Inv<K>) -> Inv<K>) {}

fun test(a: Inv<A>, b: Inv<B>) {
    val intersectionType = intersection(a, b)

    use(intersectionType) { intersectionType }
    useNested(intersectionType) { Inv(intersectionType) }

    val d by lazy { intersectionType }
}
