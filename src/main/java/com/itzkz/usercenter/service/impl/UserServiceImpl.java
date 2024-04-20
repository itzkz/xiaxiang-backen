package com.itzkz.usercenter.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itzkz.usercenter.common.ErrorCode;
import com.itzkz.usercenter.constant.UserConstant;
import com.itzkz.usercenter.exception.BusinessException;
import com.itzkz.usercenter.mapper.UserMapper;
import com.itzkz.usercenter.model.domain.Follow;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.vo.IsFollowVO;
import com.itzkz.usercenter.service.FollowService;
import com.itzkz.usercenter.service.UserService;
import com.itzkz.usercenter.tools.GenerateRecommendations;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    private static final int MAX_ATTEMPTS = 5; // 最大失败次数
    private static final int ATTEMPT_WINDOW_MINUTES = 10; // 时间窗口，单位：分钟
    private static final String REDIS_KEY_PREFIX = "login_attempt:";
    @Resource
    private FollowService followService;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }

        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码错误");
        }
        String illegalString = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern pattern = Pattern.compile(illegalString);
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "包含非法字符");
        }

        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUseraccount, userAccount);
        long count = this.count(userLambdaQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }


        //2.加密
        String encryptPassword = SecureUtil.md5(userPassword);


        //3.插入数据
        User user = new User();
        user.setUseraccount(userAccount);
        user.setUserpassword(encryptPassword);
        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      请求
     * @return 脱敏用户
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {


        //1.校验

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于四位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于八位");

        }

        String illegalString = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern pattern = Pattern.compile(illegalString);
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "包含非法字符");
        }
        // 首先检查登录是否被允许
        if (!isLoginAllowed(userAccount)) {
            throw new BusinessException("登录次数超过限制，请稍后重试", 200, "");
        }
        //2.加密
        String encryptPassword = SecureUtil.md5(userPassword);

        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUseraccount, userAccount);
        User user = this.getOne(userLambdaQueryWrapper);
        //校验账号是否存在
        if (user == null) {
            throw new BusinessException("账号不存在,请注册", 200, "");
        }
        //如果存在 判断密码是否相等
        if (!Objects.equals(encryptPassword, user.getUserpassword())) {
            //记录登录失败次数
            recordLoginAttempt(userAccount);
            throw new BusinessException("密码错误,请重试", 200, "");
        }

        //3.脱敏
        User safaUser = safaUser(user);
        //4.session
        request.getSession().setAttribute(UserConstant.SESSION_KEY, safaUser);
        return safaUser;
    }

    /**
     * 检查给定用户名的登录是否被允许。
     *
     * @param username 要检查的用户名。
     * @return 如果登录被允许，则返回 true；否则返回 false。
     */
    public boolean isLoginAllowed(String username) {
        String key = REDIS_KEY_PREFIX + username;
        long now = System.currentTimeMillis();
        long windowEnd = now - ATTEMPT_WINDOW_MINUTES * 60 * 1000; // 时间窗口的结束时间

        // 获取在时间窗口内的登录失败次数
        Long attempts = redisTemplate.opsForZSet().count(key, windowEnd, now);

        // 如果登录失败次数超过限制，则拒绝登录
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            return false;
        }

        return true;
    }

    /**
     * 记录登录失败的次数。
     *
     * @param username 登录失败的用户名。
     */
    public void recordLoginAttempt(String username) {
        String key = REDIS_KEY_PREFIX + username;
        long now = System.currentTimeMillis();

        // 记录登录失败的时间戳
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        // 设置过期时间，确保记录的时间窗口不会一直增长
        redisTemplate.expire(key, ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 整形
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "");
        }
        request.getSession().removeAttribute(UserConstant.SESSION_KEY);
        return 1;
    }

    /**
     * 用户脱敏
     *
     * @param user 用户
     * @return 脱敏用户
     */
    @Override
    public User safaUser(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        //脱敏
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setUseraccount(user.getUseraccount());
        safeUser.setAvatarurl(user.getAvatarurl());
        safeUser.setGender(user.getGender());
        safeUser.setEmail(user.getEmail());
        safeUser.setUserstatus(user.getUserstatus());
        safeUser.setPhone(user.getPhone());
        safeUser.setCreatetime(user.getCreatetime());
        safeUser.setUserrole(user.getUserrole());
        safeUser.setProfile(user.getProfile());
        safeUser.setTags(user.getTags());
        return safeUser;
    }

    /**
     * 根据用户标签列表查询对应用户列表
     *
     * @param tagNameList 标签列表
     * @return 用户列表
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {


//            if (CollectionUtils.isEmpty(tagNameList)){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR);
//            }


        //1.直接去数据库查询
//            if (tagNameList == null || tagNameList.isEmpty()) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR);
//            }
//
//            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();//拼接and查询
//
//            for (String tagName : tagNameList) {
//                queryWrapper.like(User::getTags, tagName);
//            }
//
//            List<User> userList = this.list(queryWrapper);
//            return userList.stream().map(this::safaUser).collect(Collectors.toList());

        //2.先把用户数据拿出来再进行判断查询
        //1.先查询所有用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.last("limit 100"); // 限制查询结果最多返回100条记录
        List<User> userList = this.list(queryWrapper);
        Gson gson = new Gson();
        //2.在内存中判断是否包含要求的标签
        return userList.stream().filter(user -> {
            //先获取用户的标签
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) {
                //如果标签为空
                return false;
            }
            //json转为string
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new
                    TypeToken<Set<String>>() {
                    }.getType());
            //tempTagNameSet 为空则返回空的hashset对象 不空则返回其值   上面已有空判断
            //tempTagNameSet =Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::safaUser).collect(Collectors.toList());
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        User userObj = (User) request.getSession().getAttribute(UserConstant.SESSION_KEY);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return userObj;
    }

    /**
     * 更新用户
     *
     * @param user      被修改用户
     * @param loginUser 登录用户
     * @return 1
     */
    @Override
    public boolean updateUser(User user, User loginUser) {
        //1 判断参数是否为空
        if (user == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //提取公共部分
        Long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User oldUser = this.getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //2 如果是管理员 则可以修改任意用户
//        if (isAdmin(loginUser)) {
//            if (userId <= 0) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR);
//            }
//            User oldUser = this.getById(userId);
//            if (oldUser == null) {
//                throw new BusinessException(ErrorCode.NOT_LOGIN);
//            }
//            return this.updateById(user);
//
//        }
//        //3 如果不是管理员则只能修改自己
//        if (!Objects.equals(userId, loginUser.getId())) {
//            throw new BusinessException(ErrorCode.NO_AUTH);
//        }
//        User oldUser = this.getById(userId);
//        if (oldUser == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN);
//        }
//        return this.updateById(user);

        //优化逻辑
        if (!isAdmin(loginUser) && !Objects.equals(userId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return this.updateById(user);
    }

    /**
     * 校验是否为管理员
     *
     * @param request 请求
     * @return boolean
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        //仅管理员可查询
        User user = (User) request.getSession().getAttribute(UserConstant.SESSION_KEY);
        return user != null && user.getUserrole() == UserConstant.USER_ADMIN;
    }

    /**
     * 校验是否为管理员
     *
     * @param loginUser 登录用户
     * @return boolean
     */
    @Override
    public boolean isAdmin(User loginUser) {

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return loginUser.getUserrole() == UserConstant.USER_ADMIN;
    }

    /**
     * 根据当前用户匹配相似用户
     *
     * @param loginUser 当前用户
     * @param num       匹配用户的数量
     * @return 用户列表
     */
    @Override
    public List<User> matchUser(User loginUser, Integer num) {
        if (num < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //调用自己定义的生成用户方法
        GenerateRecommendations recommendations = new GenerateRecommendations();
        //只取出用户id和用户标签不为空的字段
        List<User> userList = lambdaQuery().select(User::getId, User::getTags).isNotNull(User::getTags).list();
        //获取到了匹配后的用户列表
        List<User> matchUserList = recommendations.generateRecommendations(loginUser, userList, num);
        //根据用户id进行分组
        Map<Long, List<User>> longListMap = matchUserList.stream().collect(Collectors.groupingBy(User::getId));
        //拿到匹配的用户id列表
        List<Long> matchUserIdList = matchUserList.stream().map(User::getId).collect(Collectors.toList());
        //根据id去查询匹配用户的完整数据
        List<User> finalUser = lambdaQuery().in(User::getId, matchUserIdList).list();
        // 将用户脱敏
        List<User> safeUserList = finalUser.stream().peek(this::safaUser).collect(Collectors.toList());
        //将脱敏用户放回已排序好的列表中
        safeUserList.forEach(user -> {
            List<User> userListWithSameId = longListMap.get(user.getId());
            if (userListWithSameId != null && !userListWithSameId.isEmpty()) {
                userListWithSameId.set(0, user);
            }
        });
        //在根据上面id分组的map 进行提取已排序好的用户列表
        ArrayList<User> finalMatchUserList = new ArrayList<>();
        matchUserIdList.forEach(id -> {
            List<User> users = longListMap.get(id);
            finalMatchUserList.add(users.get(0));
        });
        return finalMatchUserList;
    }

    /**
     * 关注用户
     *
     * @param followId  被关注的用户id
     * @param loginUser 登录用户
     * @return IsFollowVO 返回封装类
     */
    @Override
    public IsFollowVO followUser(long followId, User loginUser) {
        //- 请求参数是否为空
        if (followId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //- 是否登录 未登录不允许关注
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //- 被关注的用户存在
        User user = this.getById(followId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您关注的用户不存在");
        }
        //不能关注自己
        if (followId == loginUser.getId()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能关注自己");
        }
        //不能重复关注已关注的
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFolloweruserid,loginUser.getId()).eq(Follow::getFolloweduserid,followId);
        Follow one = followService.getOne(followLambdaQueryWrapper);
        if (!(one ==null)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已关注");
        }

        //- 插入关注关系表
        Follow follow = new Follow();
        follow.setFolloweruserid(loginUser.getId());
        follow.setFolloweduserid(followId);
        follow.setFollowtime(new Date());
        boolean result = followService.save(follow);
        IsFollowVO isFollowVO = new IsFollowVO();
        isFollowVO.setFollow(true);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return isFollowVO;
    }

    /**
     * 取关用户
     *
     * @param followId  被取关的用户id
     * @param loginUser 登录用户
     * @return IsFollowVO 返回封装类
     */
    @Override
    public IsFollowVO discardUser(long followId, User loginUser) {
        //- 请求参数是否为空
        if (followId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //- 是否登录 未登录不允许取关
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //- 被取关的用户存在
        User user = this.getById(followId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您关注的用户不存在");
        }
        //不能取关自己
        if (followId == loginUser.getId()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能取关自己");
        }
        //判断要取关的用户是否已经关注了
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFolloweruserid,loginUser.getId()).eq(Follow::getFolloweduserid,followId);
        Follow one = followService.getOne(followLambdaQueryWrapper);
        if (one ==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未关注");
        }

        //- 删除关注关系表
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFolloweruserid, loginUser.getId()).eq(Follow::getFolloweduserid, followId);
        boolean result = followService.remove(queryWrapper);
        IsFollowVO isFollowVO = new IsFollowVO();
        isFollowVO.setFollow(false);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return isFollowVO;
    }

    /**
     * 查询关注的所有用户列表
     *
     * @param loginUser 登录用户
     * @return 关注的所有用户列表
     */
    @Override
    public List<User> followListUser(User loginUser) {
        return followOrFansListUser(loginUser, true);
    }

    /**
     * 查询粉丝用户列表
     *
     * @param loginUser 登录用户
     * @return 粉丝用户列表
     */
    @Override
    public List<User> fansListUser(User loginUser) {
        return followOrFansListUser(loginUser, false);
    }


    /**
     * 查询关注或粉丝用户列表
     *
     * @param loginUser 登录用户
     * @param isFollow  查询关注列表还是粉丝列表，true表示关注列表，false表示粉丝列表
     * @return 关注或粉丝用户列表
     */
    private List<User> followOrFansListUser(User loginUser, boolean isFollow) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //- 查询关注关系表  返回关注或粉丝列表用户id
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        if (isFollow) {
            queryWrapper.eq(Follow::getFolloweruserid, loginUser.getId());
        } else {
            queryWrapper.eq(Follow::getFolloweduserid, loginUser.getId());
        }

        List<Follow> followList = followService.list(queryWrapper);
        //根据id去查询用户信息
        List<Long> userIds = isFollow ?
                followList.stream().map(Follow::getFolloweduserid).collect(Collectors.toList()) :
                followList.stream().map(Follow::getFolloweruserid).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }
        //- 查询关注或粉丝用户信息并且进行脱敏返回
        return lambdaQuery().in(User::getId, userIds).list().stream().map(this::safaUser).collect(Collectors.toList());
    }
}


