package com.example.eventmanagement.presentation.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.eventmanagement.R
import com.example.eventmanagement.core.theme.ThemePreferences
import com.example.eventmanagement.core.time.DateFormatter
import com.example.eventmanagement.core.ui.animateEntrance
import com.example.eventmanagement.core.ui.applyBottomInsetAsPadding
import com.example.eventmanagement.core.ui.applyTopInsetAsPadding
import com.example.eventmanagement.core.ui.staggerChildren
import com.example.eventmanagement.core.ui.toUserMessage
import com.example.eventmanagement.databinding.ActivityEventEditorBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class EventEditorActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivityEventEditorBinding
    private val viewModel: EventEditorViewModel by viewModels()
    private var selectedDateTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var eventId: String? = null
    private val isEdit get() = !eventId.isNullOrBlank()

    private var datePickerDialog: DatePickerDialog? = null
    private var timePickerDialog: TimePickerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityEventEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.applyTopInsetAsPadding()
        binding.contentColumn.applyBottomInsetAsPadding()
        playEntrance()

        eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        binding.tvTitle.setText(if (isEdit) R.string.edit_event else R.string.add_event)
        binding.toolbar.setNavigationOnClickListener { finish() }

        updateDateAndTimeLabels()

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etTime.setOnClickListener { showTimePicker() }
        binding.btnSave.setOnClickListener { save() }

        if (isEdit) {
            viewModel.loadEvent(eventId!!)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { renderEditor(it) } }
                launch { viewModel.effects.collect { handleEffect(it) } }
            }
        }
    }

    private fun renderEditor(state: EventEditorUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !state.isLoading
        binding.btnSave.text = if (state.isLoading) "" else getString(R.string.save)

        val event = state.event ?: return
        if (binding.etTitle.text.isNullOrEmpty()) {
            binding.etTitle.setText(event.title)
            binding.etDescription.setText(event.description)
            binding.etLocation.setText(event.location)
            selectedDateTime.timeInMillis = event.dateTime.toEpochMilli()
            updateDateAndTimeLabels()
        }
    }

    private fun handleEffect(effect: EventEditorUiEffect) {
        when (effect) {
            EventEditorUiEffect.Created -> {
                Toast.makeText(this, R.string.event_created, Toast.LENGTH_SHORT).show()
                finish()
            }
            EventEditorUiEffect.Updated -> {
                Toast.makeText(this, R.string.event_updated, Toast.LENGTH_SHORT).show()
                finish()
            }
            is EventEditorUiEffect.Error -> {
                Snackbar.make(binding.root, effect.error.toUserMessage(this), Snackbar.LENGTH_LONG).show()
                if (isEdit && viewModel.uiState.value.event == null && !viewModel.uiState.value.isLoading) {
                    finish()
                }
            }
        }
    }

    private fun playEntrance() {
        binding.toolbar.animateEntrance(40L)
        binding.tvTitle.animateEntrance(140L)
        binding.tvSubtitle.animateEntrance(210L)
        binding.formCard.animateEntrance(280L)
        binding.formColumn.staggerChildren(stepMs = 60L, startDelayMs = 360L)
    }

    private fun showDatePicker() {
        if (datePickerDialog?.isShowing == true || timePickerDialog?.isShowing == true) return
        dismissPickers()

        val year = selectedDateTime.get(Calendar.YEAR)
        val month = selectedDateTime.get(Calendar.MONTH)
        val day = selectedDateTime.get(Calendar.DAY_OF_MONTH)

        datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
            selectedDateTime.set(Calendar.YEAR, y)
            selectedDateTime.set(Calendar.MONTH, m)
            selectedDateTime.set(Calendar.DAY_OF_MONTH, d)
            updateDateAndTimeLabels()
        }, year, month, day).also { dialog ->
            dialog.setOnDismissListener { datePickerDialog = null }
            dialog.show()
        }
    }

    private fun showTimePicker() {
        if (isFinishing || isDestroyed) return

        timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)
                updateDateAndTimeLabels()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        ).also { dialog ->
            dialog.setOnDismissListener { timePickerDialog = null }
            dialog.show()
        }
    }

    private fun dismissPickers() {
        datePickerDialog?.takeIf { it.isShowing }?.dismiss()
        timePickerDialog?.takeIf { it.isShowing }?.dismiss()
        datePickerDialog = null
        timePickerDialog = null
    }

    private fun updateDateAndTimeLabels() {
        val instant = Instant.ofEpochMilli(selectedDateTime.timeInMillis)
        binding.etDate.setText(DateFormatter.formatDate(instant))
        binding.etTime.setText(DateFormatter.formatTime(instant))
    }

    private fun save() {
        dismissPickers()
        viewModel.saveEvent(
            eventId = eventId,
            title = binding.etTitle.text.toString(),
            description = binding.etDescription.text.toString(),
            dateTime = Instant.ofEpochMilli(selectedDateTime.timeInMillis),
            location = binding.etLocation.text.toString(),
            isEdit = isEdit
        )
    }

    override fun onDestroy() {
        dismissPickers()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
