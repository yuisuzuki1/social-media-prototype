package edu.uw.ysuzuki1.photogram

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.IOException
import java.sql.Timestamp

/**
 * Fragment where users will upload an image and post a picture to be seen in the main feed
 */
class UploadFragment : Fragment() {

    private val viewModel by viewModels<FirebaseViewModel>()

    private val PICK_PHOTO_CODE = 2

    private var chosenUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        // If user is on this page while being unauthenticated, will recieve a toast prompting a sign in
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
        if (authenticationState == FirebaseViewModel.AuthenticationState.UNAUTHENTICATED) {
            navController.navigate(R.id.GalleryFragment)
            Toast.makeText(
                activity, "Please log in before uploading a picture", Toast.LENGTH_SHORT)
                .show()
        }
    })
}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_upload, container, false)

        // Sends an intent to android os when upload icon is clicked to access a photo gallery
        rootView.findViewById<ImageView>(R.id.upload_image).setOnClickListener{
            val intent: Intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(intent, PICK_PHOTO_CODE)
            }
        }

        // Uploads image to firebase when post button is clicked
        rootView.findViewById<Button>(R.id.submit_btn).setOnClickListener { it ->
            // Stores images to firebase storage by creating a reference
            if(chosenUri != null) {
                val captionTxt = rootView.findViewById<EditText>(R.id.img_caption).text.toString()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val storageRef = Firebase.storage.reference

                val riversRef = storageRef.child(
                    "uploads/$uid/${Timestamp(System.currentTimeMillis())}")
                val uploadTask = riversRef.putFile(chosenUri!!)


                uploadTask.addOnFailureListener {
                    Toast.makeText(activity, "Could not upload. Error: ${it.message}", Toast.LENGTH_LONG).show()
                }.addOnSuccessListener {
                    val urlTask = uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        riversRef.downloadUrl
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val ref= Firebase.database.reference
                            val thisPhoto: PhotoDetails = PhotoDetails(
                                url = "${task.result}",
                                caption = captionTxt,
                                uid = uid,
                                likes = mutableMapOf<String, Boolean>()
                            )
                            ref.push().setValue(thisPhoto)
                        } else {
                            Toast.makeText(activity, "Cannot download uploaded photo", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // navigate back to the gallery fragment after upload complete
                val action = UploadFragmentDirections.toGalleryFragment()
                Navigation.findNavController(it).navigate(action)
            }
        }
        return rootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == PICK_PHOTO_CODE) {
            chosenUri = data.data

            val selectedImage = chosenUri?.let { loadFromUri(it) }

            val ivPreview = requireView().findViewById<ImageView>(R.id.upload_image) as ImageView
            ivPreview.setImageBitmap(selectedImage)
        }
    }

    // Helper function to gather the uri of the uploaded image
    private fun loadFromUri(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            image = if (Build.VERSION.SDK_INT > 27) {
                val source: ImageDecoder.Source = ImageDecoder.createSource(requireActivity().contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }
}

/**
 * A data class for the post details
 */
data class PhotoDetails(
    val url: String = "",
    val caption: String = "",
    val uid: String = "",
    val likes: MutableMap<String, Boolean> = mutableMapOf() //used to see if user has liked the image
)
