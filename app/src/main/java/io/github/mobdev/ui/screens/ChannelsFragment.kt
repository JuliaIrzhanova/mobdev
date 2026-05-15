package io.github.mobdev.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentChannelsBinding
import io.github.mobdev.ui.viewmodel.ChannelsViewModel

class ChannelsFragment : Fragment() {

    private var _binding: FragmentChannelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChannelsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvUsername.text = viewModel.username

        val adapter = ChannelAdapter { channel ->
            openMessages(channel)
        }

        binding.rvChannels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChannels.adapter = adapter

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            adapter.setChannels(channels)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout {
                requireActivity().runOnUiThread {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, LoginFragment())
                        .commit()
                }
            }
        }
    }

    private fun openMessages(channel: String) {
        val fragment = MessagesFragment.newInstance(channel)

        // Проверяем есть ли правый контейнер (ландшафт)
        val rightContainer = requireActivity().findViewById<View?>(R.id.containerRight)

        if (rightContainer != null) {
            // Ландшафт — открываем справа
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.containerRight, fragment)
                .commit()
        } else {
            // Портрет — открываем на весь экран
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChannelAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    private var channels: List<String> = emptyList()
    private var selectedChannel: String? = null

    fun setChannels(list: List<String>) {
        channels = list
        notifyDataSetChanged()
    }

    fun setSelected(channel: String) {
        selectedChannel = channel
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvName.text = channel
        holder.tvName.isSelected = channel == selectedChannel
        holder.tvName.setBackgroundResource(
            if (channel == selectedChannel)
                android.R.color.darker_gray
            else
                android.R.color.transparent
        )
        holder.itemView.setOnClickListener {
            selectedChannel = channel
            notifyDataSetChanged()
            onClick(channel)
        }
    }

    override fun getItemCount() = channels.size
}