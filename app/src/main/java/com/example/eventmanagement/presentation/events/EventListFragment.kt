package com.example.eventmanagement.presentation.events

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventmanagement.R
import com.example.eventmanagement.core.ui.startSkeletonPulse
import com.example.eventmanagement.core.ui.staggerChildren
import com.example.eventmanagement.core.ui.stopSkeletonPulse
import com.example.eventmanagement.core.ui.toUserMessage
import com.example.eventmanagement.databinding.FragmentEventListBinding
import com.example.eventmanagement.domain.model.EventFilter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventListViewModel by viewModels()
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
        collectState()
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

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    viewModel.effects.collect { effect -> handleEffect(effect) }
                }
            }
        }
    }

    private fun render(state: EventListUiState) {
        if (state.isLoading && isFirstLoad) {
            showSkeleton(true)
            return
        }
        isFirstLoad = false
        showSkeleton(false)
        adapter.submitList(state.events) {
            if (state.events.isNotEmpty()) binding.recyclerEvents.scheduleLayoutAnimation()
        }
        renderEmptyState(state.events.isEmpty())
    }

    private fun handleEffect(effect: EventListUiEffect) {
        when (effect) {
            is EventListUiEffect.Error ->
                Snackbar.make(binding.root, effect.error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            EventListUiEffect.Deleted ->
                Snackbar.make(binding.root, R.string.event_deleted, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fabAdd)
                    .show()
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
