///*
// * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
// * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
// */
//
//package org.jetbrains.kotlin.fir.scopes
//
//import org.jetbrains.kotlin.descriptors.Visibilities
//import org.jetbrains.kotlin.fir.FirSession
//import org.jetbrains.kotlin.fir.declarations.visibility
//import org.jetbrains.kotlin.fir.resolve.ScopeSession
//import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
//import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
//import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
//import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
//
//class FirOverriddenDeclarationsProvider(
//    private val scopeSession: ScopeSession,
//    private val session: FirSession,
//) {
//    private val overrideChecker = FirStandardOverrideChecker(session)
//    private val computedFunctionsOverridden = mutableMapOf<FirNamedFunctionSymbol, Collection<FirNamedFunctionSymbol>>()
//
//    fun getOverriddenFunctions(
//        owner: FirClassSymbol<*>,
//        functionSymbol: FirNamedFunctionSymbol
//    ): Collection<FirNamedFunctionSymbol> {
//        return computedFunctionsOverridden.getOrPut(functionSymbol) {
//            computeOverriddenFunctions(owner, functionSymbol)
//        }
//    }
//
//    private fun computeOverriddenFunctions(
//        owner: FirClassSymbol<*>,
//        functionSymbol: FirNamedFunctionSymbol
//    ): Collection<FirNamedFunctionSymbol> {
//        val fir = functionSymbol.fir
//        if (fir.visibility == Visibilities.PRIVATE) return emptyList()
//        val superTypesScope = FirSuperTypeScope.getSupertypeScopesFromSessionIfComputed(scopeSession, owner) ?: return emptyList()
//        val directOverridden = mutableSetOf<FirNamedFunctionSymbol>()
//
//        superTypesScope.processFunctionsByName(functionSymbol.fir.name) { base ->
//            if (base is FirNamedFunctionSymbol && overrideChecker.isOverriddenFunction(functionSymbol.fir, base.fir)) {
//                directOverridden.add(base)
//            }
//        }
//
//        val allOverridden = mutableSetOf<FirNamedFunctionSymbol>()
//
//        for (direct in directOverridden) {
//            if (allOverridden.add(direct)) {
//                allOverridden.addAll(getOverriddenFunctions())
//            }
//        }
//    }
//}
