package com.github.jcornaz.kwik.generator.stdlib

import com.github.jcornaz.kwik.generator.api.Generator
import com.github.jcornaz.kwik.generator.api.randomSequence
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("USELESS_IS_CHECK")
class DefaultGeneratorTest {

    @Test
    fun failsForUnsupportedType() {
        assertFailsWith<IllegalArgumentException> {
            Generator.default<DefaultGeneratorTest>()
        }
    }

    @Test
    fun generateInts() {
        assertTrue(Generator.default<Int>().randomSequence(0).first() is Int)
    }

    @Test
    fun generateLongs() {
        assertTrue(Generator.default<Long>().randomSequence(0).first() is Long)
    }

    @Test
    fun generateFloats() {
        assertTrue(Generator.default<Float>().randomSequence(0).first() is Float)
    }

    @Test
    fun generateDoubles() {
        assertTrue(Generator.default<Double>().randomSequence(0).first() is Double)
    }

    @Test
    fun generateBooleans() {
        val value = Generator.default<Boolean>().randomSequence(0).first()
        println(value)
        assertTrue(value is Boolean)
    }

    @Test
    fun generateStrings() {
        assertTrue(Generator.default<String>().randomSequence(0).first() is String)
    }
}
