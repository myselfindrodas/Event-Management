package com.example.eventmanagement.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.databinding.ActivityEventEditorBinding
import com.example.eventmanagement.util.DateUtils
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.animateEntrance
import com.example.eventmanagement.util.applyBottomInsetAsPadding
import com.example.eventmanagement.util.applyTopInsetAsPadding
import com.example.eventmanagement.util.staggerChildren
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.Date

class EventEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventEditorBinding
    private val viewModel: EventViewModel by viewModels { EventViewModel.Factory() }
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
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
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

        viewModel.selectedEvent.observe(this) { result ->
            when (result) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val event = result.data
                    binding.etTitle.setText(event.title)
                    binding.etDescription.setText(event.description)
                    binding.etLocation.setText(event.location)
                    selectedDateTime.time = event.dateTime.toDate()
                    updateDateAndTimeLabels()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        viewModel.actionResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = ""
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        if (isEdit) R.string.event_updated else R.string.event_created,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
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
        binding.etDate.setText(DateUtils.formatDate(selectedDateTime.time))
        binding.etTime.setText(DateUtils.formatTime(selectedDateTime.time))
    }

    private fun save() {
        dismissPickers()
        viewModel.saveEvent(
            eventId = eventId,
            title = binding.etTitle.text.toString(),
            description = binding.etDescription.text.toString(),
            date = Date(selectedDateTime.timeInMillis),
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
