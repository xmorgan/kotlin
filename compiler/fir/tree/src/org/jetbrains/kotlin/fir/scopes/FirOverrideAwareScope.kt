/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirOverrideAwareScope : FirScope() {
    abstract override fun processOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> Unit
    )
}

fun FirScope.toDummyOverrideAwareScope(): FirOverrideAwareScope = object : FirOverrideAwareScope() {
    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        this@toDummyOverrideAwareScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        this@toDummyOverrideAwareScope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        this@toDummyOverrideAwareScope.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        this@toDummyOverrideAwareScope.processDeclaredConstructors(processor)
    }

    override fun mayContainName(name: Name): Boolean {
        return this@toDummyOverrideAwareScope.mayContainName(name)
    }

    override fun processOverriddenFunctions(functionSymbol: FirFunctionSymbol<*>, processor: (FirFunctionSymbol<*>) -> Unit) {}
}
