package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.databinding.UnsubscribeDialogBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog.Companion.newInstance
import com.greencom.android.podcasts.ui.explore.ExplorePageFragment

// Initialization parameters.
private const val PODCAST_ID = "podcast_id"

/** Dialog to confirm unsubscribe action. Use [newInstance] to create an instance. */
class UnsubscribeDialog private constructor(): BottomSheetDialogFragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: UnsubscribeDialogBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = UnsubscribeDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get a podcast ID from the fragment arguments.
        val podcastId = arguments?.getString(PODCAST_ID) ?: ""

        // Unsubscribe confirmation.
        binding.unsubscribe.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                ExplorePageFragment.UNSUBSCRIBE_DIALOG,
                bundleOf(ExplorePageFragment.PODCAST_ID to podcastId)
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    companion object {
        /** UnsubscribeDialog tag. */
        const val TAG = "UnsubscribeDialog"

        /** Create a new instance of the dialog with a given podcast ID. */
        fun newInstance(podcastId: String) = UnsubscribeDialog().apply {
            arguments = Bundle().apply {
                putString(PODCAST_ID, podcastId)
            }
        }
    }
}