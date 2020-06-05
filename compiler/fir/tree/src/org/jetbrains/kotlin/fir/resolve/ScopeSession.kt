/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

class ScopeSession {
    private val scopes = hashMapOf<Any, HashMap<ScopeSessionKey<*, *>, Any>>()

    var returnTypeCalculator: Any? = null

    @Deprecated(level = DeprecationLevel.ERROR, message = "Private for inline")
    fun scopes() = scopes

    fun <ID : Any, FS : Any> getIfComputed(id: ID, key: ScopeSessionKey<ID, FS>): FS? {
        @Suppress("UNCHECKED_CAST")
        return scopes.get(id)?.get(key) as FS?
    }

    inline fun <reified ID : Any, reified FS : Any> getOrBuild(id: ID, key: ScopeSessionKey<ID, FS>, build: () -> FS): FS {
        @Suppress("DEPRECATION_ERROR")
        return scopes().getOrPut(id) {
            hashMapOf()
        }.getOrPut(key) {
            build()
        } as FS
    }
}

abstract class ScopeSessionKey<ID : Any, FS : Any>

