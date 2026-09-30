package com.teachflow.ai.model

data class ActionStep(
    val type: String,
    val text: String?,
    val hint: String?,
    val className: String?,
    val packageName: String?,
    val bounds: String,
    val value: String? = null
)