package com.example.util

import android.util.Log

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE,
    WRITE
}

fun handleFirestoreError(error: Throwable, operation: OperationType, path: String) {
    Log.e("FirestoreError", "Operation $operation failed at path: $path", error)
}
