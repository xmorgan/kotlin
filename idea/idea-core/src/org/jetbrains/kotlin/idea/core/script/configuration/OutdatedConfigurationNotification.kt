/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle

class OutdatedConfigurationNotification(
    val title: String = KotlinIdeaCoreBundle.message("notification.text.script.configuration.has.been.changed"),
    val loadActionTitle: String = KotlinIdeaCoreBundle.message("notification.action.text.load.script.configuration"),
    val enableAutoReloadActionTitle: String = KotlinIdeaCoreBundle.message("notification.action.text.enable.auto.reload")
)