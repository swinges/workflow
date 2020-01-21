/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KClass

/**
 * A [ViewRegistry] that contains only other registries and delegates to their [buildView] methods.
 *
 * Whenever any registries are combined using the [ViewRegistry] factory functions or `plus`
 * operators, an instance of this class is returned. All registries' keys are checked at
 * construction to ensure that no duplicate keys exist.
 *
 * The implementation of [buildView] consists of a single layer of indirection – the responsible
 * [ViewRegistry] is looked up in a map by key, and then that registry's [buildView] is called.
 *
 * When multiple [CompositeViewRegistry]s are combined, they are flattened, so that there is never
 * more than one layer of indirection. In other words, a [CompositeViewRegistry] will never contain
 * a reference to another [CompositeViewRegistry].
 */
internal class CompositeViewRegistry private constructor(
  private val registriesByKey: Map<Any, ViewRegistry>
) : ViewRegistry {

  constructor (vararg registries: ViewRegistry) : this(mergeRegistries(*registries))

  override val keys: Set<Any> get() = registriesByKey.keys

  override fun <RenderingT : Any> buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View {
    val registry = getRegistryFor(initialRendering::class)
    return registry.buildView(initialRendering, initialContainerHints, contextForNewView, container)
  }

  override fun <RenderingT : Any> getBindingFor(
    renderingType: KClass<out RenderingT>
  ): ViewBinding<RenderingT>? = getRegistryFor(renderingType).getBindingFor(renderingType)

  private fun getRegistryFor(renderingType: KClass<out Any>): ViewRegistry {
    return registriesByKey[renderingType]
        ?: throw IllegalArgumentException(
            "A ${ViewBinding::class.java.name} should have been registered " +
                "to display a $renderingType."
        )
  }

  companion object {
    private fun mergeRegistries(vararg registries: ViewRegistry): Map<Any, ViewRegistry> {
      val registriesByKey = mutableMapOf<Any, ViewRegistry>()

      fun putAllUnique(other: Map<Any, ViewRegistry>) {
        val duplicateKeys = registriesByKey.keys.intersect(other.keys)
        check(duplicateKeys.isEmpty()) { "Must not have duplicate entries: $duplicateKeys" }
        registriesByKey.putAll(other)
      }

      registries.forEach { registry ->
        if (registry is CompositeViewRegistry) {
          // Try to keep the composite registry as flat as possible.
          putAllUnique(registry.registriesByKey)
        } else {
          putAllUnique(registry.keys.associateWith { registry })
        }
      }
      return registriesByKey.toMap()
    }
  }
}
