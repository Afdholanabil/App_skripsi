package com.example.app_skripsi.ui.dashboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.app_skripsi.ui.dashboard.checkanxiety.CheckAnxietyFragment
import com.example.app_skripsi.ui.dashboard.home.HomeFragment
import com.example.app_skripsi.ui.dashboard.profile.ProfileFragment

class DashboardPagerAdapter(activity:FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        HomeFragment(),
        CheckAnxietyFragment(),
        ProfileFragment()
    )
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment = fragments[position]

}