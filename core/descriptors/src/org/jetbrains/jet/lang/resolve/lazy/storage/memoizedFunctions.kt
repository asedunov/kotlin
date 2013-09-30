package org.jetbrains.jet.utils

import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNull

public fun <P, R: Any> MemoizedFunctionToNullable<P, R>.invoke(p: P): R? = `fun`(p)
public fun <P, R: Any> MemoizedFunctionToNotNull<P, R>.invoke(p: P): R = `fun`(p)