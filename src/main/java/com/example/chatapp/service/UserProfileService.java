package com.example.chatapp.service;

import com.example.chatapp.entity.Friendship;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.FriendshipRepository;
import com.example.chatapp.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * ユーザーのプロフィールを取得または作成
     */
    public UserProfile getOrCreateProfile(User user) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUser(user);
        
        if (existingProfile.isPresent()) {
            return existingProfile.get();
        }
        
        // 新しいプロフィールを作成
        UserProfile newProfile = new UserProfile(user);
        return userProfileRepository.save(newProfile);
    }

    /**
     * プロフィールの更新
     */
    public UserProfile updateProfile(User user, UserProfile updatedProfile) {
        UserProfile existingProfile = getOrCreateProfile(user);
        
        // 更新可能なフィールドのみコピー
        existingProfile.setDisplayName(updatedProfile.getDisplayName());
        existingProfile.setBio(updatedProfile.getBio());
        existingProfile.setStatus(updatedProfile.getStatus());
        existingProfile.setLocation(updatedProfile.getLocation());
        existingProfile.setBirthDate(updatedProfile.getBirthDate());
        existingProfile.setPrivacyLevel(updatedProfile.getPrivacyLevel());
        existingProfile.setIsSearchable(updatedProfile.getIsSearchable());
        existingProfile.setAllowRandomMatching(updatedProfile.getAllowRandomMatching());
        
        // ランダムマッチング用フィールドを追加
        existingProfile.setAgeGroup(updatedProfile.getAgeGroup());
        existingProfile.setChatStyle(updatedProfile.getChatStyle());
        existingProfile.setInterests(updatedProfile.getInterests());
        existingProfile.setHobbies(updatedProfile.getHobbies());
        existingProfile.setFavoriteThings(updatedProfile.getFavoriteThings());
        existingProfile.setMusicGenres(updatedProfile.getMusicGenres());
        existingProfile.setMovieGenres(updatedProfile.getMovieGenres());
        existingProfile.setLanguages(updatedProfile.getLanguages());
        existingProfile.setPersonalityTraits(updatedProfile.getPersonalityTraits());
        
        return userProfileRepository.save(existingProfile);
    }

    /**
     * アバター画像のアップロード
     */
    public UserProfile uploadAvatar(User user, MultipartFile file) throws IOException {
        System.out.println("=== アバターアップロード開始 ===");
        System.out.println("ファイル名: " + file.getOriginalFilename());
        System.out.println("ファイルサイズ: " + file.getSize());
        System.out.println("コンテンツタイプ: " + file.getContentType());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("ファイルが選択されていません");
        }

        // ファイルサイズチェック
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("ファイルサイズが大きすぎます (最大5MB)");
        }

        // 拡張子チェック
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("ファイル名が不正です");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        System.out.println("ファイル拡張子: " + extension);
        
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new IllegalArgumentException("サポートされていないファイル形式です");
        }

        // アップロードディレクトリの作成
        Path uploadPath = Paths.get(UPLOAD_DIR);
        System.out.println("アップロードディレクトリ: " + uploadPath.toAbsolutePath());
        
        if (!Files.exists(uploadPath)) {
            System.out.println("ディレクトリを作成中...");
            Files.createDirectories(uploadPath);
        }

        // ユニークなファイル名を生成
        String filename = user.getId() + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);
        System.out.println("保存先ファイルパス: " + filePath.toAbsolutePath());

        // ファイルを保存
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("ファイル保存完了");

        // プロフィールのアバターURLを更新
        UserProfile profile = getOrCreateProfile(user);
        String avatarUrl = "/uploads/avatars/" + filename;
        profile.setAvatarUrl(avatarUrl);
        System.out.println("アバターURL設定: " + avatarUrl);
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        System.out.println("プロフィール保存完了");
        System.out.println("=== アバターアップロード終了 ===");
        
        return savedProfile;
    }

    /**
     * オンラインステータスの更新
     */
    public void updateOnlineStatus(User user, UserProfile.OnlineStatus status) {
        UserProfile profile = getOrCreateProfile(user);
        profile.setOnlineStatus(status);
        profile.updateLastSeen();
        userProfileRepository.save(profile);
    }

    /**
     * 最終アクセス時間の更新
     */
    public void updateLastSeen(User user) {
        UserProfile profile = getOrCreateProfile(user);
        profile.updateLastSeen();
        userProfileRepository.save(profile);
    }

    /**
     * キーワードでプロフィールを検索
     */
    @Transactional(readOnly = true)
    public List<UserProfile> searchProfiles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return userProfileRepository.searchByKeyword(keyword.trim());
    }

    /**
     * ランダムマッチング可能なユーザーを取得（フレンド、ブロックユーザー、申請中ユーザーを除外）
     */
    @Transactional(readOnly = true)
    public List<UserProfile> getAvailableForRandomMatching(User excludeUser) {
        // 基本的なランダムマッチング可能ユーザーを取得
        List<UserProfile> candidates = userProfileRepository.findAvailableForRandomMatching(excludeUser.getId());
        
        // フレンドリストを取得
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(excludeUser);
        List<Long> friendIds = friendships.stream()
            .map(friendship -> {
                // 自分がrequesterの場合はaddresseeを、addresseeの場合はrequesterを取得
                if (friendship.getRequester().getId().equals(excludeUser.getId())) {
                    return friendship.getAddressee().getId();
                } else {
                    return friendship.getRequester().getId();
                }
            })
            .collect(Collectors.toList());
        
        // ブロックしたユーザーリストを取得
        List<Friendship> blockedFriendships = friendshipRepository.findByRequesterAndStatus(
            excludeUser, Friendship.FriendshipStatus.BLOCKED);
        List<Long> blockedUserIds = blockedFriendships.stream()
            .map(friendship -> friendship.getAddressee().getId())
            .collect(Collectors.toList());
        
        // 送信済み申請のユーザーIDを取得
        List<Friendship> sentRequests = friendshipRepository.findSentRequests(excludeUser);
        List<Long> sentRequestUserIds = sentRequests.stream()
            .map(friendship -> friendship.getAddressee().getId())
            .collect(Collectors.toList());
        
        // 受信申請のユーザーIDを取得
        List<Friendship> receivedRequests = friendshipRepository.findPendingRequests(excludeUser);
        List<Long> receivedRequestUserIds = receivedRequests.stream()
            .map(friendship -> friendship.getRequester().getId())
            .collect(Collectors.toList());
        
        // デバッグ情報出力
        System.out.println("=== ランダムマッチング候補者フィルタリング ===");
        System.out.println("基本候補者数: " + candidates.size());
        System.out.println("除外 - フレンド: " + friendIds.size() + " 人");
        System.out.println("除外 - ブロック: " + blockedUserIds.size() + " 人");
        System.out.println("除外 - 送信申請: " + sentRequestUserIds.size() + " 人");
        System.out.println("除外 - 受信申請: " + receivedRequestUserIds.size() + " 人");
        
        // フレンド、ブロックユーザー、申請中ユーザーを除外
        List<UserProfile> filteredCandidates = candidates.stream()
            .filter(profile -> !friendIds.contains(profile.getUser().getId()))
            .filter(profile -> !blockedUserIds.contains(profile.getUser().getId()))
            .filter(profile -> !sentRequestUserIds.contains(profile.getUser().getId()))
            .filter(profile -> !receivedRequestUserIds.contains(profile.getUser().getId()))
            .collect(Collectors.toList());
        
        System.out.println("最終候補者数: " + filteredCandidates.size() + " 人");
        System.out.println("===========================================");
        
        return filteredCandidates;
    }

    /**
     * オンラインユーザー一覧を取得
     */
    @Transactional(readOnly = true)
    public List<UserProfile> getOnlineUsers() {
        return userProfileRepository.findOnlineUsers();
    }

    /**
     * オンラインユーザー数を取得
     */
    @Transactional(readOnly = true)
    public long getOnlineUserCount() {
        return userProfileRepository.countOnlineUsers();
    }

    /**
     * プロフィールをIDで取得
     */
    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfileById(Long profileId) {
        return userProfileRepository.findById(profileId);
    }

    /**
     * ユーザーIDでプロフィールを取得
     */
    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfileByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    /**
     * 複数ユーザーのプロフィールを取得
     */
    @Transactional(readOnly = true)
    public List<UserProfile> getProfilesByUserIds(List<Long> userIds) {
        return userProfileRepository.findByUserIds(userIds);
    }

    /**
     * プロフィールの削除
     */
    public void deleteProfile(User user) {
        Optional<UserProfile> profile = userProfileRepository.findByUser(user);
        if (profile.isPresent()) {
            // アバター画像ファイルも削除
            deleteAvatarFile(profile.get());
            userProfileRepository.delete(profile.get());
        }
    }

    /**
     * アバター画像ファイルの削除
     */
    private void deleteAvatarFile(UserProfile profile) {
        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl != null && avatarUrl.startsWith("/uploads/avatars/")) {
            try {
                String filename = avatarUrl.substring("/uploads/avatars/".length());
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // ログに記録するが、エラーは無視
                System.err.println("アバター画像の削除に失敗: " + e.getMessage());
            }
        }
    }

    /**
     * ファイル拡張子を取得
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * プロフィールの表示可否チェック
     */
    public boolean canViewProfile(UserProfile profile, User viewer) {
        if (profile == null || viewer == null) {
            return false;
        }

        // 自分のプロフィールは常に表示可能
        if (profile.getUser().equals(viewer)) {
            return true;
        }

        // プライバシー設定に基づく判定
        switch (profile.getPrivacyLevel()) {
            case PUBLIC:
                return true;
            case FRIENDS_ONLY:
                // フレンド判定は別のサービスで実装
                return true; // 一時的にtrue
            case PRIVATE:
                return false;
            default:
                return false;
        }
    }
}
