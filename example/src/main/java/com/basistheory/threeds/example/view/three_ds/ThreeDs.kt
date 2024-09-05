package com.basistheory.threeds.example.view.three_ds

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.basistheory.threeds.example.databinding.FragmentThreeDsBinding
import com.basistheory.threeds.example.viewmodel.ThreeDsViewModel
import com.basistheory.threeds.service.Region
import com.basistheory.threeds.service.ThreeDsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        viewModel.initialize().observe(viewLifecycleOwner) {}

        return binding.root
    }


    private fun createSessionHandler() {
        Log.i("3ds_service", "Creating Session")
        viewModel.createSession("<TOKEN ID>").observe(viewLifecycleOwner) {}
    }
}