/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.compiler

import org.jetbrains.kotlin.analyzer.LanguageSettingsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.targetPlatform
import org.jetbrains.kotlin.utils.DescriptionAware

class LanguageVersionSettingsProviderImpl : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(moduleInfo: ModuleInfo): LanguageVersionSettings {
        return (moduleInfo as? ModuleSourceInfo)?.module?.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT
    }

    override fun getTargetPlatform(moduleInfo: ModuleInfo): DescriptionAware {
        return (moduleInfo as? ModuleSourceInfo)?.module?.targetPlatform?.version ?: DescriptionAware.NoVersion
    }
}
