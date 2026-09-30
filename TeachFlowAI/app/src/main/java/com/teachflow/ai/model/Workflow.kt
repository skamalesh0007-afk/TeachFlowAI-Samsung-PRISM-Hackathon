package com.teachflow.ai.model

data class Workflow(
    val name: String,
    val commandTemplate: String,
    val steps: List<ActionStep>
)