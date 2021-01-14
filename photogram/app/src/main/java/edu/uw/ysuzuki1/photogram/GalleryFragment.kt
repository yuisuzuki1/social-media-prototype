package edu.uw.ysuzuki1.photogram

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.security.AccessController.getContext

/**
 * Fragment to hold photo and comment of post. File also includes the adapter class to bind each post components
 * to the recycler objects
 */
class GalleryFragment : Fragment() {

    private val viewModel by viewModels<FirebaseViewModel>()

    lateinit var adapter: PhotoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_gallery, container, false)

        val uploadBtn = rootView.findViewById<FloatingActionButton>(R.id.upload_fab)

        // Upload button will appear when user is signed in
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
            when (authenticationState) {
                FirebaseViewModel.AuthenticationState.AUTHENTICATED -> {
                    uploadBtn.show()
                }
                else -> {
                    uploadBtn.hide()
                }
            }
        })

        // Go to the upload screen when the upload button is clicked
        uploadBtn.setOnClickListener {
            val action = GalleryFragmentDirections.toUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }

        val options: FirebaseRecyclerOptions<PhotoDetails> = FirebaseRecyclerOptions.Builder<PhotoDetails>()
            .setQuery(FirebaseDatabase.getInstance().reference, PhotoDetails::class.java)
            .build()

        // Set up the recyler with the posts as the objects
        adapter = PhotoAdapter(options, this)
        val recycler: RecyclerView = rootView.findViewById(R.id.photos_recycler)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter

        return rootView
    }

    override fun onStart() { // onStart and onStop ensures change to this page is only being observed during proper lifecycle state
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}

/**
 * Extends a recycler adapter to ensure the data of each post is binded to the recycler object
 */
class PhotoAdapter(private val options: FirebaseRecyclerOptions<PhotoDetails>, private val frag: Fragment)
    : FirebaseRecyclerAdapter<PhotoDetails, PhotoAdapter.PhotoHolder>(options) {

    // Links components of the post to be referenced to the XML
    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById<ImageView>(R.id.uploaded_img)
        val caption: TextView = view.findViewById(R.id.caption_display)
        val likes: TextView = view.findViewById(R.id.likes_num)
        val likeBtn: ImageButton = view.findViewById(R.id.like_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {

        val inflatedView = LayoutInflater.from(parent.context).inflate(
            R.layout.photo_item,
            parent,
            false
        )

        return PhotoHolder(inflatedView)
    }

    // Binds the information about the text to the PhotoHolder object creates above for the recycler
    override fun onBindViewHolder(holder: PhotoHolder, position: Int, model: PhotoDetails) {
        val ref = getRef(position)
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        // Determines the number of likes the post has
        var likes = model.likes.filterValues { it }.size
        if (model.likes.isEmpty()) {
            likes = 0
        }

        // Uses Glide to display with firebase reference url
        Glide.with(frag)
            .load(model.url)
            .into(holder.image)

        // Adds a tint to the like button if the user has liked the post
        if (model.likes[uid] == true) {
            holder.likeBtn.imageTintList = ColorStateList.valueOf(holder.likeBtn.context.getColor(R.color.colorLike))
        }
        if (model.likes[uid] == false) {
            holder.likeBtn.imageTintList = ColorStateList.valueOf(holder.likeBtn.context.getColor(R.color.colorUnlike))
        }

        holder.caption.text = model.caption
        holder.likes.text = "$likes"

        // Adds that the user liked the image to firebase database
        holder.likeBtn.setOnClickListener {
            if (uid != null) {
                if (model.likes[uid] == true) { model.likes[uid] = false }
                else if (model.likes[uid] == false) { model.likes[uid] = true }
                ref.setValue(model)
            }
        }
    }
}