package com.example.habitos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedActivity : AppCompatActivity() { // Or Fragment, depending on your app's structure

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed) // Create a layout file for the feed

        recyclerView = findViewById(R.id.recyclerViewFeed) //  Make sure you have a RecyclerView with this ID in your layout
        recyclerView.layoutManager = LinearLayoutManager(this)
        postList = mutableListOf()
        postAdapter = PostAdapter(this, postList)
        recyclerView.adapter = postAdapter
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        loadPosts()
    }

    private fun loadPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FeedActivity", "Listen failed: ", error)
                    Toast.makeText(this, "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    postList.clear()
                    for (document in snapshot) {
                        try {
                            val post = document.toObject(Post::class.java)
                            post?.let {
                                postList.add(it)
                            }
                        } catch (e: Exception) {
                            Log.e("FeedActivity", "Error converting document to Post: ", e)
                            Toast.makeText(this, "Error processing post data.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                } else {
                    Log.d("FeedActivity", "Current data: null")
                    Toast.makeText(this, "No posts yet.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

data class Post(
    val userId: String = "",
    val caption: String = "",
    val timestamp: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null
)

class PostAdapter(private val context: Context, private val postList: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false) // item_post.xml
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]
        holder.captionTextView.text = currentPost.caption

        // Load image if available
        if (currentPost.imageUrl != null && currentPost.imageUrl.isNotEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            holder.videoPlayerView.visibility = View.GONE
            Glide.with(context)
                .load(currentPost.imageUrl)
                .into(holder.imageView)
        } else if (currentPost.videoUrl != null && currentPost.videoUrl.isNotEmpty()) {
            // Load video if available
            holder.imageView.visibility = View.GONE
            holder.videoPlayerView.visibility = View.VISIBLE
            holder.initializeExoPlayer(holder.videoPlayerView, currentPost.videoUrl)
        } else {
            holder.imageView.visibility = View.GONE
            holder.videoPlayerView.visibility = View.GONE
        }
    }

    private fun PostViewHolder.initializeExoPlayer(playerView: PlayerView, videoUrl: String) {
        val exoPlayer = SimpleExoPlayer.Builder(context).build()
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Optional: Manage player lifecycle
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {}
                    Player.STATE_BUFFERING -> {}
                    Player.STATE_IDLE -> {}
                    Player.STATE_ENDED -> {}
                    else -> {}
                }
            }

            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                Log.e("PostAdapter", "ExoPlayer Error: ${error.message}")
            }
        })
        this.exoPlayer = exoPlayer
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        // Release ExoPlayer
        holder.exoPlayer?.release()
        holder.exoPlayer = null
        holder.videoPlayerView.player = null // Important: Detach player from PlayerView
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val captionTextView: TextView = itemView.findViewById(R.id.textViewCaption)
        val imageView: ImageView = itemView.findViewById(R.id.imageViewPost)
        val videoPlayerView: PlayerView = itemView.findViewById(R.id.videoViewPost)
        var exoPlayer: SimpleExoPlayer? = null // Hold a reference to the player
    }
}