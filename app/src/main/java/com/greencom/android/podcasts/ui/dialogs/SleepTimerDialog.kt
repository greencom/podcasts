package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.databinding.DialogSleepTimerBinding
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Dialog used to set or clear a sleep timer. Use [show] to create and display this dialog. */
class SleepTimerDialog : BottomSheetDialogFragment()  {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    private lateinit var listener: SleepTimerDialogListener

    /**
     * Interface for callbacks to be invoked when the user sets or clears a sleep timer.
     */
    interface SleepTimerDialogListener {

        /** Callback to be invoked when the user sets a new sleep timer with a duration in ms. */
        fun onSleepTimerSet(durationInMs: Long)

        /** Callback to be invoked when the user clears the currently set sleep timer. */
        fun onSleepTimerClear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = activity as SleepTimerDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                "$activity must implement SleepTimerDialogListener interface"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initial time picker setup.
        binding.timePicker.apply {
            setIs24HourView(true)
            hour = 0
            minute = 30
        }

        // Clear a sleep timer.
        binding.clear.setOnClickListener {
            listener.onSleepTimerClear()
            dismiss()
        }

        // Set a sleep timer.
        binding.set.setOnClickListener {
            val duration = (Duration.hours(binding.timePicker.hour) +
                    Duration.minutes(binding.timePicker.minute)).inWholeMilliseconds
            listener.onSleepTimerSet(duration)
            dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        /** SleepTimerDialog tag. */
        const val TAG = "SLEEP_TIMER_DIALOG_TAG"

        /** Create and display [SleepTimerDialog]. */
        fun show(fragmentManager: FragmentManager) {
            SleepTimerDialog().show(fragmentManager, TAG)
        }
    }
}