/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirScope {
    open fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {}

    open fun processFunctionsByName(
        name: Name,
        processor: (FirFunctionSymbol<*>) -> Unit
    ) {}

    open fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {}

    open fun processDeclaredConstructors(
        processor: (FirConstructorSymbol) -> Unit
    ) {}

    open fun mayContainName(name: Name) = true

    // Currently, this function has very weak guarantees
    // - It may silently do nothing on symbols originated from different scope instance
    // - It may return the same overridden symbols more then once in case of substitution
    // - It doesn't guarantee any specific order in which overridden tree will be traversed
    // But if the scope instance is the same as the one from which the symbol was originated, this function will enumarate all members
    // of the overridden tree
    // TODO: Consider extracting this function to a separate abstract class/interface, so only limited types of scopes would be supporing it
    // on interface level
    open fun processOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NONE

    // This is just a helper for a common implementation
    protected fun doProcessOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction,
        directOverriddenMap: Map<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>,
        baseScope: FirScope
    ): ProcessorAction {
        val directOverridden =
            directOverriddenMap[functionSymbol] ?: return baseScope.processOverriddenFunctions(functionSymbol, processor)

        for (overridden in directOverridden) {
            if (!processor(overridden)) return ProcessorAction.STOP
            if (!baseScope.processOverriddenFunctions(overridden, processor)) return ProcessorAction.STOP
        }

        return ProcessorAction.NEXT
    }
}

fun FirTypeScope.processOverriddenFunctionsAndSelf(
    functionSymbol: FirFunctionSymbol<*>,
    processor: (FirFunctionSymbol<*>) -> ProcessorAction
): ProcessorAction {
    if (!processor(functionSymbol)) return ProcessorAction.STOP

    return processOverriddenFunctions(functionSymbol, processor)
}

enum class ProcessorAction {
    STOP,
    NEXT,
    NONE;

    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
            NONE -> false
        }
    }

    fun stop() = this == STOP
    fun next() = this != STOP

    operator fun plus(other: ProcessorAction): ProcessorAction {
        if (this == NEXT || other == NEXT) return NEXT
        return this
    }
}


inline fun FirScope.processClassifiersByName(
    name: Name,
    noinline processor: (FirClassifierSymbol<*>) -> Unit
) {
    processClassifiersByNameWithSubstitution(name) { symbol, _ -> processor(symbol) }
}
