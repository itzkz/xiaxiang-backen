package com.itzkz.usercenter;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itzkz.usercenter.model.domain.Team;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.service.UserService;
import com.itzkz.usercenter.tools.GenerateRecommendations;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RList;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


@SpringBootTest
class UserCenterApplicationTests {
    @Resource
    private UserService userService;

    @Resource
    private Redisson redisson;
    @Resource
    private RedisTemplate redisTemplate;


    @Test
    void contextLoads() {
        String userAccount = "yupi";
        String userPassword = "";
        String checkPassword = "123456";
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        userAccount = "yu";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        userAccount = "yupi";
        userPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        userAccount = "yu pi";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        userAccount = "dogyupi";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);

        userAccount = "yupi";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertTrue(result > 0);
    }

    @Test
    public void searchUserByTags() {

        List<String> asList = Arrays.asList("Java", "Python");
        List<User> userList = userService.searchUserByTags(asList);
        System.out.println(userList);
        Assert.notEmpty(userList);
    }
//
//    @Test
//    public void testInsertFakeDate(){
//
//        ArrayList<User> userArrayList = new ArrayList<>();
//
//        for (int i = 0; i <100000 ; i++) {
//            User user = new User();
//            user.setUsername("fake");
//            user.setUseraccount("itzkz");
//            user.setAvatarurl("a");
//            user.setGender(0);
//            user.setUserpassword("123456");
//            user.setEmail("qqq");
//            user.setUserstatus(0);
//            user.setProfile("123321");
//            user.setPhone("123321");
//            user.setTags("男");
//            user.setUserrole(0);
//            user.setIsdelete(0);
//            userArrayList.add(user);
//        }
//
//        userService.saveBatch(userArrayList,10000);
//
//    }

    /**
     * 批量插入用户
     */
//    @Test
//    public void doInsertUser() {
//        //用StopWatch计时，看插入1000条数据要多久
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        final int INSERT_NUM = 1000;
//        for (int i = 0; i < INSERT_NUM; i++) {
//            User user = new User();
//            user.setUsername("假用户");
//            user.setUseraccount("fakeRokie");
//            user.setAvatarurl("");
//                    user.setGender(0);
//            user.setUserpassword("12345678");
//            user.setPhone("132132132");
//            user.setEmail("13213132@qq.com");
//            user.setUserrole(0);
//            user.setUserstatus(0);
//            userService.save(user);
//        }
//        stopWatch.stop();
//        System.out.println(stopWatch.getTotalTimeMillis());
//    }


    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUser() {
        //用StopWatch计时，看插入1000条数据要多久
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new LinkedList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("假用户");
            user.setUseraccount("fakeRokie");
            user.setAvatarurl("");
            user.setGender(0);
            user.setUserpassword("12345678");
            user.setPhone("132132132");
            user.setEmail("13213132@qq.com");
            user.setUserstatus(0);
            user.setUserrole(0);
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }


    private ExecutorService executorService = new ThreadPoolExecutor
            (40, 1000, 10000,
                    TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
//分10组
        int batchSize = 50000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
                User user = new User();
                user.setUsername("假用户");
                user.setUseraccount("fakeRokie");
                user.setAvatarurl("");
                user.setGender(0);
                user.setUserpassword("12345678");
                user.setPhone("132132132");
                user.setEmail("13213132@qq.com");
                user.setUserstatus(0);
                user.setUserrole(0);
                userList.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName:" + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }


    /**
     * 使用redis进行crud
     */
    @Test
    public void testRedisCrud() {
    /*
      增
     */
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set("name", "zkz");
        valueOperations.set("age", 20);
        valueOperations.set("tall", 173.1);
        User user = new User();
        user.setId(10L);
        user.setProfile("sing,rap, basketball");
        valueOperations.set("user", user);

    /*
      查
     */

        Object name = valueOperations.get("name");
        Object age = valueOperations.get("age");
        Object tall = valueOperations.get("tall");
        Object user1 = valueOperations.get("user");


        Assert.equals(name, "zkz");
        Assert.equals(age, 20);
        Assert.equals(tall, 173.1);
        System.out.println(user1);


    }


    @Test
    public void testRedisson() {

        RList<Object> list = redisson.getList("xiaxiang:redisson:key");

//        list.add("zkz");
        System.out.println(list.get(0));
        list.remove(0);
    }


    @Test
    public void testGenerateRecommendations() {
        // 创建模拟用户数据
//        List<User> users = new ArrayList<>();
        List<Integer> teamIdList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getId,teamIdList);
        queryWrapper.select(User::getId,User::getTags);
        List<User> users = userService.list(queryWrapper);
//        // 用户1
//        User user1 = new User();
//        user1.setId(1L);
//        user1.setTags("Java, xxxx, Hibernate");
//        users.add(user1);
//
//        // 用户2
//        User user2 = new User();
//        user2.setId(2L);
//        user2.setTags("Python, Django, Flask");
//        users.add(user2);
//
//        // 用户3
//        User user3 = new User();
//        user3.setId(3L);
//        user3.setTags("Java, Spring, Maven");
//        users.add(user3);
//
//        // 用户4
//        User user4 = new User();
//        user4.setId(4L);
//        user4.setTags("JavaScript, React, Node.js");
//        users.add(user4);
//
//        // 用户5
//        User user5 = new User();
//        user5.setId(5L);
//        user5.setTags("C++, OpenGL, Unity");
//        users.add(user5);
//
//        // 用户6
//        User user6 = new User();
//        user6.setId(6L);
//        user6.setTags("Python, Data Science, Machine Learning");
//        users.add(user6);

        // 创建目标用户
        User targetUser =userService.getById(3);
      // 假设目标用户ID为3


        // 生成推荐列表
        GenerateRecommendations generateRecommendations = new GenerateRecommendations();
        List<User> userList = generateRecommendations.generateRecommendations(targetUser,users,5);
        // 输出推荐列表
        for (User user : userList) {
            System.out.println("用户 " + user.getId());
        }
    }






}







