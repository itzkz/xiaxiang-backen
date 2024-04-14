package com.itzkz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itzkz.usercenter.common.BaseResponse;
import com.itzkz.usercenter.common.ErrorCode;
import com.itzkz.usercenter.common.ResultUtils;
import com.itzkz.usercenter.constant.UserConstant;
import com.itzkz.usercenter.exception.BusinessException;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.model.request.UserLoginRequest;
import com.itzkz.usercenter.model.request.UserRegisterRequest;
import com.itzkz.usercenter.service.UserService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Api(tags = "用户接口")
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param registerRequest 注册参数
     * @return 统一响应类
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);

    }

    /**
     * 用户登录
     *
     * @param loginRequest 登录参数
     * @param request      请求
     * @return 统一响应类
     */

    @PostMapping("/login")
    public BaseResponse userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 统一响应类
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 用户查询
     *
     * @param username 用户名称
     * @param request  请求
     * @return 用户列表
     */
    @GetMapping("/search")
    public List<User> search(String username, HttpServletRequest request) {

        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        List<User> userList = userService.list(queryWrapper);
        //userList.forEach(user1 -> userService.safaUser(user1));
        List<User> list = userList.stream().map(user -> userService.safaUser(user)).collect(Collectors.toList());
        return list;
    }

    /**
     * 用户推荐
     *
     * @param pageSize 每页展示的数量
     * @param pageNum  当前页面
     * @param request  请求
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> RecommendUser(Long pageSize, Long pageNum, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String redisKey = String.format("itzkz:recommend:%s", loginUser.getId());
        //如果有缓存，直接从缓存中读取
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);

        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        //如果没有缓存就从数据库中读取
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<User> list = new ArrayList<>();
        userPage.getRecords().forEach(user -> {
            User safeUser = userService.safaUser(user);
            list.add(safeUser);
        });
        //从数据库读取数据后，再写入缓存，记得写缓存过期时间
        try {
            valueOperations.set(redisKey, userPage, 300000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userPage);

    }

    /**
     * 根据当前用户匹配相似用户
     *
     * @return 统一响应类
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(HttpServletRequest request, int num) {
        if (request == null || num < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<User> teamList = userService.matchUser(loginUser, num);

        return ResultUtils.success(teamList);
    }

    /**
     * 删除用户
     *
     * @param id      用户id
     * @param request 请求
     * @return 统一响应类
     */
    @GetMapping("/delete")
    public BaseResponse<Boolean> deleteById(@PathVariable Long id, HttpServletRequest request) {

        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);

    }

    /**
     * 获取当前用户
     *
     * @param request 请求
     * @return 当前用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(UserConstant.SESSION_KEY);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long currentUserId = currentUser.getId();

        User user = userService.getById(currentUserId);
        User safaUser = userService.safaUser(user);
        return ResultUtils.success(safaUser);
    }

    /**
     * 根据用户标签搜索用户
     *
     * @param tagNameList 用户标签列表
     * @return 用户列表
     */

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> getUserListByTag(@RequestParam(required = false) List<String> tagNameList) {

        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 更新用户
     *
     * @param user    被修改用户
     * @param request 请求
     * @return boolean
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user, HttpServletRequest request) {

        if (user == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.updateUser(user, loginUser));

    }


}
