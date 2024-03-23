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
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {


    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
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
     * @param request
     * @return
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

        //2.加密
        String encryptPassword = SecureUtil.md5(userPassword);


        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUseraccount, userAccount);
        userLambdaQueryWrapper.eq(User::getUserpassword, encryptPassword);
        User user = this.getOne(userLambdaQueryWrapper);
        //校验账号是否存在
        if (user == null) {
            //            return ResultUtils.error(200,"账号已存在","");
            throw new BusinessException("账号不存在,请注册", 200, "");
        }

        //3.脱敏

        User safaUser = safaUser(user);

        //4.session
        request.getSession().setAttribute(UserConstant.SESSION_KEY, safaUser);
        return safaUser;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
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
     * @return
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


}

