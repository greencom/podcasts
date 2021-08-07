package com.greencom.android.podcasts.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.greencom.android.podcasts.databinding.DialogPlayerOptionsBinding

// Initialization parameters.
private const val EPISODE_ID = "EPISODE_ID"

/** Dialog that appears on player options click. Use [show] to create and display this dialog. */
class PlayerOptionsDialog : BottomSheetDialogFragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: DialogPlayerOptionsBinding? = null
    private val binding get() = _binding!!

    /** Use this instance of the [PlayerOptionsDialogListener] interface to deliver action events. */
    private lateinit var listener: PlayerOptionsDialogListener

    /**
     * Interface definition for callbacks to be invoked when the user performs actions
     * in the [PlayerOptionsDialog].
     */
    interface PlayerOptionsDialogListener {

        /** Callback to be invoked when the user marks an episode as completed. */
        fun onPlayerOptionsMarkCompleted(episodeId: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Verify that the host fragment implements the callback interface.
        try {
            // Instantiate the PlayerOptionsDialog.
            listener = activity as PlayerOptionsDialogListener
        } catch (e: ClassCastException) {
            // Throw an exception if the host does not implement the callback interface.
            throw ClassCastException(
                "$activity must implement PlayerOptionsDialogListener interface"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = DialogPlayerOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get an episode ID from the fragment arguments.
        val episodeId = arguments?.getString(EPISODE_ID) ?: ""

        // Mark an episode as completed.
        binding.markCompleted.setOnClickListener {
            listener.onPlayerOptionsMarkCompleted(episodeId)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    companion object {

        /** PlayerOptionsDialog tag. */
        const val TAG = "PLAYER_OPTIONS_DIALOG_TAG"

        /** Create and display [PlayerOptionsDialog] with a given episode ID. */
        fun show(fragmentManager: FragmentManager, episodeId: String) {
            PlayerOptionsDialog().apply {
                arguments = Bundle().apply {
                    putString(EPISODE_ID, episodeId)
                }
            }.show(fragmentManager, TAG)
        }
    }
}