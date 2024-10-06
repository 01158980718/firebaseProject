package com.example.firebase1.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase1.databinding.RvItemBinding
import com.example.firebase1.models.Contacts
import com.squareup.picasso.Picasso


class ContactsAdapter(val contactsList: MutableList<Contacts>, val onItemUpdate: (Contacts) -> Unit,val onItemDelete:(Pair<Contacts, Int>) -> Unit):RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):ContactViewHolder{
        val binding = RvItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder:ContactViewHolder, position: Int) {
        holder.bindData(contactsList[position], position) // Pass position to the bindData method
    }

    override fun getItemCount() = contactsList.size // Return the size of userList

    inner class ContactViewHolder(private val binding: RvItemBinding ) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(contacts: Contacts, position: Int) {
            binding.id.text = contacts.id
            binding.name.text = contacts.name
            binding.phone.text = contacts.phone
            Picasso.get().load(contacts.imgUri).into(binding.pickedImage)
            binding.root.setOnClickListener{
                onItemUpdate(contacts)
            }
            binding.root.setOnLongClickListener {
                onItemDelete(Pair(contacts,adapterPosition))
                true
            }
        }
    }
}
