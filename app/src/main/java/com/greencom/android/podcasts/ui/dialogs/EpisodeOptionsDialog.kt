package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.DialogEpisodeOptionsBinding

private const val EPISODE_ID = "EPISODE_ID"
private const val IS_EPISODE_COMPLETED = "IS_EPISODE_COMPLETED"

/** Dialog that appears on episode long click. Use [show] to create and display this dialog. */
class EpisodeOptionsDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEpisodeOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var listener: EpisodeOptionsDialogListener

    /**
     * Interface definition for callbacks to be invoked when the user performs actions
     * in the [EpisodeOptionsDialog].
     */
    interface EpisodeOptionsDialogListener {

        /**
         * Callback to be invoked when the user clicks Mark completed (or uncompleted) button
         * for an episode with a given ID.
         */
        fun onEpisodeOptionsIsCompletedChange(episodeId: String, isCompleted: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = parentFragment as EpisodeOptionsDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                "$parentFragment must implements EpisodeOptionsDialog interface"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEpisodeOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get initial values from the fragment args.
        val episodeId = arguments?.getString(EPISODE_ID) ?: ""
        val isEpisodeCompleted = arguments?.getBoolean(IS_EPISODE_COMPLETED) ?: false

        // Set up a view depending on the whether an episode is completed.
        binding.apply {
            if (isEpisodeCompleted) {
                markCompletedOrUncompletedIcon.setImageResource(R.drawable.ic_clear_24)
                markCompletedOrUncompletedText.text = getString(R.string.episode_options_mark_uncompleted)
            } else {
                markCompletedOrUncompletedIcon.setImageResource(R.drawable.ic_check_24)
                markCompletedOrUncompletedText.text = getString(R.string.episode_options_mark_completed)
            }
        }

        // Mark an episode as completed or uncompleted.
        binding.markCompletedOrUncompleted.setOnClickListener {
            listener.onEpisodeOptionsIsCompletedChange(episodeId, !isEpisodeCompleted)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        /** EpisodeOptionsDialog tag. */
        const val TAG = "EPISODE_OPTIONS_DIALOG_TAG"

        /**
         * Create and display [EpisodeOptionsDialog] with a given podcast ID. Make sure to pass in
         * the appropriate [fragmentManager] so that the host fragment implements the
         * [EpisodeOptionsDialog] interface.
         */
        fun show(
            fragmentManager: FragmentManager,
            episodeId: String,
            isEpisodeCompleted: Boolean
        ) {
            EpisodeOptionsDialog().apply {
                arguments = Bundle().apply {
                    putString(EPISODE_ID, episodeId)
                    putBoolean(IS_EPISODE_COMPLETED, isEpisodeCompleted)
                }
            }.show(fragmentManager, TAG)
        }
    }
}