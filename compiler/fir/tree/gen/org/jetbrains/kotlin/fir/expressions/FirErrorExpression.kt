/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorExpression : FirPureAbstractElement(), FirExpression, FirDiagnosticHolder {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val diagnostic: FirDiagnostic

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorExpression(this, data)
}
