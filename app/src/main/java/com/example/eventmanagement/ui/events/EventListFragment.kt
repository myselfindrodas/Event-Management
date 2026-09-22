package com.example.eventmanagement.ui.events

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.model.EventFilter
import com.example.eventmanagement.databinding.FragmentEventListBinding
import com.example.eventmanagement.util.staggerChildren
import com.example.eventmanagement.util.startSkeletonPulse
import com.example.eventmanagement.util.stopSkeletonPulse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventViewModel by viewModels { EventViewModel.Factory() }
    private lateinit var adapter: EventAdapter

    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupSearchAndFilters()
        setupFab()

        binding.listContent.staggerChildren()
        showSkeleton(true)

        observeViewModel()
    }

    private fun setupRecycler() {
        adapter = EventAdapter(
            onClick = { event ->
                startActivity(
                    Intent(requireContext(), EventEditorActivity::class.java).apply {
                        putExtra(EventEditorActivity.EXTRA_EVENT_ID, event.id)
                    }
                )
            },
            onDelete = { event -> confirmDelete(event.id, event.title) }
        )

        binding.recyclerEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@EventListFragment.adapter
            itemAnimator = DefaultItemAnimator().apply { addDuration = 220L }
            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(),
                R.anim.layout_animation_fall_down
            )

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 8) binding.fabAdd.shrink() else if (dy < -8) binding.fabAdd.extend()
                }
            })
        }
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipUpcoming -> EventFilter.UPCOMING
                R.id.chipPast -> EventFilter.PAST
                else -> EventFilter.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), EventEditorActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> if (isFirstLoad) showSkeleton(true)

                is Resource.Success -> {
                    isFirstLoad = false
                    showSkeleton(false)
                }

                is Resource.Error -> {
                    isFirstLoad = false
                    showSkeleton(false)
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.filteredEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events) {
                if (events.isNotEmpty()) binding.recyclerEvents.scheduleLayoutAnimation()
            }
            renderEmptyState(events.isEmpty())
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()

                is Resource.Success ->
                    Snackbar.make(binding.root, R.string.event_deleted, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.fabAdd)
                        .show()

                Resource.Loading -> Unit
            }
        }
    }

    private fun showSkeleton(visible: Boolean) {
        val container = binding.skeleton.skeletonContainer
        container.isVisible = visible
        binding.recyclerEvents.isVisible = !visible
        if (visible) {
            binding.emptyGroup.isVisible = false
            container.startSkeletonPulse()
        } else {
            container.stopSkeletonPulse()
        }
    }

    private fun renderEmptyState(isEmpty: Boolean) {
        if (isFirstLoad) return

        binding.emptyGroup.isVisible = isEmpty
        if (!isEmpty) return

        val isFiltering = binding.etSearch.text?.isNotBlank() == true ||
            binding.chipGroupFilter.checkedChipId != R.id.chipAll

        binding.ivEmpty.setImageResource(
            if (isFiltering) R.drawable.ic_empty_search else R.drawable.ic_empty_calendar
        )
        binding.tvEmptyTitle.setText(
            if (isFiltering) R.string.empty_search_title else R.string.empty_events_title
        )
        binding.tvEmptyBody.setText(
            if (isFiltering) R.string.empty_search_body else R.string.empty_events_body
        )
        binding.emptyGroup.staggerChildren(stepMs = 70L, startDelayMs = 0L)
    }

    private fun confirmDelete(eventId: String, title: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.delete_event)
            .setMessage(getString(R.string.delete_event_message, title))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteEvent(eventId) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
