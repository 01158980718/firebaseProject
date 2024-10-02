package com.example.firebase1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebase1.databinding.FragmentUpdateBinding
import com.example.firebase1.models.Contacts
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

@Suppress("DEPRECATION")
class UpdateFragment : Fragment() {

    lateinit var binding: FragmentUpdateBinding
    lateinit var firebaseRef: DatabaseReference
    private var imageUrl: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUpdateBinding.inflate(inflater, container, false)
        firebaseRef = FirebaseDatabase.getInstance().getReference("contact")

        // Populate fields with current data
        binding.updatename.setText(arguments?.getString("name"))
        binding.updatephone.setText(arguments?.getString("phone"))
        imageUrl = arguments?.getString("imageUrl")
        Picasso.get().load(imageUrl).into(binding.updateimage)

        // Set up the update function
        updateFunction()

        // Set onClickListener for image update
        binding.updateimage.setOnClickListener {
            showImageUpdateDialog()
        }

        return binding.root
    }

    private fun showImageUpdateDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Update Image")
            .setMessage("Do you want to update this image?")
            .setPositiveButton("Yes") { dialog, _ ->
                openImagePicker()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                uploadImageToStorage(it)
            }
        }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        val contactId = arguments?.getString("id")
        if (contactId != null) {
            val storageRef = FirebaseStorage.getInstance().getReference("images/${contactId}.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    // Get the download URL
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Update the contact in the database with the new image URL
                        updateContactImageUrl(contactId, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateContactImageUrl(contactId: String, imageUrl: String) {
        firebaseRef.child(contactId).child("imgUri").setValue(imageUrl)
            .addOnCompleteListener {
                Toast.makeText(requireContext(), "Image updated successfully", Toast.LENGTH_SHORT).show()
                // Load the new image
                Picasso.get().load(imageUrl).into(binding.updateimage)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update image URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun updateFunction() {
        // Set up the button click listener
        binding.sendData.setOnClickListener {
            // Retrieve the updated values inside the click listener
            val name = binding.updatename.text.toString().trim()
            val phone = binding.updatephone.text.toString().trim()

            // Check for non-empty fields before attempting to update
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val contactId = arguments?.getString("id")
                val contacts = Contacts(contactId, name, phone)

                if (contactId != null) {
                    // Disable the button to prevent multiple clicks
                    binding.sendData.isEnabled = false

                    firebaseRef.child(contactId).setValue(contacts).addOnCompleteListener {
                        Toast.makeText(
                            requireContext(),
                            "Successfully updated in the database",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Re-enable the button after the operation completes
                        binding.sendData.isEnabled = true
                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Re-enable the button in case of failure
                        binding.sendData.isEnabled = true
                    }
                }
            } else {
                // Handle case when fields are empty
                if (name.isEmpty()) {
                    binding.updatename.error = "Please enter a name"
                }
                if (phone.isEmpty()) {
                    binding.updatephone.error = "Please enter a phone number"
                }
            }
        }
    }
}
