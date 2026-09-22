package com.example.eventmanagement.ui.dashboard

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.model.Event
import com.example.eventmanagement.data.repository.AuthRepository
import com.example.eventmanagement.databinding.FragmentDashboardBinding
import com.example.eventmanagement.databinding.ViewDayLoadBinding
import com.example.eventmanagement.ui.events.DayLoad
import com.example.eventmanagement.ui.events.EventStats
import com.example.eventmanagement.ui.events.EventViewModel
import com.example.eventmanagement.util.DateUtils
import com.example.eventmanagement.util.animateCount
import com.example.eventmanagement.util.animatePop
import com.example.eventmanagement.util.staggerChildren
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventViewModel by viewModels { EventViewModel.Factory() }

    private var hasPlayedEntrance = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChartStyle()
        renderHeroIdentity()
        binding.dashboardContent.staggerChildren()

        viewModel.events.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.progressBar.isVisible = true

                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    bindDashboard(resource.data)
                }

                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindDashboard(events: List<Event>) {
        val stats = viewModel.computeStats(events)

        renderHeroMetrics(stats)
        renderStatCards(stats)
        renderNextEvent(viewModel.nextUpcomingEvent(events))
        renderWeekStrip(viewModel.nextSevenDays(events))
        renderInsights(events)
        renderChart(viewModel.eventsPerMonth(events))

        hasPlayedEntrance = true
    }

    private fun renderHeroIdentity() {
        val email = AuthRepository().currentUserEmail().orEmpty()
        val displayName = email.substringBefore('@').ifBlank { getString(R.string.app_name) }

        binding.tvAvatar.text = displayName.take(1).uppercase(Locale.getDefault())
        binding.tvGreeting.text = getString(greetingRes())
        binding.tvUserLine.text = getString(R.string.hero_today, DateUtils.todayLong())
    }

    private fun greetingRes(): Int {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }

    private fun renderHeroMetrics(stats: EventStats) {
        binding.tvHeroPlanned.text =
            resources.getQuantityString(R.plurals.hero_planned, stats.upcoming, stats.upcoming)
        binding.tvHeroWeek.text =
            resources.getQuantityString(R.plurals.hero_this_week, stats.thisWeek, stats.thisWeek)
        binding.tvCompletionValue.text =
            getString(R.string.completion_value, stats.completionPercent)
        binding.tvCompletionCaption.text = resources.getQuantityString(
            R.plurals.completion_caption,
            stats.total,
            stats.past,
            stats.total
        )

        ObjectAnimator.ofInt(
            binding.progressCompletion,
            "progress",
            binding.progressCompletion.progress,
            stats.completionPercent
        ).apply {
            duration = 900L
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun renderStatCards(stats: EventStats) {
        binding.tvTotalEvents.animateCount(stats.total)
        binding.tvUpcomingEvents.animateCount(stats.upcoming)
        binding.tvPastEvents.animateCount(stats.past)
    }

    private fun renderNextEvent(event: Event?) {
        if (event == null) {
            binding.tvCountdown.isVisible = false
            binding.tvNextTitle.setText(R.string.no_upcoming_event)
            binding.tvNextDate.setText(R.string.no_upcoming_event_hint)
            binding.tvNextLocation.isVisible = false
            return
        }

        val date = event.dateTime.toDate()
        binding.tvCountdown.isVisible = true
        binding.tvCountdown.text = when (val days = DateUtils.daysUntil(date)) {
            0 -> getString(R.string.countdown_today)
            1 -> getString(R.string.countdown_tomorrow)
            else -> resources.getQuantityString(R.plurals.countdown_days, days, days)
        }
        binding.tvNextTitle.text = event.title
        binding.tvNextDate.text = DateUtils.format(event.dateTime)
        binding.tvNextLocation.text =
            event.location.ifBlank { getString(R.string.location_not_set) }
        binding.tvNextLocation.isVisible = true

        if (!hasPlayedEntrance) binding.cardNextEvent.animatePop(delay = 260L)
    }

    private fun renderWeekStrip(days: List<DayLoad>) {
        val strip = binding.weekStrip
        strip.removeAllViews()

        val busiest = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        val maxBarPx = resources.getDimensionPixelSize(R.dimen.day_bar_max_height)
        val minBarPx = resources.getDimensionPixelSize(R.dimen.day_bar_min_height)
        val onSurface = MaterialColors.getColor(
            strip,
            com.google.android.material.R.attr.colorOnSurface
        )
        val onPrimary = MaterialColors.getColor(
            strip,
            com.google.android.material.R.attr.colorOnPrimary
        )

        days.forEachIndexed { index, day ->
            val column = ViewDayLoadBinding.inflate(layoutInflater, strip, false)
            column.tvDayCount.text = if (day.count > 0) day.count.toString() else ""
            column.tvDayLabel.text = day.label
            column.tvDayNumber.text = day.dayOfMonth

            if (day.isToday) {

                column.tvDayNumber.setBackgroundResource(R.drawable.bg_today_dot)
                column.tvDayNumber.setTextColor(onPrimary)
            } else {
                column.tvDayNumber.background = null
                column.tvDayNumber.setTextColor(onSurface)
            }

            val target = if (day.count == 0) {
                minBarPx
            } else {
                minBarPx + ((maxBarPx - minBarPx) * day.count / busiest)
            }
            growBar(column.barFill, target, delayMs = 120L + index * 55L)

            strip.addView(column.root)
        }
    }

    private fun growBar(bar: View, targetPx: Int, delayMs: Long) {
        val params = bar.layoutParams
        params.height = 0
        bar.layoutParams = params

        bar.postDelayed({
            if (_binding == null) return@postDelayed
            android.animation.ValueAnimator.ofInt(0, targetPx).apply {
                duration = 560L
                interpolator = DecelerateInterpolator(1.8f)
                addUpdateListener { animator ->
                    bar.layoutParams = bar.layoutParams.apply {
                        height = animator.animatedValue as Int
                    }
                }
                start()
            }
        }, delayMs)
    }

    private fun renderInsights(events: List<Event>) {
        binding.tvBusiestMonth.text =
            viewModel.busiestMonth(events) ?: getString(R.string.insight_none)

        val average = viewModel.monthlyAverage(events)
        binding.tvMonthlyAverage.text = if (average == 0.0) {
            getString(R.string.insight_none)
        } else {
            String.format(Locale.getDefault(), "%.1f", average)
        }
    }

    private fun setupChartStyle() {
        val labelColor = MaterialColors.getColor(
            binding.barChart,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        val gridColor = MaterialColors.getColor(
            binding.barChart,
            com.google.android.material.R.attr.colorOutlineVariant
        )
        val labelTypeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)

        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            setScaleEnabled(false)
            extraBottomOffset = 8f

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.textColor = labelColor
            xAxis.textSize = 10f
            xAxis.typeface = labelTypeface
            xAxis.yOffset = 10f

            axisLeft.axisMinimum = 0f
            axisLeft.granularity = 1f
            axisLeft.textColor = labelColor
            axisLeft.gridColor = gridColor
            axisLeft.setDrawAxisLine(false)
            axisLeft.typeface = labelTypeface
            axisLeft.enableGridDashedLine(8f, 8f, 0f)

            setNoDataText("")
        }
    }

    private fun renderChart(monthCounts: Map<String, Int>) {
        if (monthCounts.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.isVisible = false
            binding.chartEmptyGroup.isVisible = true
            return
        }
        binding.barChart.isVisible = true
        binding.chartEmptyGroup.isVisible = false

        val labels = monthCounts.keys.toList()
        val entries = monthCounts.values.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val startColor = MaterialColors.getColor(
            binding.barChart,
            com.google.android.material.R.attr.colorPrimary
        )
        val endColor = MaterialColors.getColor(
            binding.barChart,
            com.google.android.material.R.attr.colorTertiary
        )
        val labelColor = MaterialColors.getColor(
            binding.barChart,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        val dataSet = BarDataSet(entries, "Events").apply {
            setGradientColor(startColor, endColor)
            valueTextColor = labelColor
            valueTextSize = 11f
            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
            setDrawValues(true)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = value.toInt().toString()
            }
        }

        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.xAxis.labelCount = labels.size
        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.45f }
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
