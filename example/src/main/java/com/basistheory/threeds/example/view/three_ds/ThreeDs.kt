package com.basistheory.threeds.example.view.three_ds

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.basistheory.threeds.example.databinding.FragmentThreeDsBinding
import com.basistheory.threeds.example.viewmodel.ThreeDsViewModel
import com.basistheory.threeds.service.ThreeDsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThreeDs : Fragment() {
    private val binding: FragmentThreeDsBinding  by lazy {
        FragmentThreeDsBinding.inflate(layoutInflater)
    }

    private val threeDsService = ThreeDsService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewModel = ThreeDsViewModel(threeDsService);


        this.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                binding.viewModel!!.initialize(requireContext().applicationContext)
            }
        }
    }
}