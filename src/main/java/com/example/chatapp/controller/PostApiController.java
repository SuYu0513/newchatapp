package com.example.chatapp.controller;

import com.example.chatapp.entity.Post;
import com.example.chatapp.entity.User;
import com.example.chatapp.service.FriendshipService;
import com.example.chatapp.service.PostService;
import com.example.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/posts")
public class PostApiController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private FriendshipService friendshipService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    /**
     * 全ての投稿を取得
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPosts() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Post> posts = postService.getAllPosts();
            List<Map<String, Object>> postList = convertPostsToList(posts);

            response.put("success", true);
            response.put("posts", postList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 自分の投稿を取得
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyPosts(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("success", false);
                response.put("message", "認証が必要です");
                return ResponseEntity.status(401).body(response);
            }

            String username = principal.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            List<Post> posts = postService.getPostsByUser(currentUser);
            List<Map<String, Object>> postList = convertPostsToList(posts);

            response.put("success", true);
            response.put("posts", postList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * フォロー中のユーザーの投稿を取得
     */
    @GetMapping("/following")
    public ResponseEntity<Map<String, Object>> getFollowingPosts(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("success", false);
                response.put("message", "認証が必要です");
                return ResponseEntity.status(401).body(response);
            }

            String username = principal.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            List<User> following = friendshipService.getFollowing(currentUser);
            List<Post> posts = postService.getPostsByUsers(following);
            List<Map<String, Object>> postList = convertPostsToList(posts);

            response.put("success", true);
            response.put("posts", postList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 特定ユーザーの投稿を取得
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getPostsByUserId(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            List<Post> posts = postService.getPostsByUser(user);
            List<Map<String, Object>> postList = convertPostsToList(posts);

            response.put("success", true);
            response.put("posts", postList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private List<Map<String, Object>> convertPostsToList(List<Post> posts) {
        List<Map<String, Object>> postList = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("content", post.getContent());
            postMap.put("mediaPath", post.getMediaPath());
            postMap.put("createdAt", post.getCreatedAt().format(DATE_FORMATTER));

            User user = post.getUser();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("friendCode", user.getFriendCode());

            if (user.getProfile() != null) {
                userMap.put("displayName", user.getProfile().getDisplayName());
                userMap.put("avatarUrl", user.getProfile().getAvatarUrl());
            } else {
                userMap.put("displayName", user.getUsername());
                userMap.put("avatarUrl", null);
            }

            postMap.put("user", userMap);
            postList.add(postMap);
        }
        return postList;
    }
}
