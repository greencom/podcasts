package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.databinding.UnsubscribeDialogBinding
import com.greencom.android.podcasts.ui.explore.ExplorePageFragment

private const val PODCAST_ID = "podcast_id"

// TODO
class UnsubscribeDialog : BottomSheetDialogFragment() {

    private var _binding: UnsubscribeDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UnsubscribeDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val podcastId = arguments?.getString(PODCAST_ID) ?: ""

        binding.unsubscribe.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                ExplorePageFragment.ON_UNSUBSCRIBE_CLICK,
                bundleOf(ExplorePageFragment.PODCAST_ID to podcastId)
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UnsubscribeDialog"

        fun newInstance(podcastId: String) = UnsubscribeDialog().apply {
            arguments = Bundle().apply {
                putString(PODCAST_ID, podcastId)
            }
        }
    }
}