package com.greencom.android.podcasts.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentSettingsBinding
import com.greencom.android.podcasts.ui.home.SUBSCRIPTION_MODE_GRID_COVER_ONLY
import com.greencom.android.podcasts.ui.home.SUBSCRIPTION_MODE_GRID_WITH_TITLE
import com.greencom.android.podcasts.utils.extensions.setupMaterialSharedAxisTransitions
import com.greencom.android.podcasts.utils.setAppBarLayoutCanDrag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Contains app settings. */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Fragment views setup. */
    private fun initViews() {
        setAppBarLayoutCanDrag(binding.appBarLayout, false)

        binding.apply {
            // Navigate up.
            appBarBack.setOnClickListener {
                findNavController().navigateUp()
            }

            // Change app theme.
            theme.setOnClickListener {
                val themeItems = arrayOf(
                    getString(R.string.settings_theme_light),
                    getString(R.string.settings_theme_dark),
                    getString(R.string.settings_theme_system)
                )
                val themeMode = AppCompatDelegate.getDefaultNightMode()
                var newThemeMode = themeMode
                val selectedItem = when (themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> 0
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 2
                    else -> 2
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_theme)
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.setTheme(newThemeMode)
                        AppCompatDelegate.setDefaultNightMode(newThemeMode)
                    }
                    .setSingleChoiceItems(themeItems, selectedItem) { _, selected ->
                        newThemeMode = when (selected) {
                            0 -> AppCompatDelegate.MODE_NIGHT_NO
                            1 -> AppCompatDelegate.MODE_NIGHT_YES
                            2 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            else -> throw IllegalStateException("Out of bounds")
                        }
                    }
                    .show()
            }

            // Change subscription presentation mode.
            showSubscriptionTitles.setOnClickListener {
                showSubscriptionTitlesCheckbox.isChecked = !showSubscriptionTitlesCheckbox.isChecked
            }
            showSubscriptionTitlesCheckbox.setOnCheckedChangeListener { _, isChecked ->
                val mode = if (isChecked) SUBSCRIPTION_MODE_GRID_WITH_TITLE else SUBSCRIPTION_MODE_GRID_COVER_ONLY
                viewModel.setSubscriptionMode(mode)
            }
        }
    }

    /** Set observers for ViewModel observables. */
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe theme mode.
                launch {
                    viewModel.getTheme().collectLatest { mode ->
                        binding.apply {
                            when (mode ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                                AppCompatDelegate.MODE_NIGHT_NO -> {
                                    themeIcon.setImageResource(R.drawable.ic_light_mode_outline_24)
                                    themeCurrent.text = getString(R.string.settings_theme_light)
                                }
                                AppCompatDelegate.MODE_NIGHT_YES -> {
                                    themeIcon.setImageResource(R.drawable.ic_night_outline_24)
                                    themeCurrent.text = getString(R.string.settings_theme_dark)
                                }
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                                    val lightPrimaryColor =
                                        resources.getColor(R.color.light_primary, null)
                                    val iconResId = if (
                                        binding.themeIcon.imageTintList?.defaultColor == lightPrimaryColor
                                    ) {
                                        R.drawable.ic_light_mode_outline_24
                                    } else {
                                        R.drawable.ic_night_outline_24
                                    }
                                    themeIcon.setImageResource(iconResId)
                                    themeCurrent.text = getString(R.string.settings_theme_system)
                                }
                            }
                        }
                    }
                }

                // Observe subscription presentation mode.
                launch {
                    viewModel.getSubscriptionMode().collectLatest { mode ->
                        val mMode = mode ?: SUBSCRIPTION_MODE_GRID_WITH_TITLE
                        binding.showSubscriptionTitlesCheckbox.isChecked =
                            mMode == SUBSCRIPTION_MODE_GRID_WITH_TITLE
                    }
                }
            }
        }
    }
}