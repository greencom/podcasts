package com.greencom.android.podcasts.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.greencom.android.podcasts.R

class HomeFragment : Fragment() {

//    private val viewModel: HomeViewModel by navGraphViewModels(R.id.nav_graph)
//    private val viewModel: HomeViewModel by activityViewModels()
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val button = view.findViewById<TextView>(R.id.button)
        val text = view.findViewById<TextView>(R.id.textView)

        button.setOnClickListener {
            viewModel.add()
        }

        viewModel.number.observe(viewLifecycleOwner) {
            button.text = it.toString()
        }

        text.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_testFragment)
        }
    }
}