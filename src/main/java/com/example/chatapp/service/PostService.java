package com.example.chatapp.service;

import com.example.chatapp.entity.Post;
import com.example.chatapp.entity.User;
import com.example.chatapp.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Transactional
    public Post createPost(User user, String content) {
        Post post = new Post(user, content);
        return postRepository.save(post);
    }
    
    @Transactional
    public Post createPost(User user, String content, String mediaPath) {
        Post post = new Post(user, content);
        post.setMediaPath(mediaPath);
        return postRepository.save(post);
    }
    
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }
}
