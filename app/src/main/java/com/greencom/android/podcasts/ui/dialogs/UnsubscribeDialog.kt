package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.databinding.DialogUnsubscribeBinding

// Initialization parameters.
private const val PODCAST_ID = "podcast_id"

/** Dialog that appears on Unsubscribe click. Use [show] to create and display this dialog. */
class UnsubscribeDialog private constructor(): BottomSheetDialogFragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: DialogUnsubscribeBinding? = null
    private val binding get() = _binding!!

    /** Use this instance of the [UnsubscribeDialogListener] interface to deliver action events. */
    private lateinit var listener: UnsubscribeDialogListener

    /**
     * Interface definition for callbacks to be invoked when the user performs actions
     * in the [UnsubscribeDialog].
     */
    interface UnsubscribeDialogListener {

        /**
         * Callback to be invoked when the user clicks Unsubscribe button for a podcast
         * with a given ID.
         */
        fun onUnsubscribeClick(podcastId: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Verify that the host fragment implements the callback interface.
        try {
            // Instantiate the UnsubscribeDialogListener.
            listener = parentFragment as UnsubscribeDialogListener
        } catch (e: ClassCastException) {
            // Throw an exception if the host fragment does not implement the callback interface.
            throw ClassCastException(
                "$parentFragment must implement UnsubscribeDialogListener interface"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = DialogUnsubscribeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get a podcast ID from the fragment arguments.
        val id = arguments?.getString(PODCAST_ID) ?: ""

        // Unsubscribe confirmation.
        binding.unsubscribe.setOnClickListener {
            listener.onUnsubscribeClick(id)
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
        const val TAG = "unsubscribe_dialog"

        /**
         * Create and display [UnsubscribeDialog] with a given podcast ID. Make sure to pass in
         * the appropriate [fragmentManager] so that the host fragment implements the
         * [UnsubscribeDialogListener] interface.
         */
        fun show(fragmentManager: FragmentManager, id: String) {
            UnsubscribeDialog().apply {
                arguments = Bundle().apply {
                    putString(PODCAST_ID, id)
                }
            }.show(fragmentManager, TAG)
        }
    }
}