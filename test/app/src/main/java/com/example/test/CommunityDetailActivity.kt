/*커뮤니티 상세*/
package com.example.test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.example.test.model.board.CommentListResponse
import com.example.test.model.board.CommentRequest
import com.example.test.model.board.CommentResponse
import com.example.test.model.board.CommunityDetailResponse
import com.example.test.network.RetrofitInstance
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityDetailActivity : AppCompatActivity() {
    private val postId by lazy { intent.getLongExtra("postId", -1) }
    private val token by lazy { "Bearer ${App.prefs.token}" }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_detail)

        //조회
        loadPostDetail()
        loadComments()
        
        val inflater = LayoutInflater.from(this)
        val add = findViewById<ImageButton>(R.id.add)
        val writeReview = findViewById<EditText>(R.id.writeReview)
        val postBtn = findViewById<TextView>(R.id.post)
        val commentContainer = findViewById<LinearLayout>(R.id.commentContainer)
        val commentScroll = findViewById<NestedScrollView>(R.id.commentScroll)

        // 상단 메뉴(신고)
        add.setOnClickListener {
            val popup = PopupMenu(this, add)
            popup.menu.add("신고하기")
            popup.setOnMenuItemClickListener {
                Toast.makeText(this, "신고되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
                true
            }
            popup.show()
        }




        val goodButtons = findViewById<ImageView>(R.id.good)
        val likeCountView = findViewById<TextView>(R.id.goodNumber)

        goodButtons.setTag(R.id.good, false)
        goodButtons.setOnClickListener {

            val isLiked = it.getTag(R.id.good) as Boolean
            if (isLiked) {
                Toast.makeText(this, "이미 추천한 게시글입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버에 좋아요 요청
            RetrofitInstance.communityApi.likePost(token, postId)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            goodButtons.setImageResource(R.drawable.ic_good_fill)
                            loadPostDetail()
                            it.setTag(R.id.good, true)
                            Toast.makeText(this@CommunityDetailActivity, "해당 게시글을 추천했습니다.", Toast.LENGTH_SHORT).show()
                        } else if (response.code() == 400) {
                            Toast.makeText(this@CommunityDetailActivity, "이미 추천한 게시글입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CommunityDetailActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@CommunityDetailActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // 📝 댓글 작성 후 추가
        postBtn.setOnClickListener {
            val inputText = writeReview.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(this, "댓글을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            postComment(inputText)
        }
    }
    private fun loadPostDetail() {
        RetrofitInstance.communityApi.getPostDetail(token, postId)
            .enqueue(object : Callback<CommunityDetailResponse> {
                override fun onResponse(
                    call: Call<CommunityDetailResponse>,
                    response: Response<CommunityDetailResponse>
                ) {
                    Log.d("CommunityDetail", "✅ Response code: ${response.code()}")
                    if (response.isSuccessful) {
                        val post = response.body()!!
                        findViewById<TextView>(R.id.name).text = post.writer
                        findViewById<TextView>(R.id.content).text = post.content
                        findViewById<TextView>(R.id.goodNumber).text = post.likeCount.toString()
                        println("Like count  :"+post.likeCount.toString())
                        loadImages(post.imageUrls)

                        findViewById<EditText>(R.id.writeReview).hint = "${post.writer}님에게 답글 남기기"
                        val goodButton = findViewById<ImageView>(R.id.good)
                        if (post.liked) {
                            goodButton.setImageResource(R.drawable.ic_good_fill)
                            goodButton.setTag(R.id.good, true)
                        } else {
                            goodButton.setImageResource(R.drawable.ic_good)
                            goodButton.setTag(R.id.good, false)
                        }
                    }
                }

                override fun onFailure(call: Call<CommunityDetailResponse>, t: Throwable) {}
            })
    }
    private fun loadImages(imageUrls: List<String>) {
        val container = findViewById<LinearLayout>(R.id.imageContainer)
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (url in imageUrls) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(217.dp, 246.dp).apply {
                    rightMargin = 13.dp
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            Glide.with(this).load(RetrofitInstance.BASE_URL + url).into(imageView)
            container.addView(imageView)
        }
    }

    private fun loadComments() {
        val commentContainer = findViewById<LinearLayout>(R.id.commentAddContainer)
        val commentCountView = findViewById<TextView>(R.id.chatNumber)

        RetrofitInstance.communityApi.getCommentsWithCount(token, postId)
            .enqueue(object : Callback<CommentListResponse> {
                override fun onResponse(
                    call: Call<CommentListResponse>,
                    response: Response<CommentListResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body() ?: return
                        val inflater = LayoutInflater.from(this@CommunityDetailActivity)
                        commentContainer.removeAllViews()

                        // 총 댓글 수 표시
                        commentCountView.text = body.count.toString()

                        // 댓글 렌더링
                        body.comments.forEach { comment ->
                            val view = inflater.inflate(R.layout.item_comment, commentContainer, false)
                            view.findViewById<TextView>(R.id.nameFour).text = comment.user
                            view.findViewById<TextView>(R.id.contentFour).text = comment.content
                            view.findViewById<TextView>(R.id.timeFour).text = comment.createdAt.take(10).replace("-", ".")
                            commentContainer.addView(view)
                        }
                    }
                }

                override fun onFailure(call: Call<CommentListResponse>, t: Throwable) {}
            })
    }
    private fun postComment(content: String) {
        RetrofitInstance.communityApi.postComment(token, postId, CommentRequest(content))
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        findViewById<EditText>(R.id.writeReview).setText("")
                        loadComments()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
    }

    // 확장 함수
    val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
