package com.itzkz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itzkz.usercenter.constant.UserConstant;
import com.itzkz.usercenter.model.domain.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return userId
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);


    /**
     * 用户脱敏
     *
     * @param user 用户
     * @return 脱敏用户
     */
    User safaUser(User user);

    /**
     * 根据标签列表来查询用户
     * @param tagNameList 标签列表
     * @return 脱敏用户列表
     */
    List<User> searchUserByTags(List<String> tagNameList);


    /**
     * 获取当前用户
     * @param request 请求
     * @return 登录用户
     */
    User getLoginUser (HttpServletRequest request);

    /**
     * 更新用户
     * @param user 被修改用户
     * @param loginUser 登录用户
     * @return 1
     */
    boolean updateUser(User user ,User loginUser);

    /**
     * 校验是否为管理员
     *
     * @param request 请求
     * @return boolean
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 校验是否为管理员
     * @param loginUser 登录用户
     * @return boolean
     */
    boolean isAdmin(User loginUser);

}
