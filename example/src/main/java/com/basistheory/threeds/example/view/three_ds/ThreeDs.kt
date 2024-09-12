package com.basistheory.threeds.example.view.three_ds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.basistheory.threeds.example.databinding.FragmentThreeDsBinding
import com.basistheory.threeds.example.viewmodel.ThreeDsViewModel

class ThreeDs : Fragment() {
    private val binding: FragmentThreeDsBinding by lazy {
        FragmentThreeDsBinding.inflate(layoutInflater)
    }

    private val viewModel: ThreeDsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.lifecycleOwner = this

        binding.viewModel = viewModel

        binding.postButton.setOnClickListener { createSessionHandler() }
        binding.startChallenge.setOnClickListener { startChallengeHandler() }

        viewModel.initialize().observe(viewLifecycleOwner) {}


        viewModel.warnings.observe(viewLifecycleOwner, Observer {
            it?.map {
                Toast.makeText(
                    requireContext(),
                    "$it",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        viewModel.session.observe(viewLifecycleOwner, Observer {
            if (it !== null)
                Toast.makeText(
                    requireContext(),
                    "3DS Session Created",
                    Toast.LENGTH_LONG
                ).show()
        })

        viewModel.challengeResponse.observe(viewLifecycleOwner, Observer {
            if (it?.status == "Y") {
                Toast.makeText(
                    requireContext(),
                    "3DS Challenge Succeeded",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (it?.status == "N") {
                Toast.makeText(
                    requireContext(),
                    "3DS Challenge Failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        })



        return binding.root
    }


    private fun createSessionHandler() {
        viewModel.createSession().observe(viewLifecycleOwner) {}
    }


    private fun startChallengeHandler() {
        val sessionId = requireNotNull(viewModel.session.value).id

        viewModel.startChallenge(sessionId, requireActivity()).observe(viewLifecycleOwner) {}
    }
}