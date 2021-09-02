/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*
import io.ktor.util.network.NetworkAddress
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*

internal class TCPServerSocketNative(
    private val descriptor: Int,
    private val selectorManager: SelectorManager,
    override val localAddress: NetworkAddress,
    parent: CoroutineContext = EmptyCoroutineContext
) : ServerSocket, CoroutineScope {
    private val _socketContext: CompletableJob = SupervisorJob(parent[Job])
    private val selectable = SelectableNative(descriptor)

    override val coroutineContext: CoroutineContext = parent + Dispatchers.Unconfined + _socketContext

    override val socketContext: Job
        get() = _socketContext

    @Suppress("DUPLICATE_LABEL_IN_WHEN")
    override suspend fun accept(): Socket = memScoped {
        val clientAddress = alloc<sockaddr_storage>()
        val clientAddressLength: UIntVarOf<UInt> = alloc()
        clientAddressLength.value = sizeOf<sockaddr_storage>().convert()

        var clientDescriptor: Int
        while (true) {
            clientDescriptor = accept(descriptor, clientAddress.ptr.reinterpret(), clientAddressLength.ptr)
            if (clientDescriptor > 0) {
                break
            }

            when (errno) {
                EAGAIN, EWOULDBLOCK -> selectorManager.select(selectable, SelectInterest.ACCEPT)
                EBADF -> error("Descriptor invalid")
                ECONNABORTED -> error("Connection aborted")
                EFAULT -> error("Address is not writable part of user address space")
                EINTR -> error("Interrupted by signal")
                EINVAL -> error("Socket is unwilling to accept")
                EMFILE -> error("Process descriptor file table is full")
                ENFILE -> error("System descriptor file table is full")
                ENOMEM -> error("OOM")
                ENOTSOCK -> error("Descriptor is not a socket")
                EOPNOTSUPP -> error("Not TCP socket")
                else -> error("Unknown error: $errno")
            }
        }

        fcntl(clientDescriptor, F_SETFL, O_NONBLOCK)
            .check()

        val remoteAddress = clientAddress.reinterpret<sockaddr>().toSocketAddress()
        val localAddress = getLocalAddress(descriptor)

        TCPSocketNative(
            clientDescriptor,
            selectorManager,
            remoteAddress = io.ktor.util.network.NetworkAddress("", remoteAddress.port, remoteAddress),
            localAddress = io.ktor.util.network.NetworkAddress("", localAddress.port, localAddress),
            parent = selfContext() + coroutineContext
        )
    }

    override fun close() {
        _socketContext.complete()
        _socketContext.invokeOnCompletion {
            shutdown(descriptor, SHUT_RDWR)
            close(descriptor)
        }
    }
}

private suspend inline fun selfContext(): CoroutineContext = coroutineContext
