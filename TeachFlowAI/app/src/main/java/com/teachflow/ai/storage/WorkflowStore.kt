package com.teachflow.ai.storage

import android.content.Context
import com.teachflow.ai.model.ActionStep
import com.teachflow.ai.model.Workflow
import org.json.JSONArray
import org.json.JSONObject

class WorkflowStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("teachflow", Context.MODE_PRIVATE)

    fun save(workflow: Workflow) {
        val root = JSONObject()
        root.put("name", workflow.name)
        root.put("commandTemplate", workflow.commandTemplate)

        val array = JSONArray()
        workflow.steps.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("type", s.type)
                    put("text", s.text ?: "")
                    put("hint", s.hint ?: "")
                    put("className", s.className ?: "")
                    put("packageName", s.packageName ?: "")
                    put("bounds", s.bounds)
                    put("value", s.value ?: "")
                }
            )
        }
        root.put("steps", array)
        prefs.edit().putString("workflow", root.toString()).apply()
    }

    fun load(): Workflow? {
        val raw = prefs.getString("workflow", null) ?: return null
        val root = JSONObject(raw)
        val array = root.getJSONArray("steps")
        val steps = buildList {
            for (i in 0 until array.length()) {
                val s = array.getJSONObject(i)
                add(
                    ActionStep(
                        type = s.getString("type"),
                        text = s.optString("text").ifBlank { null },
                        hint = s.optString("hint").ifBlank { null },
                        className = s.optString("className").ifBlank { null },
                        packageName = s.optString("packageName").ifBlank { null },
                        bounds = s.getString("bounds"),
                        value = s.optString("value").ifBlank { null }
                    )
                )
            }
        }
        return Workflow(
            name = root.getString("name"),
            commandTemplate = root.getString("commandTemplate"),
            steps = steps
        )
    }
}