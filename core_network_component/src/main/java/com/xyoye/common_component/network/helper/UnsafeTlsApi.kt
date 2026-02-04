package com.xyoye.common_component.network.helper

/**
 * Marks APIs that relax TLS verification (e.g. trust-all certificates / ignore hostname verification).
 *
 * This is intentionally "opt-in" to avoid accidental usage leaking into release paths.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Unsafe TLS: ONLY use with explicit user opt-in, and prefer safer alternatives (custom CA / pinning / HTTP).",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class UnsafeTlsApi
