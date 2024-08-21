package com.basistheory.threeds.example.view.three_ds

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val threeDsService = ThreeDsService
            .Builder()
            .withApiKey("btApiKey")
            .withApplicationContext(requireContext().applicationContext)
            .withRegion(Region.EU)
            .build()

        binding.viewModel = ThreeDsViewModel(threeDsService);


        this.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                binding.viewModel!!.initialize()
            }
        }
    }
}