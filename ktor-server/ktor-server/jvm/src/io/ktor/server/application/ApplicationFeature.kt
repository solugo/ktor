/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.application

import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

/**
 * Defines an installable Application Feature
 * @param TPipeline is the type of the pipeline this feature is compatible with
 * @param TConfiguration is the type for the configuration object for this Feature
 * @param TFeature is the type for the instance of the Feature object
 */
@Suppress("AddVarianceModifier")
public interface ApplicationFeature<
    in TPipeline : Pipeline<*, ApplicationCall>,
    out TConfiguration : Any,
    TFeature : Any> {
    /**
     * Unique key that identifies a feature
     */
    public val key: AttributeKey<TFeature>

    /**
     * Feature installation script
     */
    public fun install(pipeline: TPipeline, configure: TConfiguration.() -> Unit): TFeature
}

internal val featureRegistryKey = AttributeKey<Attributes>("ApplicationFeatureRegistry")

/**
 * Gets feature instance for this pipeline, or fails with [MissingApplicationFeatureException] if the feature is not installed
 * @throws MissingApplicationFeatureException
 * @param feature application feature to lookup
 * @return an instance of feature
 */
public fun <A : Pipeline<*, ApplicationCall>, B : Any, F : Any> A.feature(feature: ApplicationFeature<A, B, F>): F {
    return attributes[featureRegistryKey].getOrNull(feature.key)
        ?: throw MissingApplicationFeatureException(feature.key)
}

/**
 * Returns feature instance for this pipeline, or null if feature is not installed
 */
public fun <A : Pipeline<*, ApplicationCall>,
    B : Any, F : Any> A.featureOrNull(feature: ApplicationFeature<A, B, F>): F? {
    return attributes.getOrNull(featureRegistryKey)?.getOrNull(feature.key)
}

/**
 * Installs [feature] into this pipeline, if it is not yet installed
 */
public fun <P : Pipeline<*, ApplicationCall>, B : Any, F : Any> P.install(
    feature: ApplicationFeature<P, B, F>,
    configure: B.() -> Unit = {}
): F {
    val registry = attributes.computeIfAbsent(featureRegistryKey) { Attributes(true) }
    val installedFeature = registry.getOrNull(feature.key)
    when (installedFeature) {
        null -> {
            try {
                val installed = feature.install(this, configure)
                registry.put(feature.key, installed)
                // environment.log.trace("`${feature.name}` feature was installed successfully.")
                return installed
            } catch (t: Throwable) {
                // environment.log.error("`${feature.name}` feature failed to install.", t)
                throw t
            }
        }
        feature -> {
            // environment.log.warning("`${feature.name}` feature is already installed")
            return installedFeature
        }
        else -> {
            throw DuplicateApplicationFeatureException(
                "Conflicting application feature is already installed with the same key as `${feature.key.name}`"
            )
        }
    }
}

/**
 * Uninstalls all features from the pipeline
 */
@Deprecated(
    "This method is misleading and will be removed. " +
        "If you have use case that requires this functionaity, please add it in KTOR-2696"
)
public fun <A : Pipeline<*, ApplicationCall>> A.uninstallAllFeatures() {
    val registry = attributes.computeIfAbsent(featureRegistryKey) { Attributes(true) }
    registry.allKeys.forEach {
        @Suppress("UNCHECKED_CAST")
        uninstallFeature(it as AttributeKey<Any>)
    }
}

/**
 * Uninstalls [feature] from the pipeline
 */
@Deprecated(
    "This method is misleading and will be removed. " +
        "If you have use case that requires this functionaity, please add it in KTOR-2696"
)
public fun <A : Pipeline<*, ApplicationCall>, B : Any, F : Any> A.uninstall(
    feature: ApplicationFeature<A, B, F>
): Unit = uninstallFeature(feature.key)

/**
 * Uninstalls feature specified by [key] from the pipeline
 */
@Deprecated(
    "This method is misleading and will be removed. " +
        "If you have use case that requires this functionaity, please add it in KTOR-2696"
)
public fun <A : Pipeline<*, ApplicationCall>, F : Any> A.uninstallFeature(key: AttributeKey<F>) {
    val registry = attributes.getOrNull(featureRegistryKey) ?: return
    val instance = registry.getOrNull(key) ?: return
    if (instance is Closeable) {
        instance.close()
    }
    registry.remove(key)
}

/**
 * Thrown when Application Feature has been attempted to be installed with the same key as already installed Feature
 */
public class DuplicateApplicationFeatureException(message: String) : Exception(message)

/**
 * Thrown when Application Feature has been attempted to be accessed but has not been installed before
 * @param key application feature's attribute key
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class MissingApplicationFeatureException(
    public val key: AttributeKey<*>
) : IllegalStateException(), CopyableThrowable<MissingApplicationFeatureException> {
    override val message: String get() = "Application feature ${key.name} is not installed"

    override fun createCopy(): MissingApplicationFeatureException? = MissingApplicationFeatureException(key).also {
        it.initCause(this)
    }
}
