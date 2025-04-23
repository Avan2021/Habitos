package com.example.habitos.View

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habitos.R
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Date

class PostHabitActivity : AppCompatActivity() {

    private lateinit var editTextHabit: EditText
    private lateinit var buttonChooseImage: Button
    private lateinit var buttonChooseVideo: Button
    private lateinit var mediaPreviewContainer: FrameLayout
    private lateinit var imageViewPreview: ImageView
    private lateinit var videoViewPreview: PlayerView
    private lateinit var buttonPost: Button

    private var selectedImageUri: Uri? = null
    private var selectedVideoUri: Uri? = null
    private var exoPlayer: SimpleExoPlayer? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth

    private val imagePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                imageViewPreview.setImageURI(selectedImageUri)
                imageViewPreview.visibility = View.VISIBLE
                exoPlayer?.release()
                exoPlayer = null
                videoViewPreview.visibility = View.GONE
            }
        }

    private val videoPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedVideoUri = result.data?.data
                selectedVideoUri?.let { initializeExoPlayer(it) }
                videoViewPreview.visibility = View.VISIBLE
                imageViewPreview.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_habit)

        editTextHabit = findViewById(R.id.editTextHabit)
        buttonChooseImage = findViewById(R.id.buttonChooseImage)
        buttonChooseVideo = findViewById(R.id.buttonChooseVideo)
        mediaPreviewContainer = findViewById(R.id.mediaPreviewContainer)
        imageViewPreview = findViewById(R.id.imageViewPreview)
        videoViewPreview = findViewById(R.id.videoViewPreview)
        buttonPost = findViewById(R.id.buttonPost)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        buttonChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        buttonChooseVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            videoPickerLauncher.launch(intent)
        }

        buttonPost.setOnClickListener {
            postHabit()
        }
    }

    private fun initializeExoPlayer(videoUri: Uri) {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        videoViewPreview.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play() // Optionally start playing immediately
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun postHabit() {
        val habitText = editTextHabit.text.toString().trim()

        if (habitText.isEmpty() && selectedImageUri == null && selectedVideoUri == null) {
            Toast.makeText(this, "Please enter a habit or select media.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }
        val timestamp = Date().time.toString()
        val mediaRef = storage.getReference().child("posts/${userId}_$timestamp")

        // Create a map for the post data
        val post = mutableMapOf<String, Any>(
            "userId" to userId,
            "caption" to habitText,
            "timestamp" to timestamp
        )

        if (selectedImageUri != null) {
            // Upload image
            mediaRef.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    mediaRef.downloadUrl.addOnSuccessListener { uri ->
                        post["imageUrl"] = uri.toString()
                        savePostToFirestore(post)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Optionally still save the text if media upload fails
                    savePostToFirestore(post)
                }
        } else if (selectedVideoUri != null) {
            // Upload video
            mediaRef.putFile(selectedVideoUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    mediaRef.downloadUrl.addOnSuccessListener { uri ->
                        post["videoUrl"] = uri.toString()
                        savePostToFirestore(post)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload video: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Optionally still save the text if media upload fails
                    savePostToFirestore(post)
                }
        } else {
            // Only text, save directly
            savePostToFirestore(post)
        }
    }

    private fun savePostToFirestore(post: Map<String, Any>) {
        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the previous screen
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to share post: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PostHabitActivity", "Error saving post to Firestore: ", e)
            }
    }
}


