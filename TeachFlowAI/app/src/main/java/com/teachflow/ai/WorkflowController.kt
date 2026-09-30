package com.teachflow.ai

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.storage.WorkflowStore

object WorkflowController {

    var currentCommand: String = ""

    fun startTeaching(
        context: Context,
        command: String
    ) {
        currentCommand = command

        TeachFlowAccessibilityService.beginRecording(
            command
        )
    }

    fun stopTeaching(
        context: Context
    ): Workflow? {

        val workflow =
            TeachFlowAccessibilityService.endRecording()

        if (workflow != null) {
            WorkflowStore(context).save(workflow)
        }

        return workflow
    }

    fun startReplay(
        context: Context,
        command: String,
        workflow: Workflow
    ) {
        currentCommand = command

        if (containsCameraCommand(command)) {

            try {
                val cameraIntent =
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                cameraIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(cameraIntent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Thread {

            if (containsCameraCommand(command)) {
                Thread.sleep(1500)
            }

            TeachFlowAccessibilityService.replay(
                workflow,
                command
            )

        }.start()
    }

    private fun containsCameraCommand(
        command: String
    ): Boolean {

        val normalized =
            command
                .lowercase()
                .trim()

        return normalized.contains("camera")
    }
}