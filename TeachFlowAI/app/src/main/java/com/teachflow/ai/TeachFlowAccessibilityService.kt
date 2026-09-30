package com.teachflow.ai

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.teachflow.ai.model.ActionStep
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.util.SemanticMatcher
import java.util.concurrent.CopyOnWriteArrayList

class TeachFlowAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: TeachFlowAccessibilityService? = null
        private var recording = false
        private var recordedCommand = ""
        private val steps = CopyOnWriteArrayList<ActionStep>()

        fun isRunning(): Boolean {
            return instance != null
        }

        fun beginRecording(command: String) {
            recordedCommand = command
            steps.clear()
            recording = true
            instance?.toast("Recording UI actions")
        }

        fun endRecording(): Workflow? {
            recording = false

            if (recordedCommand.isBlank()) {
                return null
            }

            return Workflow(
                name = "learned_${System.currentTimeMillis()}",
                commandTemplate = recordedCommand,
                steps = steps.toList()
            )
        }

        fun replay(
            workflow: Workflow,
            command: String
        ) {
            instance?.performReplay(workflow, command)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        toast("TeachFlow accessibility service connected")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null || !recording) {
            return
        }

        when (event.eventType) {

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val node = event.source ?: return
                val action = nodeToAction(node)
                steps.add(action)
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val node = event.source ?: return
                val text = node.text?.toString() ?: return

                if (text.isNotBlank()) {
                    steps.add(
                        ActionStep(
                            type = "INPUT",
                            text = node.text?.toString(),
                            hint = node.hintText?.toString(),
                            className = node.className?.toString(),
                            packageName = node.packageName?.toString(),
                            bounds = bounds(node),
                            value = text
                        )
                    )
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService.
    }

    private fun nodeToAction(
        node: AccessibilityNodeInfo
    ): ActionStep {
        return ActionStep(
            type = "CLICK",
            text = node.text?.toString(),
            hint = node.hintText?.toString(),
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            bounds = bounds(node)
        )
    }

    private fun bounds(
        node: AccessibilityNodeInfo
    ): String {
        val r = Rect()
        node.getBoundsInScreen(r)

        return "${r.left},${r.top},${r.right},${r.bottom}"
    }

    private fun performReplay(
        workflow: Workflow,
        command: String
    ) {
        Thread {

            val parameterMap =
                SemanticMatcher.extractParameters(
                    workflow.commandTemplate,
                    command
                )

            val totalSteps = workflow.steps.size

            if (totalSteps == 0) {
                postToast(
                    "Replay stopped: no recorded steps"
                )
                return@Thread
            }

            postToast(
                "Replaying $totalSteps recorded steps"
            )

            var completedSteps = 0

            for ((index, step) in workflow.steps.withIndex()) {

                val stepNumber = index + 1

                if (isCredentialBoundary(step)) {
                    postToast(
                        "Stopped at step $stepNumber: authentication/payment boundary"
                    )
                    return@Thread
                }

                var root: AccessibilityNodeInfo? = null

                for (attempt in 1..10) {

                    root = rootInActiveWindow

                    if (root != null) {
                        break
                    }

                    Thread.sleep(300)
                }

                if (root == null) {
                    postToast(
                        "Replay stopped at step $stepNumber/$totalSteps: no active screen"
                    )
                    return@Thread
                }

                val target =
                    SemanticMatcher.findBestNode(
                        root,
                        step,
                        parameterMap
                    )

                if (target == null) {
                    postToast(
                        "Replay stopped at step $stepNumber/$totalSteps: target not found"
                    )
                    return@Thread
                }

                val success: Boolean

                if (
                    step.type.equals(
                        "INPUT",
                        ignoreCase = true
                    )
                ) {

                    val value =
                        SemanticMatcher.applyParameters(
                            step.value ?: "",
                            parameterMap
                        )

                    val args =
                        android.os.Bundle().apply {
                            putCharSequence(
                                AccessibilityNodeInfo
                                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                value
                            )
                        }

                    success =
                        target.performAction(
                            AccessibilityNodeInfo
                                .ACTION_SET_TEXT,
                            args
                        )

                } else {

                    success =
                        target.performAction(
                            AccessibilityNodeInfo
                                .ACTION_CLICK
                        )
                }

                if (!success) {
                    postToast(
                        "Replay stopped at step $stepNumber/$totalSteps: action failed"
                    )
                    return@Thread
                }

                completedSteps++

                postToast(
                    "Replay step $stepNumber/$totalSteps completed"
                )

                Thread.sleep(1000)
            }

            if (completedSteps == totalSteps) {
                postToast(
                    "Replay completed: $completedSteps/$totalSteps steps"
                )
            } else {
                postToast(
                    "Replay incomplete: $completedSteps/$totalSteps steps"
                )
            }

        }.start()
    }

    private fun isCredentialBoundary(
        step: ActionStep
    ): Boolean {

        val combined =
            listOf(
                step.text,
                step.hint,
                step.className
            )
                .filterNotNull()
                .joinToString(" ")
                .lowercase()

        val sensitive =
            listOf(
                "password",
                "otp",
                "one time password",
                "verification code",
                "payment",
                "cvv",
                "card number",
                "upi pin",
                "pin"
            )

        return sensitive.any {
            combined.contains(it)
        }
    }

    private fun postToast(
        message: String
    ) {
        Handler(
            Looper.getMainLooper()
        ).post {
            toast(message)
        }
    }

    private fun toast(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}