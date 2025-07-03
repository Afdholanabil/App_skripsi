package com.healour.anxiety.ui.dashboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.healour.anxiety.ui.dashboard.checkanxiety.CheckAnxietyFragment
import com.healour.anxiety.ui.dashboard.home.HomeFragment
import com.healour.anxiety.ui.dashboard.profile.ProfileFragment

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