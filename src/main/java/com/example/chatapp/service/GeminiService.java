package com.example.chatapp.service;

import com.example.chatapp.entity.AiPersona;
import com.example.chatapp.entity.User;
import com.example.chatapp.entity.UserProfile;
import com.example.chatapp.repository.AiPersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Autowired
    private AiPersonaRepository aiPersonaRepository;

    private static final String GEMINI_API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();

    // ユーザーごとの会話履歴を保持（セッション単位）
    private final Map<Long, List<Map<String, Object>>> conversationHistory = new ConcurrentHashMap<>();

    /**
     * APIキーが設定されているかチェック
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * ペルソナに基づいたシステムプロンプトを生成
     */
    private String buildSystemPrompt(AiPersona persona, String userName) {
        if (persona == null) {
            return "あなたは親しみやすいAIアシスタントです。日本語で会話してください。";
        }

        StringBuilder prompt = new StringBuilder();
        String name = persona.getAiName() != null ? persona.getAiName() : "AI";

        prompt.append("あなたは「").append(name).append("」という名前のキャラクターです。\n");
        prompt.append("以下の設定に従って、ユーザーと会話してください。\n\n");

        // ユーザーの名前
        if (userName != null && !userName.isEmpty()) {
            prompt.append("【相手の名前】").append(userName).append("\n");
            prompt.append("相手の名前を呼ぶときは「").append(userName).append("」と呼んでください。\n\n");
        }

        // 関係性
        switch (persona.getRelationship() != null ? persona.getRelationship() : "") {
            case "friend" -> prompt.append("【関係性】ユーザーの親しい友達として接してください。\n");
            case "lover" -> prompt.append("【関係性】ユーザーの恋人として、愛情を込めて接してください。\n");
            case "senior" -> prompt.append("【関係性】ユーザーの頼れる先輩として、時に優しく時に厳しく接してください。\n");
            case "junior" -> prompt.append("【関係性】ユーザーの可愛い後輩として、尊敬の念を持って接してください。\n");
            case "teacher" -> prompt.append("【関係性】ユーザーの先生・メンターとして、導くように接してください。\n");
            case "family" -> prompt.append("【関係性】ユーザーの家族のように、温かく見守ってください。\n");
        }

        // 性別
        switch (persona.getGender() != null ? persona.getGender() : "") {
            case "male" -> prompt.append("【性別】男性として振る舞ってください。\n");
            case "female" -> prompt.append("【性別】女性として振る舞ってください。\n");
            case "neutral" -> prompt.append("【性別】性別を特定しない中性的なキャラクターとして振る舞ってください。\n");
        }

        // 年齢層
        switch (persona.getAgeGroup() != null ? persona.getAgeGroup() : "") {
            case "teen" -> prompt.append("【年齢】10代の若者らしいフレッシュな雰囲気で。\n");
            case "twenties" -> prompt.append("【年齢】20代の若々しさと落ち着きのバランスで。\n");
            case "thirties" -> prompt.append("【年齢】30代の大人の余裕を持って。\n");
            case "forties" -> prompt.append("【年齢】40代以上の包容力と経験を感じさせて。\n");
        }

        // 性格
        switch (persona.getPersonality() != null ? persona.getPersonality() : "") {
            case "cheerful" -> prompt.append("【性格】明るく元気で、ポジティブな性格。\n");
            case "calm" -> prompt.append("【性格】落ち着いていて、穏やかな性格。\n");
            case "cool" -> prompt.append("【性格】クールでかっこいい、少しミステリアスな性格。\n");
            case "tsundere" -> prompt.append("【性格】ツンデレ。普段は素っ気ないけど、時々優しさが垣間見える。\n");
            case "gentle" -> prompt.append("【性格】優しくて思いやりがあり、包み込むような性格。\n");
            case "playful" -> prompt.append("【性格】おちゃめでユーモアがあり、冗談も言う。\n");
        }

        // 口調
        switch (persona.getSpeakingStyle() != null ? persona.getSpeakingStyle() : "") {
            case "casual" -> prompt.append("【口調】カジュアルでフランクな話し方。タメ口OK。\n");
            case "polite" -> prompt.append("【口調】丁寧語を基本としつつ、親しみやすさも。\n");
            case "formal" -> prompt.append("【口調】敬語を使った礼儀正しい話し方。\n");
            case "dialect" -> prompt.append("【口調】関西弁など方言を混ぜた親しみやすい話し方。\n");
        }

        prompt.append("\n【重要】\n");
        prompt.append("- 自然な会話を心がけ、ユーザーの気持ちに寄り添ってください。\n");
        prompt.append("- 長すぎない返答を心がけてください（2-4文程度）。\n");
        prompt.append("- 設定したキャラクターを一貫して演じてください。\n");
        prompt.append("- 相手の話をよく聞き、共感を示してください。\n");

        return prompt.toString();
    }

    /**
     * Gemini APIにメッセージを送信して返答を取得
     */
    public String chat(User user, String userMessage) {
        if (!isConfigured()) {
            return "AIチャットを利用するには、環境変数 GEMINI_API_KEY を設定してください。";
        }

        Long userId = user.getId();

        // ペルソナを取得
        AiPersona persona = aiPersonaRepository.findByUser(user).orElse(null);

        // ユーザーの表示名を取得
        String userName = null;
        UserProfile profile = user.getProfile();
        if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isEmpty()) {
            userName = profile.getDisplayName();
        } else {
            userName = user.getUsername();
        }

        // ユーザーの会話履歴を取得または初期化
        List<Map<String, Object>> history = conversationHistory.computeIfAbsent(userId, k -> new ArrayList<>());

        // 最初のメッセージの場合、システムプロンプトを追加
        if (history.isEmpty() && persona != null && persona.isSetupCompleted()) {
            String systemPrompt = buildSystemPrompt(persona, userName);
            Map<String, Object> systemContent = new HashMap<>();
            systemContent.put("role", "user");
            systemContent.put("parts", List.of(Map.of("text", "【システム指示】" + systemPrompt + "\n\n以下から会話を始めてください。最初の挨拶として、自己紹介をお願いします。")));
            history.add(systemContent);

            // AIの自己紹介を先に取得
            try {
                String intro = callGeminiApi(history);
                Map<String, Object> modelContent = new HashMap<>();
                modelContent.put("role", "model");
                modelContent.put("parts", List.of(Map.of("text", intro)));
                history.add(modelContent);
            } catch (Exception e) {
                // 失敗した場合はシステムプロンプトを削除
                history.clear();
            }
        }

        // ユーザーメッセージを履歴に追加
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("role", "user");
        userContent.put("parts", List.of(Map.of("text", userMessage)));
        history.add(userContent);

        try {
            String reply = callGeminiApi(history);

            // AIの返答を履歴に追加
            Map<String, Object> modelContent = new HashMap<>();
            modelContent.put("role", "model");
            modelContent.put("parts", List.of(Map.of("text", reply)));
            history.add(modelContent);

            // 履歴が長くなりすぎたら古いものを削除（最新20件を保持、ただしシステムプロンプトは保持）
            while (history.size() > 22) {
                history.remove(2); // 最初の2つ（システムプロンプトと自己紹介）は保持
            }

            return reply;

        } catch (Exception e) {
            // エラー時は履歴からユーザーメッセージを削除
            if (!history.isEmpty()) {
                history.remove(history.size() - 1);
            }

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("401")) {
                return "APIキーが無効です。正しいGemini APIキーを設定してください。";
            } else if (errorMessage != null && errorMessage.contains("429")) {
                return "APIの利用制限に達しました。しばらく待ってから再度お試しください。";
            }
            return "エラーが発生しました: " + (errorMessage != null ? errorMessage : "不明なエラー");
        }
    }

    /**
     * Gemini APIを呼び出す
     */
    private String callGeminiApi(List<Map<String, Object>> history) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", history);
        requestBody.put("generationConfig", Map.of(
            "temperature", 0.8,
            "maxOutputTokens", 512
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_API_URL + "?key=" + apiKey;
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, (Class<Map<String, Object>>)(Class<?>)Map.class);

        return extractReplyText(response.getBody());
    }

    /**
     * APIレスポンスからテキストを抽出
     */
    @SuppressWarnings("unchecked")
    private String extractReplyText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "返答を取得できませんでした。";
        }

        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            // パース失敗
        }

        return "返答を解析できませんでした。";
    }

    /**
     * ユーザーの会話履歴をクリア
     */
    public void clearHistory(Long userId) {
        conversationHistory.remove(userId);
    }

    /**
     * ユーザーの会話履歴を取得（表示用）
     */
    public List<Map<String, String>> getHistory(Long userId) {
        List<Map<String, Object>> history = conversationHistory.get(userId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> item = history.get(i);
            String role = (String) item.get("role");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) item.get("parts");
            if (parts != null && !parts.isEmpty()) {
                String text = (String) parts.get(0).get("text");

                // システムプロンプトはスキップ
                if (text != null && text.startsWith("【システム指示】")) {
                    continue;
                }

                result.add(Map.of(
                    "role", "user".equals(role) ? "user" : "ai",
                    "text", text != null ? text : ""
                ));
            }
        }
        return result;
    }

    /**
     * 初回挨拶メッセージを生成（履歴には追加しない）
     */
    public String generateGreeting(User user) {
        if (!isConfigured()) {
            return "APIキーが設定されていません。";
        }

        AiPersona persona = aiPersonaRepository.findByUser(user).orElse(null);
        // 挨拶時は名前を使わないのでnullを渡す
        String systemPrompt = buildSystemPrompt(persona, null);

        // 挨拶用のプロンプト（ユーザー名を使わない指示）
        String greetingPrompt = systemPrompt +
            "\n\n【初回挨拶】\n" +
            "今から初めて会話を始めます。\n" +
            "相手の名前は知らないので、名前を呼ばずに自然な挨拶をしてください。\n" +
            "「こんにちは！」や「やあ！」のように、親しみやすく短い挨拶をお願いします。";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
            Map.of("role", "user", "parts", List.of(Map.of("text", greetingPrompt)))
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = GEMINI_API_URL + "?key=" + apiKey;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity, (Class<Map<String, Object>>)(Class<?>)Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        String reply = (String) parts.get(0).get("text");
                        return reply != null ? reply : "こんにちは！";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Gemini API 挨拶生成エラー: " + e.getMessage());
        }

        return "こんにちは！よろしくね！";
    }
}
