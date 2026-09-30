package com.teachflow.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.teachflow.ai.databinding.ActivityMainBinding
import com.teachflow.ai.storage.WorkflowStore
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: WorkflowStore

    private val audioPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                toast("Microphone permission is required for voice commands.")
            }
        }

    private val speechLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val text =
                result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()

            if (!text.isNullOrBlank()) {

                binding.commandInput.setText(text)

                log("Voice command: $text")

                WorkflowController.currentCommand = text
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        store = WorkflowStore(this)

        // Open Android Accessibility Settings
        binding.openAccessibility.setOnClickListener {

            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        // Voice input
        binding.voiceButton.setOnClickListener {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                audioPermission.launch(
                    Manifest.permission.RECORD_AUDIO
                )

            } else {

                startVoiceInput()
            }
        }

        // START TEACHING
        binding.teachButton.setOnClickListener {

            val command =
                binding.commandInput.text.toString().trim()

            if (command.isEmpty()) {

                toast("Enter or speak a teaching command first.")

                return@setOnClickListener
            }

            if (!TeachFlowAccessibilityService.isRunning()) {

                toast(
                    "Enable TeachFlow Accessibility Service first."
                )

                return@setOnClickListener
            }

            WorkflowController.startTeaching(
                this,
                command
            )

            log(
                "Teaching started. Perform the actions in the target app."
            )

            toast("Teaching started")
        }

        // STOP TEACHING AND SAVE
        binding.stopTeachButton.setOnClickListener {

            val workflow =
                WorkflowController.stopTeaching(this)

            if (workflow != null) {

                log(
                    "Teaching stopped. Workflow saved successfully."
                )

                log(
                    "Recorded steps: ${workflow.steps.size}"
                )

                toast("Workflow saved successfully")

            } else {

                toast(
                    "No workflow was recorded."
                )
            }
        }

        // REPLAY
        binding.replayButton.setOnClickListener {

            val command =
                binding.commandInput.text.toString().trim()

            if (command.isEmpty()) {

                toast("Enter a command first.")

                return@setOnClickListener
            }

            val flow = store.load()

            if (flow == null) {

                toast(
                    "No learned flow yet. Teach one first."
                )

                return@setOnClickListener
            }

            WorkflowController.startReplay(
                this,
                command,
                flow
            )

            log(
                "Replay requested for: $command"
            )
        }
    }

    override fun onResume() {

        super.onResume()

        val running =
            TeachFlowAccessibilityService.isRunning()

        binding.serviceStatus.text =
            if (running) {

                "Accessibility service: ENABLED"

            } else {

                "Accessibility service: NOT ENABLED"
            }
    }

    private fun startVoiceInput() {

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Say what you want TeachFlow to do"
                )
            }

        speechLauncher.launch(intent)
    }

    private fun log(message: String) {

        binding.logView.append(
            "\n$message"
        )
    }

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}