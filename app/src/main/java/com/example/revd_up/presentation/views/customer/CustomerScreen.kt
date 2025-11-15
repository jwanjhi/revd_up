package com.example.revd_up.presentation.views.customer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.revd_up.data.api.Comment
import com.example.revd_up.data.api.RevdUpPostService
import com.example.revd_up.domain.model.Post
import kotlinx.coroutines.launch

/**
 * Customer's main feed screen, which is a destination in the bottom navigation.
 * It is responsible for fetching and displaying posts.
 */
@Composable
fun CustomerFeedScreen(postService: RevdUpPostService) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // A dummy user ID for now. In a real app, you'd get this from your AuthDataStore.
    val currentUserId = "user_123"

    // --- State for the comment dialog ---
    var showCommentDialog by remember { mutableStateOf(false) }
    var selectedPostIdForComment by remember { mutableStateOf<String?>(null) }

    fun handleLike(post: Post) {
        coroutineScope.launch {
            val success = postService.likePost(post.id!!, currentUserId)
            if (success) {
                // To reflect the change immediately, we update the local list
                posts = posts.map {
                    if (it.id == post.id) {
                        it.copy(likes = it.likes + 1) // Optimistically update the like count
                    } else {
                        it
                    }
                }
                Log.d("CustomerFeed", "Successfully liked post ${post.id}")
            } else {
                Log.e("CustomerFeed", "Failed to like post ${post.id}")
            }
        }
    }

    fun handleComment(postId: String, text: String) {
        coroutineScope.launch {
            val newComment = postService.addComment(postId, currentUserId, text)
            if (newComment != null) {
                // Optimistically update comment count
                posts = posts.map {
                    if (it.id == postId) {
                        it.copy(comments = ((it.comments ?: emptyList()) + newComment.id!!) as List<Comment>?)
                    } else {
                        it
                    }
                }
                Log.d("CustomerFeed", "Successfully commented on post $postId")
            } else {
                Log.e("CustomerFeed", "Failed to comment on post $postId")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            Log.d("CustomerFeedScreen", "Fetching posts from backend...")
            posts = postService.getAllPosts()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load posts"
            Log.e("CustomerFeedScreen", "Error loading posts: $errorMessage", e)
        } finally {
            isLoading = false
        }
    }

    // --- Content Display ---
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage != null -> {
                Text("Error: $errorMessage", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
            posts.isEmpty() -> {
                Text("No posts found.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = posts, key = { it.id ?: it.hashCode() }) { post ->
                        PostCard(
                            post = post,
                            onLikeClicked = { handleLike(post) },
                            onCommentClicked = {
                                selectedPostIdForComment = post.id
                                showCommentDialog = true
                            }
                        )
                    }
                }
            }
        }

        // --- Comment Dialog ---
        if (showCommentDialog && selectedPostIdForComment != null) {
            CommentDialog(
                onDismiss = { showCommentDialog = false },
                onComment = { commentText ->
                    handleComment(selectedPostIdForComment!!, commentText)
                    showCommentDialog = false
                }
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClicked: () -> Unit,
    onCommentClicked: () -> Unit
) {
    // Local state to manage the liked status visually
    var isLiked by remember { mutableStateOf(false) }
    val likeCount = post.likes + if (isLiked) 1 else 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Post Header ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "User_${post.userId?.take(4)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Just now", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { Log.d("PostCard", "More options clicked") }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // --- Post Content ---
            Text(post.caption, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // --- Post Footer (Actions) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like Button
                IconToggleButton(
                    checked = isLiked,
                    onCheckedChange = {
                        isLiked = !isLiked
                        onLikeClicked()
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Text("$likeCount Likes", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.width(16.dp))

                // Comment Button
                Row(
                    modifier = Modifier.clickable { onCommentClicked() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Comment", tint = Color.Gray)
                    Text(" ${post.comments?.size ?: 0} Comments", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
fun CommentDialog(onDismiss: () -> Unit, onComment: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a comment") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Your comment") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onComment(text)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
