package com.alexqgon.streammonitor.core.data

import javax.inject.Qualifier

/** Qualifies the `StreamEndpoint` that polls raw numbers. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NumbersEndpoint

/** Qualifies the `StreamEndpoint` that polls raw inputs. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InputsEndpoint
