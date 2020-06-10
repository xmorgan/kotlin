/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223

import com.intellij.ide.extensionResources.ExtensionsRootType
import com.intellij.ide.scratch.RootType
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionSourceAsContributor
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.templates.standard.ScriptTemplateWithBindings

object ScriptDefinitionForExtensionRoots : ScriptDefinition.FromConfigurations(
    defaultJvmScriptingHostConfiguration,
    ScriptCompilationConfigurationForExtensionRoots,
    ScriptEvaluationConfigurationForExtensionRoots
) {
    override fun isScript(script: SourceCode): Boolean {
        val virtFileSourceCode = script as? VirtualFileScriptSource
        return if (virtFileSourceCode != null) {
            RootType.forFile(virtFileSourceCode.virtualFile)?.let { it is ExtensionsRootType } ?: false
        } else {
            super.isScript(script)
        }
    }
}

// have to use obsolete API to be able to provide a `ScriptDefinition` with `isScript implementation
// TODO: extend new definitions API to be able to implement such isScript
class ScriptDefinitionForExtensionRootsSource : ScriptDefinitionSourceAsContributor {
    override val id: String = "Script definition for extension scripts"

    override val definitions: Sequence<ScriptDefinition>
        get() = sequenceOf(ScriptDefinitionForExtensionRoots)
}

private object ScriptCompilationConfigurationForExtensionRoots : ScriptCompilationConfiguration(
    {
        baseClass(KotlinType(ScriptTemplateWithBindings::class))
        jvm {
            dependenciesFromClassContext(ScriptCompilationConfigurationForExtensionRoots::class, "kotlin-stdlib", "kotlin-script-runtime", wholeClasspath = true)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        // to test that definition is selected
//        providedProperties("myProp" to KotlinType(String::class))
    }
)

private object ScriptEvaluationConfigurationForExtensionRoots : ScriptEvaluationConfiguration(
    {

    }
)
