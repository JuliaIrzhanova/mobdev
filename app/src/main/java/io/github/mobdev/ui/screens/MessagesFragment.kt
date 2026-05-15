package io.github.mobdev.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentMessagesBinding
import io.github.mobdev.network.models.Message
import io.github.mobdev.ui.viewmodel.MessagesViewModel

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessagesViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, arguments) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                handle["channel"] = arguments?.getString(ARG_CHANNEL) ?: "1@channel"
                @Suppress("UNCHECKED_CAST")
                return MessagesViewModel(requireActivity().application, handle) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MessagesAdapter { imageUrl ->
            openImage(imageUrl)
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.setMessages(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error ?: return@observe
            if (error == "401") {
                navigateToLogin()
            } else {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.sending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_empty_message),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(text)
            binding.etMessage.setText("")
        }

        viewModel.loadMessages()
    }

    private fun openImage(imageUrl: String) {
        val fragment = ImageFragment.newInstance(imageUrl)
        val rightContainer = requireActivity().findViewById<View?>(R.id.containerRight)
        val containerId = if (rightContainer != null) R.id.containerRight else R.id.container
        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLogin() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, LoginFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CHANNEL = "channel"

        fun newInstance(channel: String): MessagesFragment {
            return MessagesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CHANNEL, channel)
                }
            }
        }
    }
}

class MessagesAdapter(
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    private var messages: List<Message> = emptyList()

    fun setMessages(list: List<Message>) {
        messages = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFrom: TextView = itemView.findViewById(R.id.tvFrom)
        val tvText: TextView = itemView.findViewById(R.id.tvText)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.tvFrom.text = message.from

        val textData = message.data.text
        val imageData = message.data.image

        if (textData != null) {
            holder.tvText.visibility = View.VISIBLE
            holder.ivThumb.visibility = View.GONE
            holder.tvText.text = textData.text
        } else if (imageData != null) {
            holder.tvText.visibility = View.GONE
            holder.ivThumb.visibility = View.VISIBLE
            val thumbUrl = "https://faerytea.name/thumb/${imageData.link}"
            val fullUrl = "https://faerytea.name/img/${imageData.link}"
            holder.ivThumb.load(thumbUrl)
            holder.ivThumb.setOnClickListener {
                onImageClick(fullUrl)
            }
        }
    }

    override fun getItemCount() = messages.size
}