/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractKotlinTypeAliasByExpansionShortNameIndexTest : KotlinCodeInsightTestCase() {

    override fun getTestProjectJdk(): Sdk? {
        return PluginTestCaseBase.mockJdk()
    }

    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/"
    }

    lateinit var scope: GlobalSearchScope

    override fun setUp() {
        super.setUp()
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, testProjectJdk)
        scope = GlobalSearchScope.allScope(project)
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, testProjectJdk)
        super.tearDown()
    }


    fun doTest(file: String) {
        configureByFile(file)
        val fileText = myFile.text
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "CONTAINS").forEach {
            assertIndexContains(it)
        }
    }

    private val regex = "\\(key=\"(.*?)\"[, ]*value=\"(.*?)\"\\)".toRegex()

    fun assertIndexContains(record: String) {
        val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
        val (_, key, value) = regex.find(record)!!.groupValues
        val result = index.get(key, project, scope)
        try {
            assertContainsElements(result.map { it.name }, value)
        }
        catch (ae: AssertionError) {
            System.err.println("Record $record not found in index")
            System.err.println("Index contents:")
            index.getAllKeys(project).flatMap {
                System.err.println(it)
                index.get(it, project, scope)
            }.forEach {
                System.err.println("    ${it.name}")
            }
            throw ae
        }

    }

}