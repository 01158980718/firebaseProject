package com.example.firebase1

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.firebase1.databinding.FragmentAddBinding
import com.example.firebase1.models.Contacts
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddFragment : Fragment() {

    lateinit var binding: FragmentAddBinding
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var storage: StorageReference
    private var uri: Uri? = null
    lateinit var imageUrl:String// Uri for the image

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddBinding.inflate(inflater, container, false)
        firebaseRef = FirebaseDatabase.getInstance().getReference("contact")
        storage = FirebaseStorage.getInstance().getReference("images")

        // Save data to Firebase on button click
        binding.button.setOnClickListener {
            saveData()
        }

        // Register an ActivityResult for image selection
        val imagePick = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.image.setImageURI(it)
            uri = it // Store the selected Uri
        }

        // Launch image picker on button click
        binding.btnPick.setOnClickListener {
            imagePick.launch("image/*")
        }

        return binding.root
    }

    // Function to save data to Firebase
    private fun saveData() {
        val name = binding.editText.text.toString()
        val phone = binding.editPhone.text.toString()

        // Ensure both name and phone are provided
        if (name.isNotEmpty() && phone.isNotEmpty()) {
            // Disable the button to prevent multiple clicks
            binding.button.isEnabled = false

            // Check if contact with the same name and phone exists in Firebase
            firebaseRef.orderByChild("name").equalTo(name).get().addOnSuccessListener { snapshot ->
                var contactExists = false

                for (contactSnap in snapshot.children) {
                    val existingContact = contactSnap.getValue(Contacts::class.java)
                    if (existingContact != null && existingContact.phone == phone) {
                        contactExists = true  // Contact with the same name and phone exists
                        if (existingContact.imgUri == uri.toString()) {
                            // Contact with the same name, phone, and image URL exists
                            Toast.makeText(requireContext(), "Contact already exists with the same image", Toast.LENGTH_SHORT).show()
                            binding.button.isEnabled = true  // Re-enable the button
                            return@addOnSuccessListener
                        }
                        break
                    }
                }

                if (contactExists) {
                    // Contact already exists, show a message
                    Toast.makeText(requireContext(), "Contact already exists", Toast.LENGTH_SHORT).show()
                    binding.button.isEnabled = true  // Re-enable the button
                } else {
                    // Add the new contact to Firebase
                    val contactId = firebaseRef.push().key!!
                    uploadImageToFirebase(contactId, name, phone)
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.button.isEnabled = true  // Re-enable the button on error
            }
        } else {
            // Show error if either name or phone is empty
            if (name.isEmpty()) {
                binding.editText.error = "Write the name"
            }
            if (phone.isEmpty()) {
                binding.editPhone.error = "Write the phone number"
            }
        }
    }


    // Function to upload the selected image to Firebase Storage
    private fun uploadImageToFirebase(contactId: String, name: String, phone: String) {
        if (uri != null) {
            val imageRef = storage.child("$contactId.jpg")

            imageRef.putFile(uri!!).addOnSuccessListener {
                // Get the download URL of the uploaded image
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                     imageUrl = downloadUri.toString()  // URL of the uploaded image
                    saveContactToFirebase(contactId, name, phone, imageUrl)  // Save contact with image URL
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to get image URL: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If no image was selected, save the contact without image URL
            saveContactToFirebase(contactId, name, phone, null)
        }
    }

    // Function to save the contact data to Firebase
    private fun saveContactToFirebase(contactId: String, name: String, phone: String, imageUrl: String?) {
        val contacts = Contacts(contactId, name, phone, imageUrl)  // Contact object with imageUrl
        firebaseRef.child(contactId).setValue(contacts).addOnCompleteListener {
            Toast.makeText(requireContext(), "Successfully added to database", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
