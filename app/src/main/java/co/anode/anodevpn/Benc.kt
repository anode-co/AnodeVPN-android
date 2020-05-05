package co.anode.anodevpn

import kotlin.Long

class Benc(val str: String) {
    var i: Int = 0
    private fun read(n: Int): String {
        val out = str.substring(i, i + n)
        i += out.length
        return out
    }

    private fun readUntil(c: Char): String {
        val out = str.substring(i, str.indexOf(c, i))
        i += out.length + 1
        return out
    }

    abstract class Obj {
        open fun str(): String = throw Error("Not a string")
        open fun num(): Long = throw Error("Not a number")
        open operator fun get(field: String): Obj = throw Error("Not an object")
        open operator fun get(field: Int): Obj = throw Error("Not a list")
        abstract fun bytes(): ByteArray
    }

    class Bnull : Obj() {
        override fun toString(): String = "null"
        override fun bytes(): ByteArray = throw Error("null value, cannot encode")
    }

    class Bstr(private val str: String) : Obj() {
        override fun toString(): String = '"' + str + '"'
        override fun str(): String = str
        override fun bytes(): ByteArray = ("" + str.toByteArray().size + ":" + str).toByteArray()
    }

    class Bint(private val num: Long) : Obj() {
        override fun toString(): String = "" + num
        override fun num(): Long = num
        override fun bytes(): ByteArray = ("i" + num + "e").toByteArray()
    }

    class Blist(private val l: List<Obj>) : Obj() {
        override fun toString(): String = "[" + l.joinToString { x -> x.toString() } + "]"
        override fun get(num: Int): Obj = l[num]
        override fun bytes(): ByteArray = "l".toByteArray() +
                l.map { el: Obj -> el.bytes() }.reduce { a, b -> a + b } +
                "e".toByteArray()
    }

    class Bdict(private val d: Map<Bstr, Obj>) : Obj() {
        override fun toString(): String = "{" +
                d.entries.joinToString { x -> x.key.toString() + ": " + x.value.toString() } +
                "}"

        override fun get(field: String): Obj {
            for (entry in d) {
                if (entry.key.str() == field) {
                    return entry.value
                }
            }
            return Bnull()
        }

        override fun bytes(): ByteArray = "d".toByteArray() +
                d.map { ent -> ent.key.bytes() + ent.value.bytes() }.reduce { a, b -> a + b } +
                "e".toByteArray()
    }

    fun _decode(): Obj? {
        val chr = read(1)[0]
        return when (chr) {
            'i' -> Bint(readUntil('e').toLong())
            'l' -> Blist(ArrayList<Obj>().apply {
                var obj = _decode()
                while (obj != null) {
                    add(obj)
                    obj = _decode()
                }
            })
            'd' -> Bdict(HashMap<Bstr, Obj>().apply {
                var obj = _decode()
                while (obj != null) {
                    val v = _decode()
                    if (v != null) {
                        put(obj as Bstr, v)
                    } else {
                        throw Error("Odd number of elements in bobject")
                    }
                    obj = _decode()
                }
            })
            'e' -> null
            in ('0'..'9') -> Bstr(read((chr + readUntil(':')).toInt()))
            else -> throw Error("Unexpected char: $chr")
        }
    }

    fun decode(): Obj {
        val out = _decode()
        if (out != null ) { return out; }
        throw Error("No object present")
    }

    companion object {
        fun bstr(obj: Any?): Bstr {
            val x = bobj(obj)
            if (x is Bstr) {
                return x
            }
            throw Error("not a string [" + x.javaClass.canonicalName + "]")
        }

        fun bobj(obj: Any?): Obj {
            if (obj == null) {
                throw Error("found null"); }
            if (obj is Obj) {
                return obj; }
            if (obj is Long || obj is Int) {
                return Bint(obj.toString().toLong())
            }
            if (obj is String) {
                return Bstr(obj)
            }
            if (obj is List<*>) {
                return Blist(obj.map { e -> bobj(e) })
            }
            if (obj is Map<*, *>) {
                val m = HashMap<Bstr, Obj>()
                for (entry in obj.entries) {
                    m[bstr(entry.key)] = bobj(entry.value);
                }
                return Bdict(m)
            }
            throw Error("type [" + obj.javaClass.canonicalName + "] is not supported")
        }

        fun dict(vararg obj: Any): Bdict {
            var key: Bstr = Bstr("")
            val out = HashMap<Bstr, Obj>()
            for (i in obj.indices) {
                if (i % 2 == 0) {
                    key = bstr(obj[i])
                } else {
                    out[key] = bobj(obj[i])
                }
            }
            return bobj(out) as Bdict
        }
    }
}