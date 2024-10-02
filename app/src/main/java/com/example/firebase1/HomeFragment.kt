package com.example.firebase1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase1.adapter.ContactsAdapter
import com.example.firebase1.databinding.FragmentHomeBinding
import com.example.firebase1.models.Contacts
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    lateinit var adapter: ContactsAdapter
    lateinit var contactsList: MutableList<Contacts>
    lateinit var firebaseReference: DatabaseReference
    lateinit var storageReference: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize binding and setup
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        storageReference=FirebaseStorage.getInstance().getReference("images")
        contactsList = mutableListOf()

        // Set up button navigation
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addFragment)
        }

        // Set up Firebase reference
        firebaseReference = FirebaseDatabase.getInstance().getReference("contact")

        // Initialize RecyclerView and adapter
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactsAdapter(contactsList, onItemUpdate ={
                task->
            findNavController().navigate(R.id.action_homeFragment_to_updateFragment, bundleOf(
                "name" to task.name,
                "phone" to task.phone,
                "id" to task.id,
                "imageUrl" to task.imgUri

            ))
        }, onItemDelete = { adapterPair ->
            showDeleteConfirmationDialog(adapterPair)
        })  // Initialize the adapter
        binding.rvContacts.adapter = adapter  // Set the adapter to RecyclerView

        // Fetch data from Firebase
        fetchData()

        return binding.root
    }

    private fun fetchData() {
        firebaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactsList.clear()  // Clear list before adding new data
                if (snapshot.exists()) {
                    for (contactsSnap in snapshot.children) {
                        val contact = contactsSnap.getValue(Contacts::class.java)
                        if (contact != null) {
                            contactsList.add(contact)  // Add valid contact to the list
                        }
                    }
                    adapter.notifyDataSetChanged()  // Notify adapter of data change
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteConfirmationDialog(adapterPair: Pair<Contacts, Int>) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteContact(adapterPair)
                dialog.dismiss()
                Toast.makeText(requireContext(), "succesfully deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
            }
            .create()
        dialog.show()
    }

    private fun deleteContact(adapterPair: Pair<Contacts, Int>) {
        val contact = adapterPair.first // The contact object to be deleted
        val position = adapterPair.second // The position in the list

        // Construct the correct path for the image
        val imageRef = storageReference.child("${contact.id}.jpg") // Ensure this matches how you stored it

        // Remove the contact from Firebase using its ID
        firebaseReference.child(contact.id!!).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Remove the contact from the local list after Firebase deletion is successful
                contactsList.removeAt(position)
                // Notify the adapter that an item has been removed
                adapter.notifyItemRemoved(position)

                // Now delete the image from storage
                imageRef.delete().addOnSuccessListener {
                    Toast.makeText(requireContext(), "Contact and image removed successfully", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to delete image: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(requireContext(), "Failed to remove contact from Firebase", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
