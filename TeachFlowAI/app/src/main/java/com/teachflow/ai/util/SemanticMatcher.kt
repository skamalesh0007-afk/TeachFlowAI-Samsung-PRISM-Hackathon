package com.teachflow.ai.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.teachflow.ai.model.ActionStep
import kotlin.math.abs

object SemanticMatcher {

    fun extractParameters(
        template: String,
        command: String
    ): Map<String, String> {

        val t = template.lowercase().trim()
        val c = command.lowercase().trim()

        val result = mutableMapOf<String, String>()

        val verbs = listOf(
            "search for",
            "search",
            "find",
            "get me",
            "order"
        )

        val marker = verbs.firstOrNull { t.contains(it) }

        if (marker != null) {

            val tRest = t.substringAfter(marker).trim()

            val cMarker = verbs.firstOrNull {
                c.contains(it)
            }

            val cRest =
                if (cMarker != null)
                    c.substringAfter(cMarker).trim()
                else
                    c

            val tOn =
                tRest.substringBefore(" on ").trim()

            val cOn =
                cRest.substringBefore(" on ").trim()

            if (
                tOn.isNotBlank() &&
                cOn.isNotBlank()
            ) {

                result["item"] =
                    cOn
                        .removePrefix("a ")
                        .removePrefix("an ")
                        .trim()
            }
        }

        return result
    }

    fun applyParameters(
        value: String,
        parameters: Map<String, String>
    ): String {

        var result = value

        parameters.forEach { (key, valueToApply) ->

            result =
                result.replace(
                    "{$key}",
                    valueToApply,
                    ignoreCase = true
                )
        }

        return result
    }

    fun findBestNode(
        root: AccessibilityNodeInfo,
        step: ActionStep,
        parameters: Map<String, String>
    ): AccessibilityNodeInfo? {

        val desiredText =
            applyParameters(
                step.text ?: "",
                parameters
            ).trim()

        val desiredHint =
            applyParameters(
                step.hint ?: "",
                parameters
            ).trim()

        val desiredClass =
            step.className?.trim()

        val desiredPackage =
            step.packageName?.trim()

        val recordedBounds =
            parseBounds(step.bounds)

        val candidates =
            mutableListOf<AccessibilityNodeInfo>()

        collect(root, candidates)

        val scored =
            candidates
                .filter {
                    it.isVisibleToUser
                }
                .map { node ->

                    val score =
                        score(
                            node = node,
                            desiredText = desiredText,
                            desiredHint = desiredHint,
                            desiredClass = desiredClass,
                            desiredPackage = desiredPackage,
                            recordedBounds = recordedBounds,
                            actionType = step.type
                        )

                    Pair(node, score)
                }
                .sortedByDescending {
                    it.second
                }

        val best =
            scored.firstOrNull()

        if (best == null) {
            return null
        }

        return if (best.second >= minimumScore(step)) {
            best.first
        } else {
            null
        }
    }

    private fun score(
        node: AccessibilityNodeInfo,
        desiredText: String,
        desiredHint: String,
        desiredClass: String?,
        desiredPackage: String?,
        recordedBounds: Rect?,
        actionType: String
    ): Int {

        var score = 0

        val text =
            node.text
                ?.toString()
                ?.trim()
                ?: ""

        val hint =
            node.hintText
                ?.toString()
                ?.trim()
                ?: ""

        val className =
            node.className
                ?.toString()
                ?.trim()
                ?: ""

        val packageName =
            node.packageName
                ?.toString()
                ?.trim()
                ?: ""

        // -----------------------------
        // TEXT MATCH
        // -----------------------------

        if (desiredText.isNotBlank()) {

            if (text.equals(desiredText, true)) {
                score += 120
            } else if (
                text.contains(
                    desiredText,
                    true
                )
            ) {
                score += 80
            } else if (
                desiredText.contains(
                    text,
                    true
                ) &&
                text.isNotBlank()
            ) {
                score += 45
            }
        }

        // -----------------------------
        // HINT MATCH
        // -----------------------------

        if (desiredHint.isNotBlank()) {

            if (hint.equals(desiredHint, true)) {
                score += 110
            } else if (
                hint.contains(
                    desiredHint,
                    true
                )
            ) {
                score += 70
            }
        }

        // -----------------------------
        // CLASS MATCH
        // -----------------------------

        if (
            !desiredClass.isNullOrBlank() &&
            className == desiredClass
        ) {
            score += 35
        }

        // -----------------------------
        // PACKAGE MATCH
        // -----------------------------

        if (
            !desiredPackage.isNullOrBlank() &&
            packageName == desiredPackage
        ) {
            score += 30
        }

        // -----------------------------
        // ACTION TYPE
        // -----------------------------

        if (
            actionType.equals(
                "CLICK",
                ignoreCase = true
            )
        ) {

            if (node.isClickable) {
                score += 20
            }

        } else if (
            actionType.equals(
                "INPUT",
                ignoreCase = true
            )
        ) {

            if (node.isEditable) {
                score += 30
            }
        }

        // -----------------------------
        // BOUNDS FALLBACK
        // -----------------------------

        if (recordedBounds != null) {

            val currentBounds =
                Rect()

            node.getBoundsInScreen(
                currentBounds
            )

            val distance =
                centerDistance(
                    recordedBounds,
                    currentBounds
                )

            when {

                distance < 50 -> {
                    score += 60
                }

                distance < 150 -> {
                    score += 35
                }

                distance < 300 -> {
                    score += 15
                }
            }
        }

        return score
    }

    private fun minimumScore(
        step: ActionStep
    ): Int {

        return when {

            step.type.equals(
                "INPUT",
                ignoreCase = true
            ) -> 25

            else -> 20
        }
    }

    private fun parseBounds(
        bounds: String
    ): Rect? {

        return try {

            val parts =
                bounds.split(",")

            if (parts.size != 4) {
                return null
            }

            Rect(
                parts[0].trim().toInt(),
                parts[1].trim().toInt(),
                parts[2].trim().toInt(),
                parts[3].trim().toInt()
            )

        } catch (
            e: Exception
        ) {

            null
        }
    }

    private fun centerDistance(
        first: Rect,
        second: Rect
    ): Double {

        val firstCenterX =
            first.centerX()

        val firstCenterY =
            first.centerY()

        val secondCenterX =
            second.centerX()

        val secondCenterY =
            second.centerY()

        val dx =
            abs(
                firstCenterX -
                        secondCenterX
            )

        val dy =
            abs(
                firstCenterY -
                        secondCenterY
            )

        return kotlin.math.sqrt(
            (dx * dx + dy * dy).toDouble()
        )
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>
    ) {

        out.add(node)

        for (
        i in 0 until node.childCount
        ) {

            val child =
                node.getChild(i)

            if (child != null) {
                collect(
                    child,
                    out
                )
            }
        }
    }
}