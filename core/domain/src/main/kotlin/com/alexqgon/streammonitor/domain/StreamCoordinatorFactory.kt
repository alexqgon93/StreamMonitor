package com.alexqgon.streammonitor.domain

fun interface StreamCoordinatorFactory {
    fun create(): StreamCoordinator
}
