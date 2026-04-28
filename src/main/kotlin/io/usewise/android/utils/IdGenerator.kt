package io.usewise.android.utils

import java.util.UUID

object IdGenerator {
    fun uuid(): String = UUID.randomUUID().toString().lowercase()
}
