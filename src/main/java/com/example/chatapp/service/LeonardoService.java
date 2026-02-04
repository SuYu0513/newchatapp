package com.example.chatapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LeonardoService {

    @Value("${leonardo.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String LEONARDO_API_URL = "https://cloud.leonardo.ai/api/rest/v1";

    /**
     * APIキーが設定されているかチェック
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * ペルソナ設定に基づいてアバター画像を生成
     */
    public String generateAvatar(String gender, String ageGroup, String personality) {
        if (!isConfigured()) {
            return null;
        }

        // プロンプトを生成
        String prompt = buildPrompt(gender, ageGroup, personality);

        try {
            // 画像生成リクエスト
            String generationId = createGeneration(prompt);
            if (generationId == null) {
                return null;
            }

            // 生成完了を待って画像URLを取得
            return waitForGenerationAndGetUrl(generationId);

        } catch (Exception e) {
            System.err.println("Leonardo AI エラー: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ペルソナ設定からプロンプトを生成
     */
    private String buildPrompt(String gender, String ageGroup, String personality) {
        StringBuilder prompt = new StringBuilder();

        // 基本スタイル
        prompt.append("anime style portrait, high quality, detailed, ");

        // 性別
        switch (gender != null ? gender : "") {
            case "male" -> prompt.append("young man, handsome, ");
            case "female" -> prompt.append("young woman, beautiful, cute, ");
            default -> prompt.append("androgynous person, ");
        }

        // 年齢層
        switch (ageGroup != null ? ageGroup : "") {
            case "teen" -> prompt.append("teenager, youthful, ");
            case "twenties" -> prompt.append("in their 20s, ");
            case "thirties" -> prompt.append("mature, in their 30s, ");
            case "forties" -> prompt.append("mature adult, elegant, ");
        }

        // 性格に基づく表情・雰囲気
        switch (personality != null ? personality : "") {
            case "cheerful" -> prompt.append("bright smile, happy expression, warm colors, ");
            case "calm" -> prompt.append("gentle smile, serene expression, soft colors, ");
            case "cool" -> prompt.append("confident smirk, cool expression, blue tones, ");
            case "tsundere" -> prompt.append("slight blush, looking away, pink accents, ");
            case "gentle" -> prompt.append("kind eyes, warm smile, soft lighting, ");
            case "playful" -> prompt.append("playful wink, mischievous smile, vibrant colors, ");
        }

        // 品質指定
        prompt.append("bust shot, facing viewer, simple background, soft lighting, masterpiece, best quality");

        return prompt.toString();
    }

    /**
     * 画像生成リクエストを送信
     */
    @SuppressWarnings("unchecked")
    private String createGeneration(String prompt) {
        String url = LEONARDO_API_URL + "/generations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("modelId", "6b645e3a-d64f-4341-a6d8-7a3690fbf042"); // Leonardo Anime XL
        requestBody.put("width", 512);
        requestBody.put("height", 512);
        requestBody.put("num_images", 4); // 4枚生成
        requestBody.put("guidance_scale", 7);
        requestBody.put("negative_prompt", "bad quality, blurry, distorted, ugly, deformed, text, watermark, signature, multiple people, full body");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                (Class<Map<String, Object>>)(Class<?>)Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("sdGenerationJob")) {
                Map<String, Object> job = (Map<String, Object>) body.get("sdGenerationJob");
                return (String) job.get("generationId");
            }
        } catch (Exception e) {
            System.err.println("Leonardo API 生成リクエストエラー: " + e.getMessage());
        }

        return null;
    }

    /**
     * 生成完了を待って画像URLを取得
     */
    @SuppressWarnings("unchecked")
    private String waitForGenerationAndGetUrl(String generationId) throws InterruptedException {
        String url = LEONARDO_API_URL + "/generations/" + generationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 最大30秒待機
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000); // 1秒待機

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    (Class<Map<String, Object>>)(Class<?>)Map.class
                );

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("generations_by_pk")) {
                    Map<String, Object> generation = (Map<String, Object>) body.get("generations_by_pk");
                    String status = (String) generation.get("status");

                    if ("COMPLETE".equals(status)) {
                        List<Map<String, Object>> images = (List<Map<String, Object>>) generation.get("generated_images");
                        if (images != null && !images.isEmpty()) {
                            // 最初の画像URLを返す
                            return (String) images.get(0).get("url");
                        }
                    } else if ("FAILED".equals(status)) {
                        System.err.println("画像生成失敗");
                        return null;
                    }
                }
            } catch (Exception e) {
                System.err.println("Leonardo API ステータス確認エラー: " + e.getMessage());
            }
        }

        System.err.println("画像生成タイムアウト");
        return null;
    }

    /**
     * 複数の画像URLを取得（選択用）
     */
    public List<String> generateAvatars(String gender, String ageGroup, String personality) {
        if (!isConfigured()) {
            return List.of();
        }

        String prompt = buildPrompt(gender, ageGroup, personality);

        try {
            String generationId = createGeneration(prompt);
            if (generationId == null) {
                return List.of();
            }

            return waitForGenerationAndGetUrls(generationId);

        } catch (Exception e) {
            System.err.println("Leonardo AI エラー: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 生成完了を待って全画像URLを取得
     */
    @SuppressWarnings("unchecked")
    private List<String> waitForGenerationAndGetUrls(String generationId) throws InterruptedException {
        String url = LEONARDO_API_URL + "/generations/" + generationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 最大60秒待機
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    (Class<Map<String, Object>>)(Class<?>)Map.class
                );

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("generations_by_pk")) {
                    Map<String, Object> generation = (Map<String, Object>) body.get("generations_by_pk");
                    String status = (String) generation.get("status");

                    if ("COMPLETE".equals(status)) {
                        List<Map<String, Object>> images = (List<Map<String, Object>>) generation.get("generated_images");
                        if (images != null && !images.isEmpty()) {
                            List<String> urls = new ArrayList<>();
                            for (Map<String, Object> image : images) {
                                urls.add((String) image.get("url"));
                            }
                            return urls;
                        }
                    } else if ("FAILED".equals(status)) {
                        System.err.println("画像生成失敗");
                        return List.of();
                    }
                }
            } catch (Exception e) {
                System.err.println("Leonardo API ステータス確認エラー: " + e.getMessage());
            }
        }

        System.err.println("画像生成タイムアウト");
        return List.of();
    }
}
