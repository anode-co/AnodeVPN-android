package com.anode.anode

import kotlin.Int

abstract class Bobj {
    abstract fun bytes(): ByteArray
}
class Bstr(private val str: String) : Bobj() {
    override fun bytes(): ByteArray {
        return ("" + str.toByteArray().size + ":" + str).toByteArray();
    }
}
class Bint(private val num: Int) : Bobj() {
    override fun bytes(): ByteArray {
        return ("i" + num + "e").toByteArray()
    }
}
class Blist(private val l: List<Bobj>) : Bobj() {
    override fun bytes(): ByteArray {
        val out = l.map{ el:Bobj -> el.bytes() }.reduce{ a, b -> a + b }
        return "l".toByteArray() + out + "e".toByteArray()
    }
}
class Bdict(private val d: Map<Bstr,Bobj>) : Bobj() {
    override fun bytes(): ByteArray {
        val out = d.map{ ent -> ent.key.bytes() + ent.value.bytes() }.reduce{ a, b -> a + b }
        return "d".toByteArray() + out + "e".toByteArray()
    }
}
class BobjTools {
    fun bstr(obj: Any?): Bstr {
        val x = bobj(obj)
        if (x is Bstr) { return x }
        throw Error("not a string [" + x.javaClass.canonicalName + "]")
    }
    fun bobj(obj: Any?): Bobj {
        if (obj == null) { throw Error("found null"); }
        if (obj is Bobj) { return obj; }
        if (obj is Int) { return Bint(obj) }
        if (obj is String) { return Bstr(obj) }
        if (obj is List<*>) { return Blist(obj.map{ e -> bobj(e) }) }
        if (obj is Map<*,*>) {
            val m = HashMap<Bstr, Bobj>()
            for (entry in obj.entries) {
                m[bstr(entry.key)] = bobj(entry.value);
            }
            return Bdict(m)
        }
        throw Error("type [" + obj.javaClass.canonicalName + "] is not supported")
    }
    fun dict(vararg obj: Any): Bdict {
        var key: Bstr = Bstr("")
        val out = HashMap<Bstr,Bobj>()
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
    fun decode(): Any {
        val chr = read(1)[0]
        return when (chr) {
            'i' -> readUntil('e').toInt()
            'l' -> ArrayList<Any>().apply {
                var obj = decode()
                while (obj != Unit) {
                    add(obj)
                    obj = decode()
                }
            }
            'd' -> HashMap<String, Any>().apply {
                var obj = decode()
                while (obj != Unit) {
                    put(obj as String, decode())
                    obj = decode()
                }
            }
            'e' -> Unit
            in ('0'..'9') -> read((chr + readUntil(':')).toInt())
            else -> throw Error("Unexpected char: $chr")
        }
    }
}