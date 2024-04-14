package com.itzkz.usercenter.tools;

import com.itzkz.usercenter.model.domain.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 利用余弦相似度算法去实现生成相似的用户列表
 */
public class GenerateRecommendations {

    // 计算余弦相似度
    private double cosineSimilarity(User user1, User user2) {
        List<String> tags1 = extractTags(user1.getTags());
        List<String> tags2 = extractTags(user2.getTags());
        List<String> intersection = new ArrayList<>(tags1);
        intersection.retainAll(tags2);

        double dotProduct = 0;
        double magnitudeUser1 = 0;
        double magnitudeUser2 = 0;

        for (String tag : intersection) {
            dotProduct += tags1.contains(tag) && tags2.contains(tag) ? 1 : 0;
        }

        for (String tag : tags1) {
            magnitudeUser1 += Math.pow(tags1.contains(tag) ? 1 : 0, 2);
        }

        for (String tag : tags2) {
            magnitudeUser2 += Math.pow(tags2.contains(tag) ? 1 : 0, 2);
        }

        if (magnitudeUser1 == 0 || magnitudeUser2 == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(magnitudeUser1) * Math.sqrt(magnitudeUser2));
    }

    // 从标签字符串中提取标签列表
    private List<String> extractTags(String tags) {
        List<String> tagList = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                tagList.add(tag.trim());
            }
        }
        return tagList;
    }

    // 生成推荐列表
    // 生成推荐列表

    public List<User> generateRecommendations(User targetUser, List<User> users, int numOfRecommendations) {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // 用于存储用户相似度的并发映射
        ConcurrentHashMap<Long, Double> similarityMap = new ConcurrentHashMap<>();

        // 提交任务
        for (User user : users) {
            if (user.getTags() != null && !user.getId().equals(targetUser.getId())) {
                executorService.submit(() -> {
                    double similarity = cosineSimilarity(targetUser, user);
                    similarityMap.put(user.getId(), similarity);
                });
            }
        }

        // 关闭线程池
        executorService.shutdown();

        // 等待所有任务完成
        while (!executorService.isTerminated()) {
            // 等待
        }

        // 对相似度进行排序
        List<UserSimilarity> similarityList = new ArrayList<>();
        for (Long userId : similarityMap.keySet()) {
            similarityList.add(new UserSimilarity(userId, similarityMap.get(userId)));
        }
        Collections.sort(similarityList);

        // 提取前numOfRecommendations个相似度最高的用户
        List<User> recommendations = new ArrayList<>();
        int count = 0;
        for (UserSimilarity userSimilarity : similarityList) {
            if (count >= numOfRecommendations) {
                break;
            }
            long userId = userSimilarity.getUserId();
            for (User user : users) {
                if (user.getId().equals(userId)) {
                    recommendations.add(user);
                    count++;
                    break;
                }
            }
        }

        return recommendations;
    }

    public  class UserSimilarity implements Comparable<UserSimilarity> {
        private final long userId;
        private final double similarity;

        public UserSimilarity(long userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }

        public long getUserId() {
            return userId;
        }

        public double getSimilarity() {
            return similarity;
        }

        @Override
        public int compareTo(UserSimilarity o) {
            // 降序排序
            return Double.compare(o.similarity, this.similarity);
        }
    }


}
