/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name

class ResolutionScopeWithLastRequestsCached(private val baseResolutionScope: ResolutionScope): ResolutionScope by baseResolutionScope {

    private var cachedContributedClassifierName: Name? = null
    private var cachedContributedClassifier: ClassifierDescriptor? = null

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        if (cachedContributedClassifierName == name) cachedContributedClassifier
        else baseResolutionScope.getContributedClassifier(name, location).also {
            cachedContributedClassifierName = name
            cachedContributedClassifier = it
        }

    private var cachedContributedVariablesName: Name? = null
    private var cachedContributedVariables: Collection<VariableDescriptor>? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
        if (cachedContributedVariablesName == name) cachedContributedVariables!!
        else baseResolutionScope.getContributedVariables(name, location).also {
            cachedContributedVariablesName = name
            cachedContributedVariables = it
        }

    private var cachedContributedFunctionsName: Name? = null
    private var cachedContributedFunctions: Collection<FunctionDescriptor>? = null

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        if (cachedContributedFunctionsName == name) cachedContributedFunctions!!
        else baseResolutionScope.getContributedFunctions(name, location).also {
            cachedContributedFunctionsName = name
            cachedContributedFunctions = it
        }
}

fun ResolutionScope.withLocalCache() = ResolutionScopeWithLastRequestsCached(this)