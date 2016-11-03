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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.analysis.CodeFragmentAnalyzer
import org.jetbrains.kotlin.idea.analysis.ElementsResolver
import org.jetbrains.kotlin.idea.analysis.PartialBodyResolver
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.PartialBodyResolveFilter
import org.jetbrains.kotlin.resolve.lazy.ProbablyNothingCallableNames
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class ResolveElementCache(
        private val resolveSession: ResolveSession,
        private val project: Project,
        targetPlatform: TargetPlatform,
        codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache, ElementsResolver {
    private val partialBodyResolver = PartialBodyResolver(resolveSession, codeFragmentAnalyzer, targetPlatform, this::probablyNothingCallableNames)

    private class CachedFullResolve(val bindingContext: BindingContext, resolveElement: KtElement) {
        private val modificationStamp: Long? = modificationStamp(resolveElement)

        fun isUpToDate(resolveElement: KtElement) = modificationStamp == modificationStamp(resolveElement)

        private fun modificationStamp(resolveElement: KtElement): Long? {
            val file = resolveElement.containingFile
            return when {
                // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset
                // data on any modification of the file
                !file.isPhysical -> file.modificationStamp

                resolveElement is KtDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement) -> resolveElement.getModificationStamp()
                resolveElement is KtSuperTypeList -> resolveElement.modificationStamp
                else -> null
            }
        }
    }

    // drop whole cache after change "out of code block"
    private val fullResolveCache: CachedValue<MutableMap<KtElement, CachedFullResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            CachedValueProvider<MutableMap<KtElement, ResolveElementCache.CachedFullResolve>> {
                CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<KtElement, CachedFullResolve>(),
                                                  PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                                  resolveSession.exceptionTracker)
            },
            false)

    private class CachedPartialResolve(val bindingContext: BindingContext, file: KtFile, val mode: BodyResolveMode) {
        private val modificationStamp: Long? = modificationStamp(file)

        fun isUpToDate(file: KtFile, newMode: BodyResolveMode) =
                modificationStamp == modificationStamp(file) && mode.doesNotLessThan(newMode)

        private fun modificationStamp(file: KtFile): Long? {
            return if (!file.isPhysical) // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else
                null
        }
    }

    private val partialBodyResolveCache: CachedValue<MutableMap<KtExpression, CachedPartialResolve>> =
            CachedValuesManager.getManager(project).createCachedValue(
                    CachedValueProvider<MutableMap<KtExpression, ResolveElementCache.CachedPartialResolve>> {
                        CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<KtExpression, CachedPartialResolve>(),
                                                          PsiModificationTracker.MODIFICATION_COUNT,
                                                          resolveSession.exceptionTracker)
                    },
                    false)


    private fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = KotlinProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = KotlinProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: KtNamedFunction)
            = getElementsAdditionalResolve(function, null, BodyResolveMode.FULL)

    fun resolvePrimaryConstructorParametersDefaultValues(ktClass: KtClass): BindingContext {
        return partialBodyResolver
                .constructorAdditionalResolve(resolveSession, ktClass, ktClass.getContainingKtFile(), BindingTraceFilter.NO_DIAGNOSTICS)
                .bindingContext
    }

    @Deprecated("Use getElementsAdditionalResolve")
    fun getElementAdditionalResolve(resolveElement: KtElement, contextElement: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        return getElementsAdditionalResolve(resolveElement, listOf(contextElement), bodyResolveMode)
    }

    fun getElementsAdditionalResolve(resolveElement: KtElement, contextElements: Collection<KtElement>?, bodyResolveMode: BodyResolveMode): BindingContext {
        if (contextElements == null) {
            assert(bodyResolveMode == BodyResolveMode.FULL)
        }

        // check if full additional resolve already performed and is up-to-date
        val fullResolveMap = fullResolveCache.value
        val cachedFullResolve = fullResolveMap[resolveElement]
        if (cachedFullResolve != null) {
            if (cachedFullResolve.isUpToDate(resolveElement)) {
                return cachedFullResolve.bindingContext
            }
            else {
                fullResolveMap.remove(resolveElement) // remove outdated cache entry
            }
        }

        when (bodyResolveMode) {
            BodyResolveMode.FULL -> {
                val bindingContext = partialBodyResolver.performElementAdditionalResolve(resolveElement, null, BodyResolveMode.FULL).first
                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            else -> {
                if (resolveElement !is KtDeclaration) {
                    return getElementsAdditionalResolve(resolveElement, null, BodyResolveMode.FULL)
                }

                val file = resolveElement.getContainingKtFile()
                val statementsToResolve = contextElements!!.map { PartialBodyResolveFilter.findStatementToResolve(it, resolveElement) }.distinct()
                val partialResolveMap = partialBodyResolveCache.value
                val cachedResults = statementsToResolve.map { partialResolveMap[it ?: resolveElement] }
                if (cachedResults.all { it != null && it.isUpToDate(file, bodyResolveMode) }) { // partial resolve is already cached for these statements
                    return CompositeBindingContext.create(cachedResults.map { it!!.bindingContext }.distinct())
                }

                val (bindingContext, statementFilter) = partialBodyResolver.performElementAdditionalResolve(resolveElement, contextElements, bodyResolveMode)

                if (statementFilter == StatementFilter.NONE) { // partial resolve is not supported for the given declaration - full resolve performed instead
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file, bodyResolveMode)

                for (statement in (statementFilter as PartialBodyResolveFilter).allStatementsToResolve) {
                    if (!partialResolveMap.containsKey(statement) && bindingContext[BindingContext.PROCESSED, statement] == true) {
                        partialResolveMap[statement] = resolveToCache
                    }
                }

                // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)
                partialResolveMap[resolveElement] = resolveToCache

                return bindingContext
            }
        }
    }

    override fun resolveToElements(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        val elementsByAdditionalResolveElement: Map<KtElement?, List<KtElement>> = elements.groupBy {
            partialBodyResolver.findElementOfAdditionalResolve(it)
        }

        val bindingContexts = ArrayList<BindingContext>()
        val declarationsToResolve = ArrayList<KtDeclaration>()
        var addResolveSessionBindingContext = false
        for ((elementOfAdditionalResolve, contextElements) in elementsByAdditionalResolveElement) {
            if (elementOfAdditionalResolve != null) {
                if (elementOfAdditionalResolve !is KtParameter) {
                    val bindingContext = getElementsAdditionalResolve(elementOfAdditionalResolve, contextElements, bodyResolveMode)
                    bindingContexts.add(bindingContext)
                }
                else {
                    // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
                    declarationsToResolve.addIfNotNull(elementOfAdditionalResolve.getNonStrictParentOfType<KtDeclaration>())
                    addResolveSessionBindingContext = true
                }
            }
            else {
                contextElements
                        .map { it.getNonStrictParentOfType<KtDeclaration>() }
                        .filterNotNull()
                        .filterTo(declarationsToResolve) {
                            it !is KtAnonymousInitializer && it !is KtDestructuringDeclaration && it !is KtDestructuringDeclarationEntry
                        }
                addResolveSessionBindingContext = true
            }
        }

        declarationsToResolve.forEach { resolveSession.resolveToDescriptor(it) }
        if (addResolveSessionBindingContext) {
            bindingContexts.add(resolveSession.bindingContext)
        }

        //TODO: it can be slow if too many contexts
        return CompositeBindingContext.create(bindingContexts)
    }
}

